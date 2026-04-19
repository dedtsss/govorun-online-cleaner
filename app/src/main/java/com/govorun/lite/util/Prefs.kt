package com.govorun.lite.util

import android.content.Context

/**
 * Single place that knows the app's SharedPreferences file name + keys.
 * Keeps key spelling consistent across UI (where the user toggles it) and
 * the AccessibilityService (which reads it on every bubble tap).
 */
object Prefs {

    private const val FILE_NAME = "govorun_lite_prefs"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"

    fun isHapticsEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTICS_ENABLED, false)

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAPTICS_ENABLED, enabled)
            .apply()
    }
}
