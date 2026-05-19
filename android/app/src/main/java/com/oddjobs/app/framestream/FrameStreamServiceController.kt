package com.oddjobs.app.framestream

import android.content.Context
import android.content.Intent
import android.os.Build

class FrameStreamServiceController(private val context: Context) {
    fun start(config: FrameStreamConfig) {
        launchService(
            action = FrameStreamService.ACTION_START,
            config = config
        )
    }

    fun pause() {
        launchService(FrameStreamService.ACTION_PAUSE)
    }

    fun stop() {
        launchService(FrameStreamService.ACTION_STOP)
    }

    private fun launchService(
        action: String,
        config: FrameStreamConfig? = null
    ) {
        val intent = Intent(context, FrameStreamService::class.java).apply {
            this.action = action
            if (config != null) {
                putExtra(FrameStreamService.EXTRA_INTERVAL, config.interval.name)
                putExtra(FrameStreamService.EXTRA_QUALITY, config.quality.name)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
