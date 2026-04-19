package com.govorun.lite.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.govorun.lite.R
import com.govorun.lite.service.LiteAccessibilityService
import com.govorun.lite.util.AccessibilityHelper
import com.govorun.lite.util.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var disableRow: View
    private lateinit var disableDivider: View

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<MaterialToolbar>(R.id.topAppBar)
            .setNavigationOnClickListener { finish() }

        val scroll = findViewById<View>(R.id.scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(bottom = bars.bottom)
            insets
        }

        val hapticsSwitch = findViewById<MaterialSwitch>(R.id.hapticsSwitch)
        val hapticsRow = findViewById<View>(R.id.hapticsRow)

        hapticsSwitch.isChecked = Prefs.isHapticsEnabled(this)
        hapticsSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setHapticsEnabled(this, checked)
        }
        hapticsRow.setOnClickListener { hapticsSwitch.toggle() }

        disableRow = findViewById(R.id.disableRow)
        disableDivider = findViewById(R.id.disableDivider)
        disableRow.setOnClickListener { confirmDisableService() }
    }

    override fun onResume() {
        super.onResume()
        // Disable row is only meaningful while the accessibility service is on —
        // if the user turned it off from system settings, hiding the row avoids
        // a no-op button.
        val serviceOn = AccessibilityHelper.isLiteServiceEnabled(this)
        disableRow.visibility = if (serviceOn) View.VISIBLE else View.GONE
        disableDivider.visibility = if (serviceOn) View.VISIBLE else View.GONE
    }

    private fun confirmDisableService() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.main_disable_service)
            .setMessage(R.string.main_disable_service_hint)
            .setPositiveButton(R.string.main_disable_service) { _, _ ->
                LiteAccessibilityService.instance?.disableSelf()
                // System updates the enabled list asynchronously; refresh after
                // a beat so the row hides itself.
                disableRow.postDelayed({
                    val stillOn = AccessibilityHelper.isLiteServiceEnabled(this)
                    disableRow.visibility = if (stillOn) View.VISIBLE else View.GONE
                    disableDivider.visibility = if (stillOn) View.VISIBLE else View.GONE
                }, 300)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
