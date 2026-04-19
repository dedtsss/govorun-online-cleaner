package com.govorun.lite.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import com.govorun.lite.service.LiteAccessibilityService

/**
 * Whether [LiteAccessibilityService] is currently enabled in system settings.
 *
 * Uses the colon-separated [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES]
 * string rather than [android.view.accessibility.AccessibilityManager], because
 * the latter also needs the service to have connected, which can race on first
 * toggle.
 */
object AccessibilityHelper {

    fun isLiteServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, LiteAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next()) ?: continue
            if (component == expected) return true
        }
        return false
    }

    /**
     * Whether the user enabled the system accessibility Shortcut for our service.
     * Covers the navbar / floating a11y button (accessibility_button_targets) and
     * the volume-key shortcut (accessibility_shortcut_target_service). Either one
     * manifests as an unwanted extra floating icon on top of the bubble.
     */
    fun isLiteShortcutEnabled(context: Context): Boolean {
        val expected = ComponentName(context, LiteAccessibilityService::class.java)
        val keys = arrayOf(
            "accessibility_button_targets",
            "accessibility_shortcut_target_service",
        )
        for (key in keys) {
            val raw = Settings.Secure.getString(context.contentResolver, key) ?: continue
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(raw)
            while (splitter.hasNext()) {
                val component = ComponentName.unflattenFromString(splitter.next()) ?: continue
                if (component == expected) return true
            }
        }
        return false
    }

    /**
     * Tries to open the settings page for our service directly so the user can
     * toggle the Shortcut off without hunting. Falls back to the global
     * Accessibility list if the direct intent isn't handled or is blocked.
     *
     * ACTION_ACCESSIBILITY_DETAILS_SETTINGS requires the signature permission
     * OPEN_ACCESSIBILITY_DETAILS_SETTINGS on some builds, so a bare try/catch
     * on ActivityNotFoundException isn't enough — we also need to swallow
     * SecurityException to stop the app from crashing.
     */
    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, LiteAccessibilityService::class.java).flattenToString()
        val detail = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("android.intent.extra.COMPONENT_NAME", component)
            putExtra(":settings:fragment_args_key", component)
            putExtra(":settings:show_fragment_args", Bundle().apply {
                putString(":settings:fragment_args_key", component)
            })
        }
        val fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(detail)
        } catch (_: Exception) {
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {
                // Nothing else we can do — settings app missing/locked.
            }
        }
    }
}
