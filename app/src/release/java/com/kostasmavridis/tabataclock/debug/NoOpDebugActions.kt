package com.kostasmavridis.tabataclock.debug

import android.content.Context

/**
 * Release implementation of DebugActions — all methods are intentional no-ops.
 * The release compiler will inline and eliminate these call sites entirely.
 */
object NoOpDebugActions : DebugActions {
    override fun exportLogs(context: Context) = Unit
}

/** Single access point used by SettingsScreen. */
val debugActions: DebugActions = NoOpDebugActions
