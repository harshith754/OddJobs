package com.oddjobs.app.framestream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oddjobs.app.settings.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FrameStreamViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = AppSettingsStore(application)
    private val sessionAdminRepository = HttpFrameSessionAdminRepository(settingsStore)
    private val controlsState = MutableStateFlow(FrameStreamConfig())
    private val sessionsState = MutableStateFlow(SessionBrowserState())
    val uiState: StateFlow<FrameStreamUiState> =
        combine(
            controlsState,
            FrameStreamRuntime.state,
            settingsStore.state,
            sessionsState
        ) { controls, runtime, _, sessions ->
            FrameStreamUiState(
                interval = controls.interval,
                quality = controls.quality,
                status = runtime.status,
                uploadedImages = runtime.session.uploadedImages,
                lastUploadSummary = runtime.lastUploadSummary,
                viewerUrl = runtime.session.viewerUrl
                    ?: settingsStore.buildViewerUrl(
                        runtime.session.streamToken ?: AppSettingsStore.DEFAULT_STREAM_TOKEN
                    ),
                serviceRunning = runtime.serviceRunning,
                session = runtime.session,
                latestFramePath = runtime.latestFramePath,
                sessions = sessions.items,
                sessionsLoading = sessions.loading,
                deletingSessionId = sessions.deletingSessionId,
                sessionsMessage = sessions.message
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FrameStreamUiState()
        )

    init {
        refreshSessions()
    }

    fun setInterval(interval: CaptureInterval) {
        controlsState.update { it.copy(interval = interval) }
    }

    fun setQuality(quality: QualityMode) {
        controlsState.update { it.copy(quality = quality) }
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

    fun refreshSessions() {
        viewModelScope.launch {
            sessionsState.update {
                it.copy(
                    loading = true,
                    message = null
                )
            }
            runCatching { sessionAdminRepository.listSessions() }
                .onSuccess { sessions ->
                    sessionsState.update {
                        it.copy(
                            items = sessions,
                            loading = false,
                            deletingSessionId = null,
                            message = null
                        )
                    }
                }
                .onFailure { error ->
                    sessionsState.update {
                        it.copy(
                            loading = false,
                            deletingSessionId = null,
                            message = error.message ?: "Failed to load sessions"
                        )
                    }
                }
        }
    }

    fun deleteSession(sessionId: String) {
        if (uiState.value.serviceRunning && uiState.value.session.sessionId == sessionId) {
            sessionsState.update {
                it.copy(message = "Stop the current stream before deleting its session.")
            }
            return
        }

        viewModelScope.launch {
            sessionsState.update {
                it.copy(
                    deletingSessionId = sessionId,
                    message = null
                )
            }
            runCatching { sessionAdminRepository.deleteSession(sessionId) }
                .onSuccess { deletedImages ->
                    val remaining = sessionsState.value.items.filterNot { it.id == sessionId }
                    sessionsState.update {
                        it.copy(
                            items = remaining,
                            deletingSessionId = null,
                            message = "Deleted session and $deletedImages images."
                        )
                    }
                }
                .onFailure { error ->
                    sessionsState.update {
                        it.copy(
                            deletingSessionId = null,
                            message = error.message ?: "Failed to delete session"
                        )
                    }
                }
        }
    }
}

private data class SessionBrowserState(
    val items: List<SessionSummary> = emptyList(),
    val loading: Boolean = false,
    val deletingSessionId: String? = null,
    val message: String? = null
)
