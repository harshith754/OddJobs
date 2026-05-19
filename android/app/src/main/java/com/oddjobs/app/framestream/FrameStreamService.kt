package com.oddjobs.app.framestream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.oddjobs.app.MainActivity
import com.oddjobs.app.settings.AppSettingsStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FrameStreamService : LifecycleService() {
    private lateinit var orchestrator: FrameStreamOrchestrator
    private lateinit var cameraLifecycleOwner: ServiceCameraLifecycleOwner
    private var lastConfig: FrameStreamConfig = FrameStreamConfig()

    override fun onCreate() {
        super.onCreate()
        cameraLifecycleOwner = ServiceCameraLifecycleOwner().apply { start() }
        orchestrator = FrameStreamOrchestrator(
            captureEngine = CameraXFrameCaptureEngine(
                context = this,
                lifecycleOwner = cameraLifecycleOwner
            ),
            repository = HttpFrameUploadRepository(AppSettingsStore(this))
        )
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(FrameStreamRuntime.state.value))
        lifecycleScope.launch {
            FrameStreamRuntime.state.collect { state ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = intent.toConfig()
                lastConfig = config
                FrameStreamRuntime.update(
                    FrameStreamServiceState(
                        status = StreamStatus.Starting,
                        serviceRunning = true,
                        lastUploadSummary = "Preparing camera and upload session"
                    )
                )
                orchestrator.start(config)
            }
            ACTION_RESUME -> {
                FrameStreamRuntime.update(
                    FrameStreamServiceState(
                        status = StreamStatus.Starting,
                        serviceRunning = true,
                        session = FrameStreamRuntime.state.value.session,
                        lastUploadSummary = "Resuming current recording session",
                        latestFramePath = FrameStreamRuntime.state.value.latestFramePath
                    )
                )
                orchestrator.start(lastConfig)
            }
            ACTION_PAUSE -> {
                orchestrator.pause()
            }
            ACTION_STOP -> {
                orchestrator.stop()
            }
            else -> Unit
        }

        if (intent?.action == ACTION_STOP) {
            cameraLifecycleOwner.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_STICKY
    }
    override fun onDestroy() {
        orchestrator.shutdown()
        cameraLifecycleOwner.destroy()
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

    private fun buildNotification(state: FrameStreamServiceState): Notification {
        val title = when (state.status) {
            StreamStatus.Running -> "Frame Stream recording"
            StreamStatus.Starting -> "Frame Stream starting"
            StreamStatus.Paused -> "Frame Stream paused"
            StreamStatus.UploadFailed -> "Frame Stream upload failed"
            StreamStatus.CameraError -> "Frame Stream camera error"
            StreamStatus.Stopped -> "Frame Stream inactive"
        }
        val sessionLabel = state.session.sessionId
            ?.takeLast(8)
            ?.let { "Session $it" }
            ?: "No session yet"
        val contentText = when (state.status) {
            StreamStatus.Running ->
                "$sessionLabel • Recording • ${state.session.uploadedImages} frames uploaded"
            StreamStatus.Starting ->
                "$sessionLabel • Preparing camera and stream upload"
            StreamStatus.Paused ->
                "$sessionLabel • Capture paused"
            StreamStatus.UploadFailed ->
                "$sessionLabel • ${state.session.lastError ?: "Upload failed"}"
            StreamStatus.CameraError ->
                "$sessionLabel • ${state.session.lastError ?: "Camera error"}"
            StreamStatus.Stopped ->
                "Frame Stream is ready"
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(Notification.BigTextStyle().bigText(contentText))
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .setContentIntent(buildOpenAppIntent())
            .addAction(buildPrimaryAction(state))
            .addAction(buildStopAction())
            .build()
    }

    private fun buildPrimaryAction(state: FrameStreamServiceState): Notification.Action {
        return if (state.status == StreamStatus.Running || state.status == StreamStatus.Starting) {
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                buildServiceActionIntent(ACTION_PAUSE, REQUEST_CODE_PAUSE)
            ).build()
        } else {
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                "Resume",
                buildServiceActionIntent(ACTION_RESUME, REQUEST_CODE_RESUME)
            ).build()
        }
    }

    private fun buildStopAction(): Notification.Action {
        return Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            buildServiceActionIntent(ACTION_STOP, REQUEST_CODE_STOP)
        ).build()
    }

    private fun buildOpenAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildServiceActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, FrameStreamService::class.java).apply {
            this.action = action
            putExtra(EXTRA_INTERVAL, lastConfig.interval.name)
            putExtra(EXTRA_QUALITY, lastConfig.quality.name)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_START = "com.oddjobs.app.framestream.START"
        const val ACTION_RESUME = "com.oddjobs.app.framestream.RESUME"
        const val ACTION_PAUSE = "com.oddjobs.app.framestream.PAUSE"
        const val ACTION_STOP = "com.oddjobs.app.framestream.STOP"
        const val EXTRA_INTERVAL = "extra_interval"
        const val EXTRA_QUALITY = "extra_quality"

        private const val CHANNEL_ID = "frame_stream"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_OPEN_APP = 1001
        private const val REQUEST_CODE_PAUSE = 1002
        private const val REQUEST_CODE_RESUME = 1003
        private const val REQUEST_CODE_STOP = 1004
    }
}

private fun Intent.toConfig(): FrameStreamConfig {
    val interval = getStringExtra(FrameStreamService.EXTRA_INTERVAL)
        ?.let(CaptureInterval::valueOf)
        ?: CaptureInterval.TwoSeconds
    val quality = getStringExtra(FrameStreamService.EXTRA_QUALITY)
        ?.let(QualityMode::valueOf)
        ?: QualityMode.High

    return FrameStreamConfig(
        interval = interval,
        quality = quality
    )
}
