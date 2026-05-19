package com.oddjobs.app.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = AppSettingsStore(application)
    val uiState: StateFlow<SettingsUiState> = settingsStore.state

    fun updateBackendUrl(url: String) {
        settingsStore.updateBackendUrl(url)
    }

    fun toggleDebugLogs() {
        settingsStore.updateDebugLogsEnabled(!uiState.value.debugLogsEnabled)
    }
}
