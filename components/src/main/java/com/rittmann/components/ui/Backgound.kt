package com.rittmann.components.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rittmann.components.theme.AppTheme

@Preview
@Composable
fun BackgroundPreview() {
    Background {

    }
}

@Composable
fun Background(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            color = AppTheme.colors.background.copy(
                alpha = AppTheme.floats.backgroundSolidAlpha,
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}