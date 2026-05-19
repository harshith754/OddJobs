package com.oddjobs.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OddJobsDarkColorScheme = darkColorScheme()

@Composable
fun OddJobsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OddJobsDarkColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
