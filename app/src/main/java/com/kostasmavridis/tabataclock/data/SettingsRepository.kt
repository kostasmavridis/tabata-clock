package com.kostasmavridis.tabataclock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "tabata_settings")

class SettingsRepository @Inject constructor(private val context: Context) {

    companion object {
        val KEY_PREPARE = intPreferencesKey("prepare_secs")
        val KEY_WORK = intPreferencesKey("work_secs")
        val KEY_REST = intPreferencesKey("rest_secs")
        val KEY_ROUNDS = intPreferencesKey("rounds")
        val KEY_SETS = intPreferencesKey("sets")
    }

    val settingsFlow: Flow<TabataSettings> = context.dataStore.data.map { prefs ->
        TabataSettings(
            prepareSecs = prefs[KEY_PREPARE] ?: 10,
            workSecs    = prefs[KEY_WORK]    ?: 20,
            restSecs    = prefs[KEY_REST]    ?: 10,
            rounds      = prefs[KEY_ROUNDS]  ?: 8,
            sets        = prefs[KEY_SETS]    ?: 1
        )
    }

    suspend fun saveSettings(settings: TabataSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREPARE] = settings.prepareSecs
            prefs[KEY_WORK]    = settings.workSecs
            prefs[KEY_REST]    = settings.restSecs
            prefs[KEY_ROUNDS]  = settings.rounds
            prefs[KEY_SETS]    = settings.sets
        }
    }
}
