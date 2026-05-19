package com.oddjobs.app.settings

import com.oddjobs.app.framestream.CaptureInterval
import com.oddjobs.app.framestream.QualityMode

data class SettingsUiState(
    val backendUrl: String = "https://oddjobs.app",
    val defaultInterval: CaptureInterval = CaptureInterval.TwoSeconds,
    val defaultQuality: QualityMode = QualityMode.High,
    val keepFramesForHours: Int = 24,
    val debugLogsEnabled: Boolean = false
)

