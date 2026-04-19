package com.govorun.lite.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.R
import com.govorun.lite.model.ModelDownloadManager
import kotlinx.coroutines.launch

/**
 * Step 6: download the GigaAM v3 model files (~327 MB).
 *
 * Observes [ModelDownloadManager] state so download survives config changes
 * and re-focusing. Primary button cycles through Start → Cancel (during
 * download) → Retry (after failure). Step is complete when state is Installed.
 */
class ModelDownloadFragment : OnboardingStepFragment() {

    private lateinit var statusText: MaterialTextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var progressText: MaterialTextView
    private lateinit var primaryButton: MaterialButton
    private lateinit var secondaryButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_onboarding_model, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.modelStatus)
        progressBar = view.findViewById(R.id.modelProgress)
        progressText = view.findViewById(R.id.modelProgressText)
        primaryButton = view.findViewById(R.id.modelPrimary)
        secondaryButton = view.findViewById(R.id.modelSecondary)

        ModelDownloadManager.refreshInstalledStatus(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ModelDownloadManager.state.collect { render(it) }
            }
        }
    }

    override fun onStepFocused() {
        ModelDownloadManager.refreshInstalledStatus(requireContext())
    }

    private fun render(state: ModelDownloadManager.State) {
        when (state) {
            is ModelDownloadManager.State.Idle -> {
                statusText.setText(R.string.onb_model_body)
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                primaryButton.visibility = View.VISIBLE
                primaryButton.setText(R.string.onb_model_start)
                primaryButton.setOnClickListener {
                    ModelDownloadManager.start(requireContext())
                }
                secondaryButton.visibility = View.GONE
                setStepComplete(false)
            }
            is ModelDownloadManager.State.Running -> {
                statusText.setText(R.string.onb_model_body)
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE

                val total = state.totalBytes.coerceAtLeast(1L)
                val percent = ((state.bytesDownloaded * 100L) / total).toInt().coerceIn(0, 100)
                progressBar.isIndeterminate = state.bytesDownloaded <= 0L
                if (!progressBar.isIndeterminate) {
                    progressBar.setProgressCompat(percent, true)
                }
                progressText.text = getString(
                    R.string.onb_model_progress,
                    percent,
                    formatMb(state.bytesDownloaded),
                    formatMb(state.totalBytes)
                )

                primaryButton.visibility = View.GONE
                secondaryButton.visibility = View.VISIBLE
                secondaryButton.setText(R.string.onb_model_cancel)
                secondaryButton.setOnClickListener { ModelDownloadManager.cancel() }
                setStepComplete(false)
            }
            is ModelDownloadManager.State.Installed -> {
                statusText.setText(R.string.onb_model_installed)
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                primaryButton.visibility = View.GONE
                secondaryButton.visibility = View.GONE
                setStepComplete(true)
            }
            is ModelDownloadManager.State.Failed -> {
                statusText.text = getString(R.string.onb_model_failed_prefix, state.message)
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                primaryButton.visibility = View.VISIBLE
                primaryButton.setText(R.string.onb_model_retry)
                primaryButton.setOnClickListener {
                    ModelDownloadManager.retry(requireContext())
                }
                secondaryButton.visibility = View.GONE
                setStepComplete(false)
            }
        }
    }

    private fun formatMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(java.util.Locale.ROOT, "%.1f МБ", mb)
    }
}
