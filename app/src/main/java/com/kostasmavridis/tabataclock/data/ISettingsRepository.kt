package com.kostasmavridis.tabataclock.data

import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for reading and persisting Tabata settings.
 *
 * [settingsFlow] is a [StateFlow] so that callers can always read the
 * current value synchronously via [StateFlow.value] without suspending.
 * This guarantees the ViewModel can seed its initial [TimerState] with
 * the correct prepareSecs even before any coroutine has been scheduled.
 */
interface ISettingsRepository {
    val settingsFlow: StateFlow<TabataSettings>
    suspend fun saveSettings(settings: TabataSettings)
}
