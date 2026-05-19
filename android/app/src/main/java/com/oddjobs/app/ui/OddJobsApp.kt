package com.oddjobs.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oddjobs.app.framestream.FrameStreamViewModel
import com.oddjobs.app.settings.SettingsViewModel

@Composable
fun OddJobsApp() {
    val navController = rememberNavController()
    val frameStreamViewModel: FrameStreamViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        openFrameStream = { navController.navigate("frame-stream") },
                        openSettings = { navController.navigate("settings") }
                    )
                }
                composable("frame-stream") {
                    FrameStreamScreen(
                        viewModel = frameStreamViewModel,
                        navigateBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        navigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
