package com.govorun.lite.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.R

/**
 * Step 2: RECORD_AUDIO permission. The step is marked complete whenever the
 * runtime permission check returns GRANTED — this covers three flows:
 *   - first launch: user taps "Разрешить доступ", system dialog, grant
 *   - returning user: permission already granted, step auto-completes
 *   - permanent deny: user is routed to app settings; onResume re-checks
 *     after they come back
 */
class MicPermissionFragment : OnboardingStepFragment() {

    private lateinit var statusText: MaterialTextView
    private lateinit var grantButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var deniedHint: MaterialTextView
    private lateinit var chooseHint: View

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            refreshState()
        } else {
            // After a deny we may be in "don't ask again" territory — surface
            // the settings shortcut. shouldShowRequestPermissionRationale
            // returns false both before the first request and after a
            // permanent deny; combined with a prior refusal we treat it as
            // permanent.
            val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            settingsButton.visibility = if (permanentlyDenied) View.VISIBLE else View.GONE
            deniedHint.visibility = if (permanentlyDenied) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding_mic, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.micStatus)
        grantButton = view.findViewById(R.id.micGrant)
        settingsButton = view.findViewById(R.id.micOpenSettings)
        deniedHint = view.findViewById(R.id.micDeniedHint)
        chooseHint = view.findViewById(R.id.micChooseHint)

        grantButton.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        settingsButton.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
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
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            statusText.setText(R.string.onb_mic_granted)
            grantButton.visibility = View.GONE
            settingsButton.visibility = View.GONE
            deniedHint.visibility = View.GONE
            chooseHint.visibility = View.GONE
        } else {
            statusText.setText(R.string.onb_mic_body)
            grantButton.visibility = View.VISIBLE
            chooseHint.visibility = View.VISIBLE
        }

        setStepComplete(granted)
    }
}
