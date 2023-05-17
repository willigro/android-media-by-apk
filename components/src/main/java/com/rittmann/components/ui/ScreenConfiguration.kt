package com.rittmann.components.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun ScreenConfiguration(
    portrait: @Composable () -> Unit,
    landscape: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            landscape()
        }
        else -> {
            portrait()
        }
    }
}