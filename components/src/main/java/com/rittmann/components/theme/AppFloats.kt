package com.rittmann.components.theme

import androidx.compose.runtime.staticCompositionLocalOf

data class AppFloats(
    val sameWeight: Float = 1f,
    val backgroundSolidAlpha: Float = 1f,
    val progressAlpha: Float = .4f,
)

internal val LocalFloats = staticCompositionLocalOf { AppFloats() }