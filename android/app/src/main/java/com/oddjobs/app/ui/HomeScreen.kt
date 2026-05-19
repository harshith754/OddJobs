package com.oddjobs.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

private data class OddJobCardModel(
    val title: String,
    val description: String,
    val status: String,
    val icon: ImageVector,
    val enabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    openFrameStream: () -> Unit,
    openSettings: () -> Unit
) {
    val jobs = listOf(
        OddJobCardModel(
            title = "Frame Stream",
            description = "Capture high-quality frames and publish them to a private viewer.",
            status = "Available",
            icon = Icons.Outlined.CameraAlt,
            enabled = true
        ),
        OddJobCardModel(
            title = "Quick Capture",
            description = "One-shot capture workflow.",
            status = "Coming soon",
            icon = Icons.Outlined.Build,
            enabled = false
        ),
        OddJobCardModel(
            title = "File Drop",
            description = "Push files to a private destination.",
            status = "Coming soon",
            icon = Icons.Outlined.UploadFile,
            enabled = false
        ),
        OddJobCardModel(
            title = "Clipboard Sync",
            description = "Move snippets across devices.",
            status = "Coming soon",
            icon = Icons.Outlined.ContentPaste,
            enabled = false
        ),
        OddJobCardModel(
            title = "Reminder Ping",
            description = "Send a small personal reminder.",
            status = "Coming soon",
            icon = Icons.Outlined.Notifications,
            enabled = false
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OddJobs")
                        Text(
                            text = "Tools for oddly specific tasks.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = openSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(jobs) { job ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = job.enabled) {
                            if (job.title == "Frame Stream") {
                                openFrameStream()
                            }
                        },
                    colors = CardDefaults.cardColors()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = job.icon,
                            contentDescription = null
                        )
                        Text(job.title, style = MaterialTheme.typography.titleMedium)
                        Text(job.description, style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${job.status}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
