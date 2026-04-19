package com.govorun.lite.ui.onboarding

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.R
import com.govorun.lite.util.AccessibilityHelper

/**
 * Step 4: enabling the LiteAccessibilityService in system settings.
 *
 * Three states:
 *  - service OFF → "please enable" state, block progress
 *  - service ON + Shortcut ON → system auto-toggled the Shortcut; block progress
 *    until the user turns it off (otherwise a system floating icon fights the
 *    bubble and the user can't find how to disable it later)
 *  - service ON + Shortcut OFF → step complete
 */
class AccessibilityFragment : OnboardingStepFragment() {

    private lateinit var topIcon: ImageView
    private lateinit var statusText: MaterialTextView
    private lateinit var openButton: MaterialButton
    private lateinit var hintCard: MaterialCardView
    private lateinit var hintHeader: View
    private lateinit var hintIcon: ImageView
    private lateinit var hintTitle: MaterialTextView
    private lateinit var hintText: MaterialTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding_accessibility, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topIcon = view.findViewById(R.id.accIcon)
        statusText = view.findViewById(R.id.accStatus)
        openButton = view.findViewById(R.id.accOpen)
        hintCard = view.findViewById(R.id.accHint)
        hintHeader = view.findViewById(R.id.accHintHeader)
        hintIcon = view.findViewById(R.id.accHintIcon)
        hintTitle = view.findViewById(R.id.accHintTitle)
        hintText = view.findViewById(R.id.accHintText)

        openButton.setOnClickListener {
            AccessibilityHelper.openAccessibilitySettings(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    override fun onStepFocused() {
        refreshState()
    }

    private fun refreshState() {
        val ctx = requireContext()
        val serviceOn = AccessibilityHelper.isLiteServiceEnabled(ctx)
        val shortcutOn = AccessibilityHelper.isLiteShortcutEnabled(ctx)

        when {
            serviceOn && !shortcutOn -> {
                topIcon.setImageResource(R.drawable.ic_accessibility_24)
                applyNormalAccent()
                statusText.setText(R.string.onb_accessibility_body_enabled)
                openButton.visibility = View.GONE
                hintCard.visibility = View.GONE
                setStepComplete(true)
            }
            serviceOn && shortcutOn -> {
                topIcon.setImageResource(R.drawable.ic_warning_24)
                applyErrorAccent()
                statusText.setText(R.string.onb_accessibility_shortcut_title)
                hintHeader.visibility = View.GONE
                hintText.setText(R.string.onb_accessibility_shortcut_body)
                openButton.setText(R.string.onb_accessibility_shortcut_open)
                openButton.visibility = View.VISIBLE
                hintCard.visibility = View.VISIBLE
                setStepComplete(false)
            }
            else -> {
                topIcon.setImageResource(R.drawable.ic_accessibility_24)
                applyWarningAccent()
                statusText.setText(R.string.onb_accessibility_body_pending)
                hintHeader.visibility = View.VISIBLE
                hintTitle.setText(R.string.onb_accessibility_hint_warning_title)
                hintText.setText(R.string.onb_accessibility_hint)
                openButton.setText(R.string.onb_accessibility_open)
                openButton.visibility = View.VISIBLE
                hintCard.visibility = View.VISIBLE
                setStepComplete(false)
            }
        }
    }

    private fun applyErrorAccent() {
        val error = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorError)
        val errContainer = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorErrorContainer)
        val onErrContainer = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnErrorContainer)
        topIcon.imageTintList = ColorStateList.valueOf(error)
        statusText.setTextColor(error)
        hintCard.setCardBackgroundColor(errContainer)
        hintCard.strokeWidth = 0
        hintIcon.imageTintList = ColorStateList.valueOf(onErrContainer)
        hintTitle.setTextColor(onErrContainer)
        hintText.setTextColor(onErrContainer)
    }

    /**
     * Neutral outlined card with a warning-coloured icon. Stays out of the way
     * of the dynamic colour palette — no guessing which hue Monet lands on —
     * while still reading as "heads up, not a failure" via the amber/red icon.
     */
    private fun applyWarningAccent() {
        val primary = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimary)
        val error = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorError)
        val onSurface = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface)
        val onSurfaceVariant = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurfaceVariant)
        val surface = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorSurface)
        val outlineVariant = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOutlineVariant)
        topIcon.imageTintList = ColorStateList.valueOf(primary)
        statusText.setTextColor(onSurface)
        hintCard.setCardBackgroundColor(surface)
        hintCard.strokeColor = outlineVariant
        hintCard.strokeWidth = resources.displayMetrics.density.toInt()
        hintIcon.imageTintList = ColorStateList.valueOf(error)
        hintTitle.setTextColor(onSurface)
        hintText.setTextColor(onSurfaceVariant)
    }

    private fun applyNormalAccent() {
        val primary = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimary)
        val onSurface = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface)
        val surfaceVariant = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorSurfaceVariant)
        val onSurfaceVariant = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurfaceVariant)
        topIcon.imageTintList = ColorStateList.valueOf(primary)
        statusText.setTextColor(onSurface)
        hintCard.setCardBackgroundColor(surfaceVariant)
        hintCard.strokeWidth = 0
        hintIcon.imageTintList = ColorStateList.valueOf(onSurfaceVariant)
        hintTitle.setTextColor(onSurfaceVariant)
        hintText.setTextColor(onSurfaceVariant)
    }
}
