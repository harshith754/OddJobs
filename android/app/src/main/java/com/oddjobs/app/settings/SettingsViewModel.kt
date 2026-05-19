package com.oddjobs.app.settings

import androidx.lifecycle.ViewModel
import com.oddjobs.app.framestream.CaptureInterval
import com.oddjobs.app.framestream.QualityMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateBackendUrl(url: String) {
        _uiState.update { it.copy(backendUrl = url) }
    }

    fun updateInterval(interval: CaptureInterval) {
        _uiState.update { it.copy(defaultInterval = interval) }
    }

    fun updateQuality(quality: QualityMode) {
        _uiState.update { it.copy(defaultQuality = quality) }
    }

    fun updateRetentionHours(hours: Int) {
        _uiState.update { it.copy(keepFramesForHours = hours.coerceAtLeast(1)) }
    }

    fun toggleDebugLogs() {
        _uiState.update { it.copy(debugLogsEnabled = !it.debugLogsEnabled) }
    }
}

