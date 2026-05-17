package com.kostasmavridis.tabataclock.debug

import android.content.Context

/**
 * Abstraction over debug-only actions so that the main source set never
 * directly references classes that only exist in the debug source set.
 *
 * - debug source set provides: RealDebugActions (backed by LogExporter)
 * - release source set provides: NoOpDebugActions (empty stubs)
 *
 * SettingsScreen calls through this interface; the release compiler sees
 * only NoOpDebugActions and dead-code-eliminates the BuildConfig.DEBUG
 * branch entirely.
 */
interface DebugActions {
    fun exportLogs(context: Context)
}
