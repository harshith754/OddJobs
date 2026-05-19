package com.oddjobs.app.settings

data class SettingsUiState(
    val backendUrl: String = AppSettingsStore.DEFAULT_BACKEND_URL,
    val debugLogsEnabled: Boolean = false
)
