package com.kostasmavridis.tabataclock

import com.kostasmavridis.tabataclock.data.ISettingsRepository
import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake for [ISettingsRepository].
 * Avoids DataStore touching the filesystem in unit tests.
 *
 * Backed by a [MutableStateFlow] so [settingsFlow] is a [StateFlow] with
 * a synchronously readable [StateFlow.value]. This matches the updated
 * [ISettingsRepository] contract and ensures the ViewModel can read the
 * initial prepareSecs without waiting for a coroutine to run.
 */
class FakeSettingsRepository(
    initial: TabataSettings = TabataSettings()
) : ISettingsRepository {

    private val _flow = MutableStateFlow(initial)

    override val settingsFlow: StateFlow<TabataSettings> = _flow.asStateFlow()

    // Exposed so tests can push new values and assert `.value` directly.
    val flow: MutableStateFlow<TabataSettings> get() = _flow

    override suspend fun saveSettings(settings: TabataSettings) {
        _flow.value = settings
    }
}
