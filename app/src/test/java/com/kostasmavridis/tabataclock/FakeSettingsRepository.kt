package com.kostasmavridis.tabataclock

import com.kostasmavridis.tabataclock.data.SettingsRepository
import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake for [SettingsRepository].
 * Avoids DataStore touching the filesystem in unit tests.
 */
class FakeSettingsRepository(
    initial: TabataSettings = TabataSettings()
) : SettingsRepository(TODO("Not used in fake — constructor bypassed via subclass")) {

    private val _flow = MutableStateFlow(initial)
    override val settingsFlow: Flow<TabataSettings> = _flow.asStateFlow()

    override suspend fun saveSettings(settings: TabataSettings) {
        _flow.value = settings
    }
}
