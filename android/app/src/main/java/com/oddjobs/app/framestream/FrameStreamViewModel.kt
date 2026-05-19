package com.oddjobs.app.framestream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class FrameStreamViewModel : ViewModel() {
    private val controlsState = MutableStateFlow(FrameStreamConfig())
    val uiState: StateFlow<FrameStreamUiState> =
        combine(controlsState, FrameStreamRuntime.state) { controls, runtime ->
            FrameStreamUiState(
                interval = controls.interval,
                quality = controls.quality,
                status = runtime.status,
                torchEnabled = controls.torchEnabled,
                uploadedImages = runtime.session.uploadedImages,
                lastUploadSummary = runtime.lastUploadSummary,
                viewerUrl = "https://oddjobs.app/s/main-frame-stream",
                serviceRunning = runtime.serviceRunning,
                session = runtime.session,
                latestFramePath = runtime.latestFramePath
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FrameStreamUiState()
        )

    fun setInterval(interval: CaptureInterval) {
        controlsState.update { it.copy(interval = interval) }
    }

    fun setQuality(quality: QualityMode) {
        controlsState.update { it.copy(quality = quality) }
    }

    fun toggleTorch() {
        controlsState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    fun currentConfig(): FrameStreamConfig {
        return controlsState.value
    }

    fun markStarting() {
        FrameStreamRuntime.update(
            FrameStreamServiceState(
                status = StreamStatus.Starting,
                serviceRunning = true,
                session = uiState.value.session,
                lastUploadSummary = "Preparing camera and upload session",
                latestFramePath = uiState.value.latestFramePath
            )
        )
    }

    fun markStopped() {
        FrameStreamRuntime.reset()
    }

    fun markError(message: String, cameraRelated: Boolean = false) {
        FrameStreamRuntime.update(
            FrameStreamServiceState(
                status = if (cameraRelated) StreamStatus.CameraError else StreamStatus.UploadFailed,
                serviceRunning = false,
                session = uiState.value.session.copy(lastError = message),
                lastUploadSummary = message,
                latestFramePath = uiState.value.latestFramePath
            )
        )
    }
}
