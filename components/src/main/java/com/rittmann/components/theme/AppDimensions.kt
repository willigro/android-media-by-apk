package com.rittmann.components.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    // Padding
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 16.dp,
    val paddingLarge: Dp = 22.dp,

    val paddingScreen: Dp = 18.dp,

    val paddingTopBetweenComponentsSmall: Dp = 6.dp,
    val paddingTopBetweenComponentsMedium: Dp = 12.dp,
    val paddingTopBetweenComponentsLarge: Dp = 22.dp,

    // Size
    val progressSize: Dp = 100.dp,
    val divisor: Dp = 1.dp,

    val dialogDimens: DialogDimens = DialogDimens(),
    val mediaScreenDimens: MediaScreenDimens = MediaScreenDimens(),
)

class MediaScreenDimens(
    val thumbnailPadding: Dp = 5.dp,
    val thumbnailNamePadding: Dp = 5.dp,
)

class DialogDimens(
    val titleMinHeight: Dp = 60.dp,
    val messageMinHeight: Dp = 100.dp,
)

internal val LocalDimensions = staticCompositionLocalOf { AppDimensions() }