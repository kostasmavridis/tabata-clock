package com.kostasmavridis.tabataclock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "tabata_settings")

class SettingsRepository @Inject constructor(private val context: Context) : ISettingsRepository {

    companion object {
        val KEY_PREPARE = intPreferencesKey("prepare_secs")
        val KEY_WORK    = intPreferencesKey("work_secs")
        val KEY_REST    = intPreferencesKey("rest_secs")
        val KEY_ROUNDS  = intPreferencesKey("rounds")
        val KEY_SETS    = intPreferencesKey("sets")
    }

    // Repository-owned scope so the StateFlow stays alive for the process
    // lifetime, independent of any subscriber. SupervisorJob ensures a
    // child failure does not cancel the whole scope.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * DataStore mapped to [TabataSettings], exposed as a [StateFlow].
     *
     * Using [SharingStarted.Eagerly] in the repository scope means the
     * flow starts collecting as soon as the repository is created (at
     * app-start, via Hilt singleton), so [StateFlow.value] is populated
     * well before the ViewModel reads it.
     *
     * [TabataSettings.validated] silently clamps corrupted DataStore
     * entries to safe ranges.
     */
    override val settingsFlow: StateFlow<TabataSettings> = context.dataStore.data
        .map { prefs ->
            TabataSettings.validated(
                prepareSecs = prefs[KEY_PREPARE] ?: 10,
                workSecs    = prefs[KEY_WORK]    ?: 20,
                restSecs    = prefs[KEY_REST]    ?: 10,
                rounds      = prefs[KEY_ROUNDS]  ?: 8,
                sets        = prefs[KEY_SETS]    ?: 1
            )
        }
        .stateIn(
            scope          = scope,
            started        = SharingStarted.Eagerly,
            initialValue   = TabataSettings()
        )

    override suspend fun saveSettings(settings: TabataSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREPARE] = settings.prepareSecs
            prefs[KEY_WORK]    = settings.workSecs
            prefs[KEY_REST]    = settings.restSecs
            prefs[KEY_ROUNDS]  = settings.rounds
            prefs[KEY_SETS]    = settings.sets
        }
    }
}
