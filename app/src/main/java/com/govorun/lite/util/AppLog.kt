package com.govorun.lite.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** File-based journal, capped at MAX_LINES, trimmed from the top on overflow. */
object AppLog {

    private const val FILE_NAME = "app_log.txt"
    private const val MAX_LINES = 500

    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun log(ctx: Context, message: String) {
        try {
            val file = File(ctx.filesDir, FILE_NAME)
            val line = "${timeFormat.format(Date())}  $message\n"
            file.appendText(line)
            trimIfNeeded(file)
        } catch (_: Exception) {
        }
    }

    fun read(ctx: Context): String {
        val file = File(ctx.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else ""
    }

    fun clear(ctx: Context) {
        File(ctx.filesDir, FILE_NAME).delete()
    }

    private fun trimIfNeeded(file: File) {
        if (!file.exists()) return
        val lines = file.readLines()
        if (lines.size > MAX_LINES) {
            file.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }
}
