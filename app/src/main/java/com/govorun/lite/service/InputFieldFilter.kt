package com.govorun.lite.service

import android.accessibilityservice.InputMethod
import android.text.InputType

/**
 * Filters that decide whether the bubble should be visible based on the
 * IME's current input editor info. Lives outside [LiteAccessibilityService]
 * to keep that class focused on lifecycle and event dispatch — pure
 * inspection helpers don't need to be members.
 *
 * Extracted in 1.0.6 when password-field detection pushed the service
 * past the 500-line ceiling. Future filters (e.g. URL fields, sensitive
 * search boxes) belong here too.
 */
internal object InputFieldFilter {

    /**
     * True if the IME is currently bound to a password-style field.
     * Reads inputType from the bound editor info — the canonical signal
     * that every Android IME uses to render dots-instead-of-text and
     * suppress autocorrect. Banking PIN screens, login forms, password
     * managers and 2FA entries all set one of these variation flags.
     *
     * Returns false on null inputs (no IME bound, no editor info) — the
     * safe default is "not a password", so we'd rather leak a bubble
     * appearance than miss showing it on a regular field.
     */
    fun isPasswordField(inputMethod: InputMethod?): Boolean {
        val info = inputMethod?.currentInputEditorInfo ?: return false
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        val klass = info.inputType and InputType.TYPE_MASK_CLASS
        return when (klass) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }
}
