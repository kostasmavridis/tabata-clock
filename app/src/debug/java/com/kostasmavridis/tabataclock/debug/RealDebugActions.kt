package com.kostasmavridis.tabataclock.debug

import android.content.Context

/**
 * Debug implementation of DebugActions — delegates to LogExporter.
 */
object RealDebugActions : DebugActions {
    override fun exportLogs(context: Context) = LogExporter.share(context)
}

/** Single access point used by SettingsScreen. */
val debugActions: DebugActions = RealDebugActions
