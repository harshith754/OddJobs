package com.oddjobs.app.framestream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FrameStreamRuntime {
    private val mutableState = MutableStateFlow(FrameStreamServiceState())
    val state: StateFlow<FrameStreamServiceState> = mutableState.asStateFlow()

    fun update(state: FrameStreamServiceState) {
        mutableState.value = state
    }

    fun reset() {
        mutableState.value = FrameStreamServiceState()
    }
}

