package com.govorun.lite.stats

import android.content.Context

/**
 * Usage counters stored in SharedPreferences. Cheap single-process increments —
 * all writes happen from the accessibility service, reads from MainActivity.
 */
object StatsStore {
    private const val PREFS = "govorun_lite_prefs"
    private const val KEY_WORDS = "stats_words_total"
    private const val KEY_SECONDS = "stats_seconds_total"

    fun addWords(context: Context, count: Int) {
        if (count <= 0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getLong(KEY_WORDS, 0L)
        prefs.edit().putLong(KEY_WORDS, current + count).apply()
    }

    fun addSeconds(context: Context, seconds: Long) {
        if (seconds <= 0L) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getLong(KEY_SECONDS, 0L)
        prefs.edit().putLong(KEY_SECONDS, current + seconds).apply()
    }

    fun getWords(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_WORDS, 0L)

    fun getSeconds(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SECONDS, 0L)

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }
}
