package com.oddjobs.app.framestream

import com.oddjobs.app.settings.AppSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

interface FrameUploadRepository {
    suspend fun startSession(
        config: FrameStreamConfig,
        existingSession: StreamSessionSnapshot?
    ): StreamSessionSnapshot

    suspend fun uploadFrame(session: StreamSessionSnapshot, framePayload: FramePayload): UploadReceipt
    suspend fun pauseSession(sessionId: String)
    suspend fun endSession(sessionId: String)
}

class HttpFrameUploadRepository(
    private val settingsStore: AppSettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : FrameUploadRepository {
    override suspend fun startSession(
        config: FrameStreamConfig,
        existingSession: StreamSessionSnapshot?
    ): StreamSessionSnapshot = withContext(Dispatchers.IO) {
        if (existingSession?.sessionId != null) {
            postEmpty("/api/streams/${existingSession.sessionId}/resume")
            return@withContext existingSession.copy(
                lastStartedAt = Instant.now().toString(),
                lastError = null
            )
        }

        val payload = JSONObject()
            .put("name", "Main Frame Stream")
            .put(
                "settings",
                JSONObject()
                    .put("intervalSeconds", config.interval.seconds)
                    .put("quality", config.quality.apiValue())
            )

        val response = executeJsonRequest(
            path = "/api/streams",
            method = "POST",
            body = payload.toString()
        )

        val streamToken = response.getString("streamToken")
        StreamSessionSnapshot(
            sessionId = response.getString("sessionId"),
            streamId = response.getString("streamId"),
            streamToken = streamToken,
            viewerUrl = settingsStore.buildViewerUrl(streamToken),
            lastStartedAt = Instant.now().toString(),
            uploadedImages = 0
        )
    }

    override suspend fun uploadFrame(
        session: StreamSessionSnapshot,
        framePayload: FramePayload
    ): UploadReceipt = withContext(Dispatchers.IO) {
        val sessionId = requireNotNull(session.sessionId) { "Missing session id" }
        val sequenceNumber = session.uploadedImages + 1
        val imageBody = framePayload.file.asRequestBody("image/jpeg".toMediaType())
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", framePayload.file.name, imageBody)
            .addFormDataPart("sequenceNumber", sequenceNumber.toString())
            .addFormDataPart("width", framePayload.width.toString())
            .addFormDataPart("height", framePayload.height.toString())
            .addFormDataPart("fileSizeBytes", framePayload.fileSizeBytes.toString())
            .build()

        val request = Request.Builder()
            .url("${requireBackendUrl()}/api/streams/$sessionId/images")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Upload failed (${response.code}): $body")
            }

            val json = JSONObject(body)
            val uploadedImages = json.optInt("sequenceNumber", sequenceNumber)
            UploadReceipt(
                uploadedImages = uploadedImages,
                uploadedAt = Instant.now().toString(),
                summary = "Uploaded frame #$uploadedImages",
                latestFramePath = framePayload.file.absolutePath
            )
        }
    }

    override suspend fun pauseSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            postEmpty("/api/streams/$sessionId/pause")
        }
    }

    override suspend fun endSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            postEmpty("/api/streams/$sessionId/end")
        }
    }

    private fun executeJsonRequest(path: String, method: String, body: String? = null): JSONObject {
        val requestBody = body?.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder()
            .url("${requireBackendUrl()}$path")

        when (method) {
            "POST" -> builder.post(requestBody ?: EMPTY_JSON_BODY)
            "PATCH" -> builder.patch(requestBody ?: EMPTY_JSON_BODY)
            else -> error("Unsupported method: $method")
        }

        return client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("$method $path failed (${response.code}): $responseBody")
            }
            JSONObject(responseBody)
        }
    }

    private fun postEmpty(path: String) {
        val request = Request.Builder()
            .url("${requireBackendUrl()}$path")
            .post(EMPTY_JSON_BODY)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("POST $path failed (${response.code}): $body")
            }
        }
    }

    private fun requireBackendUrl(): String {
        val backendUrl = settingsStore.currentState().backendUrl.trim().removeSuffix("/")
        if (backendUrl.isBlank()) {
            throw IOException("Backend URL is empty. Set it in OddJobs settings.")
        }
        return backendUrl
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val EMPTY_JSON_BODY = "{}".toRequestBody(JSON_MEDIA_TYPE)
    }
}

private fun QualityMode.apiValue(): String = when (this) {
    QualityMode.Balanced -> "balanced"
    QualityMode.High -> "high"
    QualityMode.Max -> "max"
}
