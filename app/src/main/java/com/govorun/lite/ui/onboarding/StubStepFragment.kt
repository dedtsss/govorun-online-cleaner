package com.govorun.lite.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.R

/**
 * Placeholder fragment for steps that don't have their real implementation
 * yet. Shows the step's title, its eventual-purpose body, and a "coming soon"
 * note. Auto-completes so the skeleton flow can be exercised end-to-end.
 */
class StubStepFragment : OnboardingStepFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding_stub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        view.findViewById<MaterialTextView>(R.id.stubTitle).setText(args.getInt(ARG_TITLE))
        view.findViewById<MaterialTextView>(R.id.stubBody).setText(args.getInt(ARG_BODY))
        setStepComplete(true)
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_BODY = "body"

        fun forStep(step: OnboardingStep): StubStepFragment {
            val titleBody = when (step) {
                OnboardingStep.TEST -> R.string.onb_test_title to R.string.onb_test_body
                else -> throw IllegalArgumentException("Step $step has a dedicated fragment")
            }
            return StubStepFragment().apply {
                arguments = bundleOf(ARG_TITLE to titleBody.first, ARG_BODY to titleBody.second)
            }
        }
    }
}
