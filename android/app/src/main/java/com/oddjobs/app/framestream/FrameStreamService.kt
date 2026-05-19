package com.oddjobs.app.framestream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class FrameStreamService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Frame Stream is ready"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val contentText = when (intent?.action) {
            ACTION_START -> "Capturing frames in the background"
            ACTION_PAUSE -> "Capture paused"
            ACTION_STOP -> "Stopping stream"
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

        private const val CHANNEL_ID = "frame_stream"
        private const val NOTIFICATION_ID = 1001
    }
}

