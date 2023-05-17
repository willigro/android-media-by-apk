package com.rittmann.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rittmann.components.theme.AppTheme

@Composable
fun ProgressScreen(modifier: Modifier, isLoadingState: MutableState<Boolean>) {
    if (isLoadingState.value) {
        ProgressScreen(modifier)
    }
}

@Composable
fun ProgressScreen(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = AppTheme.floats.progressAlpha)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(AppTheme.dimensions.progressSize),
            color = AppTheme.colors.primary,
        )
    }
}