package com.kostasmavridis.tabataclock

import com.kostasmavridis.tabataclock.data.ISettingsRepository
import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake for [ISettingsRepository].
 * Avoids DataStore touching the filesystem in unit tests.
 */
class FakeSettingsRepository(
    initial: TabataSettings = TabataSettings()
) : ISettingsRepository {

    private val _flow = MutableStateFlow(initial)

    override val settingsFlow: Flow<TabataSettings> = _flow.asStateFlow()

    // Expose as MutableStateFlow so tests can assert `.value` directly
    val flow: MutableStateFlow<TabataSettings> get() = _flow

    override suspend fun saveSettings(settings: TabataSettings) {
        _flow.value = settings
    }
}
