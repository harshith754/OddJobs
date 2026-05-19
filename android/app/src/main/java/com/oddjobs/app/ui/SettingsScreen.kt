package com.oddjobs.app.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oddjobs.app.settings.AppSettingsStore
import com.oddjobs.app.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                    Text("App", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "These are OddJobs-wide settings. Job-specific controls stay inside each job screen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.backendUrl,
                        onValueChange = viewModel::updateBackendUrl,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Backend URL") },
                        placeholder = { Text("http://192.168.1.23:3001") }
                    )
                    Text(
                        "Use your laptop's LAN IP and the Next.js port while testing. Do not use localhost from the phone.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Current default: ${AppSettingsStore.DEFAULT_BACKEND_URL}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Debug Logs", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Enable extra diagnostics while capture/upload reliability is being tuned.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.debugLogsEnabled,
                        onCheckedChange = { viewModel.toggleDebugLogs() }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "OddJobs is the container app. Frame Stream is the first odd job in the app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Open a job from the home screen to change that job's behavior.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
