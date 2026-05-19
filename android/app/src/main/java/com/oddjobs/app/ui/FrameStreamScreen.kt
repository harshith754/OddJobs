package com.oddjobs.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oddjobs.app.framestream.CaptureInterval
import com.oddjobs.app.framestream.FrameStreamConfig
import com.oddjobs.app.framestream.FrameStreamServiceController
import com.oddjobs.app.framestream.FrameStreamViewModel
import com.oddjobs.app.framestream.QualityMode
import com.oddjobs.app.framestream.StreamStatus
import com.oddjobs.app.framestream.displayName
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameStreamScreen(
    viewModel: FrameStreamViewModel,
    navigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val serviceController = FrameStreamServiceController(context)
    var pendingStart by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted && pendingStart) {
            pendingStart = false
            viewModel.markStarting()
            serviceController.start(
                FrameStreamConfig(
                    interval = state.interval,
                    quality = state.quality
                )
            )
        } else if (!granted) {
            pendingStart = false
            viewModel.markError(
                "Camera permission is required to start Frame Stream",
                cameraRelated = true
            )
        }
    }

    LaunchedEffect(Unit) {
        cameraGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    val latestBitmap = remember(state.latestFramePath) {
        state.latestFramePath
            ?.takeIf { File(it).exists() }
            ?.let(BitmapFactory::decodeFile)
            ?.asImageBitmap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frame Stream") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Stream Status", style = MaterialTheme.typography.titleMedium)
                    Text("State: ${state.status.displayName()}")
                    Text("Service: ${if (state.serviceRunning) "Foreground active" else "Inactive"}")
                    state.session.sessionId?.let { Text("Session: $it") }
                    state.session.lastStartedAt?.let { Text("Started: $it") }
                    Text("Last upload: ${state.lastUploadSummary}")
                    Text("Uploaded images: ${state.uploadedImages}")
                    state.session.lastError?.let { Text("Error: $it") }
                    if (!cameraGranted) {
                        Text("Camera permission not granted", color = Color(0xFFFFB4AB))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Capture Interval", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CaptureInterval.entries.forEach { interval ->
                            FilterChip(
                                selected = state.interval == interval,
                                onClick = { viewModel.setInterval(interval) },
                                label = { Text(interval.label) }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quality", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityMode.entries.forEach { quality ->
                            FilterChip(
                                selected = state.quality == quality,
                                onClick = { viewModel.setQuality(quality) },
                                label = { Text("${quality.label} - ${quality.description}") }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Viewer Link", style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Outlined.Link, contentDescription = null)
                    }
                    Text(state.viewerUrl)
                    Text(
                        "Stable reusable link for the one permanent V1 stream.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.viewerUrl))
                            }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Text("Copy")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, state.viewerUrl)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share viewer link"))
                            }
                        ) {
                            Text("Share")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.viewerUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Open")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (state.status == StreamStatus.Running) {
                            serviceController.pause()
                        } else {
                            val hasCameraPermission =
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                            if (!hasCameraPermission) {
                                pendingStart = true
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                cameraGranted = true
                                viewModel.markStarting()
                                serviceController.start(
                                    FrameStreamConfig(
                                        interval = state.interval,
                                        quality = state.quality
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (state.status == StreamStatus.Running) "Pause"
                        else if (state.status == StreamStatus.Paused) "Resume"
                        else "Start"
                    )
                }
                OutlinedButton(
                    onClick = {
                        serviceController.stop()
                        viewModel.markStopped()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Latest Captured Frame", style = MaterialTheme.typography.titleMedium)
                    if (latestBitmap != null) {
                        Image(
                            bitmap = latestBitmap,
                            contentDescription = "Latest captured frame",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            state.latestFramePath ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No frame saved yet. Start the stream to verify local capture.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            "Foreground service capture is wired to CameraX and uploads each frame to the configured backend."
                        } else {
                            "Foreground capture runtime is wired to the backend for testing."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Current build saves one latest local frame for verification while uploading the real JPEG to the web API.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
