package com.oddjobs.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(loadState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            mutableState.value = loadState()
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun currentState(): SettingsUiState = mutableState.value

    fun updateBackendUrl(rawUrl: String) {
        preferences.edit()
            .putString(KEY_BACKEND_URL, normalizeBackendUrl(rawUrl))
            .apply()
    }

    fun updateDebugLogsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DEBUG_LOGS_ENABLED, enabled)
            .apply()
    }

    fun buildViewerUrl(streamToken: String): String {
        val backendUrl = currentState().backendUrl.ifBlank { DEFAULT_BACKEND_URL }
        return "${backendUrl.removeSuffix("/")}/s/$streamToken"
    }

    private fun loadState(): SettingsUiState {
        return SettingsUiState(
            backendUrl = preferences.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL).orEmpty(),
            debugLogsEnabled = preferences.getBoolean(KEY_DEBUG_LOGS_ENABLED, false)
        )
    }

    private fun normalizeBackendUrl(rawUrl: String): String {
        return rawUrl.trim().removeSuffix("/")
    }

    companion object {
        const val DEFAULT_BACKEND_URL = "https://oddjobs.app"
        const val DEFAULT_STREAM_TOKEN = "main-frame-stream"

        private const val PREFERENCES_NAME = "oddjobs_settings"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DEBUG_LOGS_ENABLED = "debug_logs_enabled"
    }
}
