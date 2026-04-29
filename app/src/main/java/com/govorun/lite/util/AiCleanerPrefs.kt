package com.govorun.lite.util

import android.content.Context

/**
 * Preferences for the optional online AI cleanup feature.
 *
 * Speech recognition stays offline. These settings are only used after GigaAM
 * has already produced text and the user explicitly enables AI cleanup.
 */
object AiCleanerPrefs {

    private const val FILE_NAME = "govorun_ai_cleaner_prefs"

    private const val KEY_ENABLED = "ai_cleanup_enabled"
    private const val KEY_AUTH_KEY = "gigachat_authorization_key"
    private const val KEY_MODEL = "gigachat_model"
    private const val KEY_MODE = "cleanup_mode"

    const val MODEL_GIGACHAT_2_LITE = "GigaChat-2-Lite"
    const val MODEL_GIGACHAT_2_PRO = "GigaChat-2-Pro"
    const val MODEL_DEFAULT = MODEL_GIGACHAT_2_LITE

    const val MODE_LIGHT = "light"
    const val MODE_NORMAL = "normal"
    const val MODE_CLEAN = "clean"
    const val MODE_DEFAULT = MODE_NORMAL

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun getAuthorizationKey(context: Context): String =
        prefs(context).getString(KEY_AUTH_KEY, "")?.trim().orEmpty()

    fun setAuthorizationKey(context: Context, key: String) {
        prefs(context).edit()
            .putString(KEY_AUTH_KEY, key.trim())
            .apply()
    }

    fun getModel(context: Context): String {
        val raw = prefs(context).getString(KEY_MODEL, MODEL_DEFAULT) ?: MODEL_DEFAULT
        return when (raw) {
            MODEL_GIGACHAT_2_LITE,
            MODEL_GIGACHAT_2_PRO -> raw
            else -> MODEL_DEFAULT
        }
    }

    fun setModel(context: Context, model: String) {
        require(model == MODEL_GIGACHAT_2_LITE || model == MODEL_GIGACHAT_2_PRO) {
            "Invalid GigaChat model: $model"
        }
        prefs(context).edit()
            .putString(KEY_MODEL, model)
            .apply()
    }

    fun getMode(context: Context): String {
        val raw = prefs(context).getString(KEY_MODE, MODE_DEFAULT) ?: MODE_DEFAULT
        return when (raw) {
            MODE_LIGHT,
            MODE_NORMAL,
            MODE_CLEAN -> raw
            else -> MODE_DEFAULT
        }
    }

    fun setMode(context: Context, mode: String) {
        require(mode == MODE_LIGHT || mode == MODE_NORMAL || mode == MODE_CLEAN) {
            "Invalid cleanup mode: $mode"
        }
        prefs(context).edit()
            .putString(KEY_MODE, mode)
            .apply()
    }

    fun hasAuthorizationKey(context: Context): Boolean =
        getAuthorizationKey(context).isNotBlank()

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
