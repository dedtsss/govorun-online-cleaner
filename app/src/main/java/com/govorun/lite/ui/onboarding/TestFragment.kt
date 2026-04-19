package com.govorun.lite.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.R

/**
 * Step 7: optional end-to-end smoke test.
 *
 * Focusing the field triggers the accessibility bubble, so the user can speak
 * something and see their words land. But we don't gate «Готово» on any text
 * being entered — by this point every required permission has already been
 * verified on prior steps, so forcing another action is friction for nothing.
 */
class TestFragment : OnboardingStepFragment() {

    private lateinit var statusText: MaterialTextView
    private lateinit var field: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding_test, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.testStatus)
        field = view.findViewById(R.id.testField)

        setStepComplete(true)

        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    statusText.setText(R.string.onb_test_done)
                } else {
                    statusText.setText(R.string.onb_test_body)
                }
            }
        })
    }
}
