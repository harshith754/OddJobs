package com.oddjobs.app.framestream

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FrameStreamViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FrameStreamUiState())
    val uiState: StateFlow<FrameStreamUiState> = _uiState.asStateFlow()

    fun setInterval(interval: CaptureInterval) {
        _uiState.update { it.copy(interval = interval) }
    }

    fun setQuality(quality: QualityMode) {
        _uiState.update { it.copy(quality = quality) }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    fun startStream() {
        _uiState.update {
            it.copy(
                status = StreamStatus.Starting,
                serviceRunning = true,
                session = it.session.copy(
                    sessionId = "session-${System.currentTimeMillis()}",
                    lastStartedAt = java.time.Instant.now().toString(),
                    lastError = null
                ),
                lastUploadSummary = "Preparing camera and upload session"
            )
        }
        _uiState.update {
            it.copy(
                status = StreamStatus.Running,
                serviceRunning = true,
                lastUploadSummary = "Waiting for first upload"
            )
        }
    }

    fun pauseStream() {
        _uiState.update {
            it.copy(
                status = StreamStatus.Paused,
                lastUploadSummary = "Capture paused, viewer remains available"
            )
        }
    }

    fun stopStream() {
        _uiState.update {
            it.copy(
                status = StreamStatus.Stopped,
                serviceRunning = false,
                lastUploadSummary = "Stream stopped",
                session = it.session.copy(sessionId = null)
            )
        }
    }

    fun markUploadSuccess(uploadedImages: Int) {
        _uiState.update {
            it.copy(
                uploadedImages = uploadedImages,
                lastUploadSummary = "Last upload succeeded just now",
                status = StreamStatus.Running
            )
        }
    }

    fun markError(message: String, cameraRelated: Boolean = false) {
        _uiState.update {
            it.copy(
                status = if (cameraRelated) StreamStatus.CameraError else StreamStatus.UploadFailed,
                session = it.session.copy(lastError = message),
                lastUploadSummary = message
            )
        }
    }
}
