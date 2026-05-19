package com.oddjobs.app.framestream

import com.oddjobs.app.settings.AppSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpFrameSessionAdminRepository(
    private val settingsStore: AppSettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun listSessions(): List<SessionSummary> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${requireBackendUrl()}/api/streams/sessions")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("List sessions failed (${response.code}): $body")
            }

            val json = JSONObject(body)
            parseSessions(json.getJSONArray("sessions"))
        }
    }

    suspend fun deleteSession(sessionId: String): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${requireBackendUrl()}/api/streams/$sessionId")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Delete session failed (${response.code}): $body")
            }

            JSONObject(body).optInt("deletedImages", 0)
        }
    }

    private fun parseSessions(array: JSONArray): List<SessionSummary> {
        return buildList(array.length()) {
            repeat(array.length()) { index ->
                val session = array.getJSONObject(index)
                add(
                    SessionSummary(
                        id = session.getString("id"),
                        status = session.getString("status"),
                        startedAt = session.getString("startedAt"),
                        endedAt = session.optString("endedAt").takeIf { it.isNotBlank() && it != "null" },
                        lastImageAt = session.optString("lastImageAt").takeIf { it.isNotBlank() && it != "null" },
                        imageCount = session.optInt("imageCount", 0)
                    )
                )
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
}
