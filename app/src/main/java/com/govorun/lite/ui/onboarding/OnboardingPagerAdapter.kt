package com.govorun.lite.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val steps = OnboardingStep.entries

    override fun getItemCount(): Int = steps.size

    override fun createFragment(position: Int): Fragment = when (steps[position]) {
        OnboardingStep.WELCOME -> WelcomeFragment()
        OnboardingStep.MIC -> MicPermissionFragment()
        OnboardingStep.ACCESSIBILITY -> AccessibilityFragment()
        OnboardingStep.BATTERY -> BatteryFragment()
        OnboardingStep.MODEL -> ModelDownloadFragment()
        OnboardingStep.TEST -> TestFragment()
    }

    fun stepAt(position: Int): OnboardingStep = steps[position]
}
