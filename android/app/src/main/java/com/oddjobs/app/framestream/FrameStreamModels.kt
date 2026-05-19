package com.oddjobs.app.framestream

enum class CaptureInterval(val seconds: Int, val label: String) {
    OneSecond(1, "1s"),
    TwoSeconds(2, "2s"),
    FiveSeconds(5, "5s"),
    Manual(0, "Manual")
}

enum class QualityMode(val label: String, val description: String) {
    Balanced("Balanced", "Lighter upload, lower detail"),
    High("High", "Best default for text and screen reading"),
    Max("Max", "Highest detail, higher thermal/load risk")
}

enum class StreamStatus {
    Stopped,
    Starting,
    Running,
    Paused,
    UploadFailed,
    CameraError
}

data class StreamSessionSnapshot(
    val sessionId: String? = null,
    val lastStartedAt: String? = null,
    val lastError: String? = null,
    val uploadedImages: Int = 0
)

data class FrameStreamConfig(
    val interval: CaptureInterval = CaptureInterval.TwoSeconds,
    val quality: QualityMode = QualityMode.High,
    val torchEnabled: Boolean = false
)

data class FrameStreamUiState(
    val interval: CaptureInterval = CaptureInterval.TwoSeconds,
    val quality: QualityMode = QualityMode.High,
    val status: StreamStatus = StreamStatus.Stopped,
    val torchEnabled: Boolean = false,
    val uploadedImages: Int = 0,
    val lastUploadSummary: String = "No uploads yet",
    val viewerUrl: String = "https://oddjobs.app/s/main-frame-stream",
    val serviceRunning: Boolean = false,
    val session: StreamSessionSnapshot = StreamSessionSnapshot()
)

data class FramePayload(
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val description: String
)

data class UploadReceipt(
    val uploadedImages: Int,
    val uploadedAt: String,
    val summary: String
)

data class FrameStreamServiceState(
    val status: StreamStatus = StreamStatus.Stopped,
    val serviceRunning: Boolean = false,
    val session: StreamSessionSnapshot = StreamSessionSnapshot(),
    val lastUploadSummary: String = "No uploads yet"
)

fun QualityMode.toPayload(): FramePayload = when (this) {
    QualityMode.Balanced -> FramePayload(
        width = 1440,
        height = 1080,
        fileSizeBytes = 280_000,
        description = "Balanced quality frame prepared"
    )
    QualityMode.High -> FramePayload(
        width = 1920,
        height = 1080,
        fileSizeBytes = 520_000,
        description = "High quality frame prepared"
    )
    QualityMode.Max -> FramePayload(
        width = 2560,
        height = 1440,
        fileSizeBytes = 860_000,
        description = "Max quality frame prepared"
    )
}

fun StreamStatus.displayName(): String = when (this) {
    StreamStatus.Stopped -> "Stopped"
    StreamStatus.Starting -> "Starting"
    StreamStatus.Running -> "Running"
    StreamStatus.Paused -> "Paused"
    StreamStatus.UploadFailed -> "Upload failed"
    StreamStatus.CameraError -> "Camera error"
}
