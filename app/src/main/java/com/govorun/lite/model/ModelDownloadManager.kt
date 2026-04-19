package com.govorun.lite.model

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-scoped holder for GigaAM model download state.
 *
 * A single background job runs per process, so rotating the screen or
 * re-focusing the onboarding step does not kick off duplicate downloads.
 * UI observes [state]; calls to [start] while a download is already running
 * are idempotent. [cancel] cancels the job; [retry] restarts after a failure.
 */
object ModelDownloadManager {

    sealed class State {
        object Idle : State()
        object Installed : State()
        data class Running(
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val currentFile: String
        ) : State()
        data class Failed(val message: String) : State()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Refresh [state] against on-disk model files. Safe to call anytime. */
    fun refreshInstalledStatus(context: Context) {
        val current = _state.value
        if (current is State.Running) return
        _state.value = if (GigaAmModel.isInstalled(context)) State.Installed else State.Idle
    }

    fun start(context: Context) {
        if (job?.isActive == true) return
        if (GigaAmModel.isInstalled(context)) {
            _state.value = State.Installed
            return
        }
        _state.value = State.Running(0L, GigaAmModel.TOTAL_BYTES, "")
        val appContext = context.applicationContext
        job = scope.launch {
            val result = ModelDownloader.downloadAll(appContext) { p ->
                _state.value = State.Running(p.bytesDownloaded, p.totalBytes, p.currentFile)
            }
            _state.value = when (result) {
                is ModelDownloader.Result.Success -> State.Installed
                is ModelDownloader.Result.Failed -> State.Failed(result.message)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = State.Idle
    }

    fun retry(context: Context) {
        job?.cancel()
        job = null
        start(context)
    }
}
