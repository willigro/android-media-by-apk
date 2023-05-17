package com.rittmann.components.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rittmann.components.R

val sfProNormal = FontFamily(
    Font(R.font.sf_pro_medium, FontWeight.Normal)
)

data class AppTypography(
    val h1Bold: TextStyle = TextStyle(
        fontFamily = sfProNormal,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp
    ),
    val h2Bold: TextStyle = TextStyle(
        fontFamily = sfProNormal,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = sfProNormal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    val info: TextStyle = TextStyle(
        fontFamily = sfProNormal,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = sfProNormal,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    val captionBold: TextStyle = caption.copy(fontWeight = FontWeight.Bold),
)

internal val LocalTypography = staticCompositionLocalOf { AppTypography() }