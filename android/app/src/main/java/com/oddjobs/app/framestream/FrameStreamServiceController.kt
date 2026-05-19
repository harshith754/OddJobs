package com.oddjobs.app.framestream

import android.content.Context
import android.content.Intent
import android.os.Build

class FrameStreamServiceController(private val context: Context) {
    fun start() {
        launchService(FrameStreamService.ACTION_START)
    }

    fun pause() {
        launchService(FrameStreamService.ACTION_PAUSE)
    }

    fun stop() {
        launchService(FrameStreamService.ACTION_STOP)
    }

    private fun launchService(action: String) {
        val intent = Intent(context, FrameStreamService::class.java).apply {
            this.action = action
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

