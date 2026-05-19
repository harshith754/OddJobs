package com.oddjobs.app.framestream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class FrameStreamService : Service() {
    private val orchestrator = FrameStreamOrchestrator(StubFrameUploadRepository())

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Frame Stream is ready"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val contentText = when (intent?.action) {
            ACTION_START -> {
                val config = intent.toConfig()
                FrameStreamRuntime.update(
                    FrameStreamServiceState(
                        status = StreamStatus.Starting,
                        serviceRunning = true,
                        lastUploadSummary = "Preparing camera and upload session"
                    )
                )
                orchestrator.start(config)
                "Capturing frames in the background"
            }
            ACTION_PAUSE -> {
                orchestrator.pause()
                "Capture paused"
            }
            ACTION_STOP -> {
                orchestrator.stop()
                "Stopping stream"
            }
            else -> "Frame Stream active"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))

        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        orchestrator.shutdown()
        FrameStreamRuntime.reset()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Frame Stream",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("OddJobs Frame Stream")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.oddjobs.app.framestream.START"
        const val ACTION_PAUSE = "com.oddjobs.app.framestream.PAUSE"
        const val ACTION_STOP = "com.oddjobs.app.framestream.STOP"
        const val EXTRA_INTERVAL = "extra_interval"
        const val EXTRA_QUALITY = "extra_quality"
        const val EXTRA_TORCH = "extra_torch"

        private const val CHANNEL_ID = "frame_stream"
        private const val NOTIFICATION_ID = 1001
    }
}

private fun Intent.toConfig(): FrameStreamConfig {
    val interval = getStringExtra(FrameStreamService.EXTRA_INTERVAL)
        ?.let(CaptureInterval::valueOf)
        ?: CaptureInterval.TwoSeconds
    val quality = getStringExtra(FrameStreamService.EXTRA_QUALITY)
        ?.let(QualityMode::valueOf)
        ?: QualityMode.High
    val torchEnabled = getBooleanExtra(FrameStreamService.EXTRA_TORCH, false)

    return FrameStreamConfig(
        interval = interval,
        quality = quality,
        torchEnabled = torchEnabled
    )
}
