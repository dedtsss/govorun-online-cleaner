package com.govorun.lite.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.R
import com.govorun.lite.model.GigaAmModel
import com.govorun.lite.stats.StatsStore
import com.govorun.lite.ui.onboarding.OnboardingStep
import com.govorun.lite.util.AccessibilityHelper

/**
 * Post-onboarding home screen.
 *
 *  - AppBar with overflow menu: Настройки / Pro-версия / О программе
 *  - Headline / subline adapt: ready / needs setup / just finished
 *  - Status card appears only when something is off; single «Исправить»
 *    button re-enters onboarding at the first broken step
 *  - Stats card always visible (shows empty-state hint before first use)
 *  - Promo card links to FuturePlansActivity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var headline: MaterialTextView
    private lateinit var subline: MaterialTextView
    private lateinit var statusMic: View
    private lateinit var statusService: View
    private lateinit var statusModel: View
    private lateinit var statusBattery: View
    private lateinit var fixAllButton: MaterialButton
    private lateinit var statusCard: View
    private lateinit var statsRow: View
    private lateinit var statsWordsNumber: MaterialTextView
    private lateinit var statsMinutesNumber: MaterialTextView
    private lateinit var statsEmpty: MaterialTextView
    private lateinit var toolbar: MaterialToolbar
    private var showJustFinished: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.topAppBar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_pro -> {
                    startActivity(Intent(this, FuturePlansActivity::class.java))
                    true
                }
                R.id.action_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }

        val scroll = findViewById<View>(R.id.scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(bottom = bars.bottom)
            insets
        }

        headline = findViewById(R.id.mainHeadline)
        subline = findViewById(R.id.mainSubline)
        statusMic = findViewById(R.id.statusMic)
        statusService = findViewById(R.id.statusService)
        statusModel = findViewById(R.id.statusModel)
        statusBattery = findViewById(R.id.statusBattery)
        fixAllButton = findViewById(R.id.statusFixAll)
        statusCard = findViewById(R.id.statusCard)
        statsRow = findViewById(R.id.statsRow)
        statsWordsNumber = findViewById(R.id.statsWordsNumber)
        statsMinutesNumber = findViewById(R.id.statsMinutesNumber)
        statsEmpty = findViewById(R.id.statsEmpty)

        findViewById<View>(R.id.promoCard).setOnClickListener {
            startActivity(Intent(this, FuturePlansActivity::class.java))
        }

        if (com.govorun.lite.BuildConfig.DEBUG) {
            // Long-press the stats card to seed a realistic "power user" total,
            // so we can eyeball the big-number layout without having to dictate
            // for an hour. Short-press (long press again) clears.
            val statsCard = findViewById<View>(R.id.statsCard)
            statsCard?.setOnLongClickListener {
                val prefs = getSharedPreferences("govorun_lite_prefs", MODE_PRIVATE)
                val hasDemo = prefs.getLong("stats_words_total", 0L) >= 3000L
                if (hasDemo) {
                    prefs.edit().putLong("stats_words_total", 0L).putLong("stats_seconds_total", 0L).apply()
                } else {
                    prefs.edit().putLong("stats_words_total", 3000L).putLong("stats_seconds_total", 3600L).apply()
                }
                refreshStats()
                true
            }
        }

        showJustFinished = intent.getBooleanExtra(EXTRA_JUST_FINISHED, false)
        if (showJustFinished) {
            // Consume the flag so orientation changes / re-launches show the
            // standard headline.
            intent.removeExtra(EXTRA_JUST_FINISHED)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
        refreshStats()
    }

    private fun refreshStats() {
        val words = StatsStore.getWords(this)
        val voiceSeconds = StatsStore.getSeconds(this)
        val voiceMinutes = voiceSeconds / 60L

        if (words <= 0L) {
            statsRow.visibility = View.GONE
            statsEmpty.visibility = View.VISIBLE
            return
        }
        statsRow.visibility = View.VISIBLE
        statsEmpty.visibility = View.GONE

        statsWordsNumber.text = formatThousands(words)
        statsMinutesNumber.text = voiceMinutes.toString()
    }

    private fun formatThousands(value: Long): String {
        // Use a non-breaking space as the thousands separator so "1 234" never
        // wraps mid-number in the narrow two-column layout.
        return if (value < 1000L) value.toString()
        else value.toString().reversed().chunked(3).joinToString("\u00A0").reversed()
    }

    private fun refreshStatuses() {
        val micOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val serviceOk = AccessibilityHelper.isLiteServiceEnabled(this)
        val modelOk = GigaAmModel.isInstalled(this)
        val batteryOk = run {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(packageName) == true
        }
        val allOk = micOk && serviceOk && modelOk && batteryOk

        // Headline + subline always at the top. The wording adapts:
        //  - just finished onboarding → celebratory "Всё готово!"
        //  - everything ok → calmer "Всё готово" (toolbar already says "Говорун", so no duplication)
        //  - something missing → "Нужна настройка" + nudge to the status card
        when {
            showJustFinished && allOk -> {
                headline.setText(R.string.main_just_finished)
                subline.setText(R.string.main_just_finished_sub)
            }
            allOk -> {
                headline.setText(R.string.main_ready)
                subline.setText(R.string.main_hint)
            }
            else -> {
                headline.setText(R.string.main_needs_setup)
                subline.setText(R.string.main_needs_setup_sub)
            }
        }

        if (allOk) {
            statusCard.visibility = View.GONE
            return
        }

        statusCard.visibility = View.VISIBLE
        bindRow(statusMic, micOk, R.string.main_status_mic_ok, R.string.main_status_mic_missing)
        bindRow(statusService, serviceOk, R.string.main_status_service_ok, R.string.main_status_service_missing)
        bindRow(statusModel, modelOk, R.string.main_status_model_ok, R.string.main_status_model_missing)
        bindRow(statusBattery, batteryOk, R.string.main_status_battery_ok, R.string.main_status_battery_missing)

        val firstBrokenStep = when {
            !micOk -> OnboardingStep.MIC
            !serviceOk -> OnboardingStep.ACCESSIBILITY
            !modelOk -> OnboardingStep.MODEL
            else -> OnboardingStep.BATTERY
        }
        fixAllButton.visibility = View.VISIBLE
        fixAllButton.setOnClickListener { reenterOnboarding(firstBrokenStep) }
    }

    private fun bindRow(row: View, ok: Boolean, okLabel: Int, missingLabel: Int) {
        val icon = row.findViewById<ImageView>(R.id.statusIcon)
        val label = row.findViewById<MaterialTextView>(R.id.statusLabel)

        if (ok) {
            icon.setImageResource(R.drawable.ic_check_circle_24)
            icon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(row, com.google.android.material.R.attr.colorPrimary)
            )
            label.setText(okLabel)
        } else {
            icon.setImageResource(R.drawable.ic_warning_24)
            icon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(row, com.google.android.material.R.attr.colorError)
            )
            label.setText(missingLabel)
        }
    }

    private fun reenterOnboarding(startAt: OnboardingStep) {
        getSharedPreferences("govorun_lite_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarding_done", false)
            .putString("onboarding_step", startAt.name)
            .apply()
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_JUST_FINISHED = "just_finished"
    }
}
