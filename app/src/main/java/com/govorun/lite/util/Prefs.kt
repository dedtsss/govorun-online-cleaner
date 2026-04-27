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
    private const val KEY_BUBBLE_ALPHA = "bubble_alpha"
    private const val KEY_BUBBLE_SIZE = "bubble_size"
    private const val KEY_BUBBLE_Y = "bubble_y"
    private const val KEY_BUBBLE_SIDE = "bubble_side"
    // Version-suffixed so each release with new feature highlights re-shows
    // the card to existing users (their dismissal of the previous version's
    // card doesn't carry over). Bump the suffix when the card content changes.
    private const val KEY_WHATS_NEW_HINT_DISMISSED = "whats_new_hint_dismissed_v106"
    private const val KEY_DICTIONARY = "dictionary_text"
    private const val KEY_DICTIONARY_ENABLED = "dictionary_enabled"

    const val BUBBLE_SIDE_RIGHT = "right"
    const val BUBBLE_SIDE_LEFT = "left"

    // Clamp range for the bubble fill alpha. The floor (0.4) keeps the
    // bubble visible enough that the bird silhouette is still recognisable
    // on any wallpaper — allowing lower values leads to "where did it go?"
    // support questions. Ceiling 1.0 is fully opaque.
    const val BUBBLE_ALPHA_MIN = 0.4f
    const val BUBBLE_ALPHA_MAX = 1.0f
    const val BUBBLE_ALPHA_STEP = 0.05f
    // Slight translucency by default — enough to blend the bubble with a
    // patterned wallpaper, not so much that it becomes hard to spot. Must
    // land on the slider step (0.4 + N × 0.05) or the M3 Slider throws
    // IllegalStateException on setValue.
    const val BUBBLE_ALPHA_DEFAULT = 0.85f

    // Default ON since 1.0.4 — vibration is a "feature" we want users to
    // experience out of the box. The MainActivity shows a one-time FYI card
    // explaining this and offering a one-tap path to disable, so users
    // who don't want it can flip it off without hunting through settings.
    fun isHapticsEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTICS_ENABLED, true)

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAPTICS_ENABLED, enabled)
            .apply()
    }

    fun getBubbleAlpha(context: Context): Float {
        val raw = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_BUBBLE_ALPHA, BUBBLE_ALPHA_DEFAULT)
        return snapBubbleAlpha(raw)
    }

    fun setBubbleAlpha(context: Context, alpha: Float) {
        val snapped = snapBubbleAlpha(alpha)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BUBBLE_ALPHA, snapped)
            .apply()
    }

    // Bubble size scale relative to the baseline 56dp disc. 1.0 = stock,
    // 0.7 = compact (≈39 dp), 1.4 = chunky (≈78 dp). Same slider-step
    // discipline as alpha — Material Slider throws on off-step values.
    const val BUBBLE_SIZE_MIN = 0.7f
    const val BUBBLE_SIZE_MAX = 1.4f
    const val BUBBLE_SIZE_STEP = 0.05f
    const val BUBBLE_SIZE_DEFAULT = 1.0f

    fun getBubbleSize(context: Context): Float {
        val raw = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_BUBBLE_SIZE, BUBBLE_SIZE_DEFAULT)
        return snapBubbleSize(raw)
    }

    fun setBubbleSize(context: Context, size: Float) {
        val snapped = snapBubbleSize(size)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BUBBLE_SIZE, snapped)
            .apply()
    }

    private fun snapBubbleSize(value: Float): Float {
        val clamped = value.coerceIn(BUBBLE_SIZE_MIN, BUBBLE_SIZE_MAX)
        val steps = Math.round((clamped - BUBBLE_SIZE_MIN) / BUBBLE_SIZE_STEP)
        val raw = BUBBLE_SIZE_MIN + steps * BUBBLE_SIZE_STEP
        val rounded = Math.round(raw * 100f) / 100f
        return rounded.coerceIn(BUBBLE_SIZE_MIN, BUBBLE_SIZE_MAX)
    }

    // WindowManager Y offset of the floating bubble — set after the user
    // drags it. Default 0 = vertical centre (the LayoutParams gravity is
    // CENTER_VERTICAL, so Y is interpreted as offset from centre). Stored
    // so the bubble doesn't snap back to the middle every time the system
    // restarts the accessibility service.
    fun getBubbleY(context: Context): Int =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BUBBLE_Y, 0)

    fun setBubbleY(context: Context, y: Int) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }

    // Which screen edge the bubble snaps to. Default right (matches the
    // accessibility-overlay convention on most Android voice/translate apps).
    fun getBubbleSide(context: Context): String =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BUBBLE_SIDE, BUBBLE_SIDE_RIGHT) ?: BUBBLE_SIDE_RIGHT

    fun setBubbleSide(context: Context, side: String) {
        require(side == BUBBLE_SIDE_LEFT || side == BUBBLE_SIDE_RIGHT) {
            "Invalid bubble side: $side"
        }
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BUBBLE_SIDE, side)
            .apply()
    }

    // One-time "What's new in 1.0.4" card on the main screen. Once
    // dismissed, never shown again. New users (post-onboarding) and
    // upgrading users (skip onboarding) both see it once.
    fun isWhatsNewHintDismissed(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WHATS_NEW_HINT_DISMISSED, false)

    fun setWhatsNewHintDismissed(context: Context, dismissed: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WHATS_NEW_HINT_DISMISSED, dismissed)
            .apply()
    }

    // User dictionary — raw text in "key=value" per line format. Parsing,
    // word-boundary regex compilation and replacement happen in
    // [com.govorun.lite.transcriber.Dictionary]. Stored as a single string
    // so import/export trivially round-trips through ACTION_OPEN/CREATE_DOCUMENT.
    fun getDictionary(context: Context): String =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DICTIONARY, "") ?: ""

    fun setDictionary(context: Context, text: String) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DICTIONARY, text)
            .apply()
    }

    // Master on/off for the dictionary. Default OFF so a brand-new user
    // can browse the editor (with starter examples) without their next
    // dictation being silently transformed by rules they never asked for.
    // Enabling it is a deliberate one-tap action inside the dictionary
    // screen, after the user has seen what's there.
    fun isDictionaryEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DICTIONARY_ENABLED, false)

    fun setDictionaryEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DICTIONARY_ENABLED, enabled)
            .apply()
    }

    // Always land on a multiple of the slider step — M3 Slider throws if
    // setValue() is called with anything between two steps, so we keep the
    // pref storage aligned too. Rounded to 2 decimal places to neutralise
    // float drift (0.4f + 9 * 0.05f = 0.8500001f, which would squeak past
    // the current Slider tolerance but could break in a future release).
    private fun snapBubbleAlpha(value: Float): Float {
        val clamped = value.coerceIn(BUBBLE_ALPHA_MIN, BUBBLE_ALPHA_MAX)
        val steps = Math.round((clamped - BUBBLE_ALPHA_MIN) / BUBBLE_ALPHA_STEP)
        val raw = BUBBLE_ALPHA_MIN + steps * BUBBLE_ALPHA_STEP
        val rounded = Math.round(raw * 100f) / 100f
        return rounded.coerceIn(BUBBLE_ALPHA_MIN, BUBBLE_ALPHA_MAX)
    }
}
