package com.kostasmavridis.tabataclock.data

import com.kostasmavridis.tabataclock.model.TabataSettings
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and persisting [TabataSettings].
 * Decoupled from [SettingsRepository] so unit tests can inject an
 * in-memory fake without requiring DataStore or a file system.
 */
interface ISettingsRepository {
    val settingsFlow: Flow<TabataSettings>
    suspend fun saveSettings(settings: TabataSettings)
}
