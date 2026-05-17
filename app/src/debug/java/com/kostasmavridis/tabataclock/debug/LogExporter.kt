package com.kostasmavridis.tabataclock.debug

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

private const val TAG = "LogExporter"

/**
 * Captures the last ~500 lines of logcat output for this app's process
 * and shares them via the standard Android share sheet.
 *
 * Uses a FileProvider so no WRITE_EXTERNAL_STORAGE permission is needed.
 * The provider authority is declared only in the debug source-set manifest
 * overlay, so it is completely stripped from release builds.
 */
object LogExporter {

    fun share(context: Context) {
        try {
            val logText = captureLogcat()
            val file    = writeToCacheFile(context, logText)
            val uri     = FileProvider.getUriForFile(
                context,
                "${context.packageName}.debug.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type      = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Tabata Clock crash log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share log via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
        }
    }

    private fun captureLogcat(): String {
        return try {
            // -d  = dump and exit (non-blocking)
            // -v  = threadtime format includes date, time, PID, TID, level, tag
            // --pid = restrict to this process only (avoids noise from other apps)
            val pid     = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-v", "threadtime", "--pid", pid.toString())
            )
            val output = process.inputStream.bufferedReader().readLines()
            // Keep the last 500 lines — enough to capture a crash without
            // bloating the share payload.
            output.takeLast(500).joinToString("\n")
        } catch (e: IOException) {
            "Failed to read logcat: ${e.message}"
        }
    }

    private fun writeToCacheFile(context: Context, text: String): File {
        val dir  = File(context.cacheDir, "logs").also { it.mkdirs() }
        val file = File(dir, "tabata_log.txt")
        file.writeText(text)
        return file
    }
}
