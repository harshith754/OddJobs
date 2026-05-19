package com.oddjobs.app.framestream

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FrameStreamOrchestrator(
    private val captureEngine: CameraXFrameCaptureEngine,
    private val repository: FrameUploadRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: kotlinx.coroutines.Job? = null
    private var activeConfig: FrameStreamConfig = FrameStreamConfig()
    private var activeSession: StreamSessionSnapshot = StreamSessionSnapshot()

    fun start(config: FrameStreamConfig) {
        activeConfig = config
        captureJob?.cancel()
        captureJob = scope.launch {
            try {
                val session = if (activeSession.sessionId == null) {
                    repository.createOrResumeSession(config)
                } else {
                    activeSession.copy(lastError = null)
                }

                captureEngine.start(config)
                activeSession = session
                FrameStreamRuntime.update(
                    FrameStreamServiceState(
                        status = StreamStatus.Running,
                        serviceRunning = true,
                        session = activeSession,
                        lastUploadSummary = "Waiting for first upload"
                    )
                )

                while (isActive) {
                    if (activeConfig.interval == CaptureInterval.Manual) {
                        break
                    }

                    val frame = captureEngine.capture(activeConfig)
                    val receipt = repository.uploadFrame(
                        sessionId = requireNotNull(activeSession.sessionId),
                        framePayload = frame
                    )

                    activeSession = activeSession.copy(uploadedImages = receipt.uploadedImages)
                    FrameStreamRuntime.update(
                        FrameStreamServiceState(
                            status = StreamStatus.Running,
                            serviceRunning = true,
                            session = activeSession,
                            lastUploadSummary = "${receipt.summary} at ${receipt.uploadedAt}",
                            latestFramePath = receipt.latestFramePath
                        )
                    )

                    delay(activeConfig.interval.seconds * 1_000L)
                }
            } catch (_: CancellationException) {
            } catch (error: Exception) {
                activeSession = activeSession.copy(lastError = error.message ?: "Unknown upload error")
                FrameStreamRuntime.update(
                    FrameStreamServiceState(
                        status = StreamStatus.UploadFailed,
                        serviceRunning = true,
                        session = activeSession,
                        lastUploadSummary = activeSession.lastError ?: "Upload error",
                        latestFramePath = FrameStreamRuntime.state.value.latestFramePath
                    )
                )
            }
        }
    }

    fun pause() {
        captureJob?.cancel()
        captureEngine.stop()
        FrameStreamRuntime.update(
            FrameStreamServiceState(
                status = StreamStatus.Paused,
                serviceRunning = true,
                session = activeSession,
                lastUploadSummary = "Capture paused, session preserved",
                latestFramePath = FrameStreamRuntime.state.value.latestFramePath
            )
        )
    }

    fun stop() {
        captureJob?.cancel()
        captureEngine.stop()
        val sessionId = activeSession.sessionId
        if (sessionId != null) {
            scope.launch {
                repository.endSession(sessionId)
            }
        }
        activeSession = StreamSessionSnapshot()
        FrameStreamRuntime.reset()
    }

    fun shutdown() {
        captureJob?.cancel()
        captureEngine.stop()
        scope.cancel()
    }
}
