package com.oddjobs.app.framestream

interface FrameUploadRepository {
    suspend fun createOrResumeSession(config: FrameStreamConfig): StreamSessionSnapshot
    suspend fun uploadFrame(sessionId: String, framePayload: FramePayload): UploadReceipt
    suspend fun endSession(sessionId: String)
}

class StubFrameUploadRepository : FrameUploadRepository {
    private var uploadedImages = 0

    override suspend fun createOrResumeSession(config: FrameStreamConfig): StreamSessionSnapshot {
        uploadedImages = 0
        return StreamSessionSnapshot(
            sessionId = "session-${System.currentTimeMillis()}",
            lastStartedAt = java.time.Instant.now().toString()
        )
    }

    override suspend fun uploadFrame(sessionId: String, framePayload: FramePayload): UploadReceipt {
        uploadedImages += 1
        return UploadReceipt(
            uploadedImages = uploadedImages,
            uploadedAt = java.time.Instant.now().toString(),
            summary = "${framePayload.file.name} captured for $sessionId",
            latestFramePath = framePayload.file.absolutePath
        )
    }

    override suspend fun endSession(sessionId: String) {
    }
}
