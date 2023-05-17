package com.rittmann.components.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


class AppColors(
    isLightColors: Boolean,
    primary: Color,
    secondary: Color,
    textPrimary: Color,
    textSecondary: Color,
    textInfo: Color,
    background: Color,
    backgroundFilter: Color,
    searchFieldBackground: Color,
    searchFieldMinimizeBackground: Color,
    primaryIcon: Color,
    secondaryIcon: Color,
) {
    var primary by mutableStateOf(primary)
        private set
    var secondary by mutableStateOf(secondary)
        private set
    var textPrimary by mutableStateOf(textPrimary)
        private set
    var textSecondary by mutableStateOf(textSecondary)
        private set
    var textInfo by mutableStateOf(textInfo)
        private set
    var isLight by mutableStateOf(isLightColors)
        internal set

    /**
     * Backgrounds
     * */
    var background by mutableStateOf(background)
        private set
    var backgroundFilter by mutableStateOf(backgroundFilter)
        private set

    /**
     * Icons
     * */
    var primaryIcon by mutableStateOf(primaryIcon)
        private set
    var secondaryIcon by mutableStateOf(secondaryIcon)
        private set

    /**
     * Specific
     * */
    var searchFieldBackground by mutableStateOf(searchFieldBackground)
        private set
    var searchFieldMinimizeBackground by mutableStateOf(searchFieldMinimizeBackground)
        private set

    fun copy(
        isLight: Boolean = this.isLight,
        primary: Color = this.primary,
        secondary: Color = this.secondary,
        textPrimary: Color = this.textPrimary,
        textSecondary: Color = this.textSecondary,
        textInfo: Color = this.textInfo,
        background: Color = this.background,
        backgroundFilter: Color = this.backgroundFilter,
        primaryIcon: Color = this.primaryIcon,
        secondaryIcon: Color = this.secondaryIcon,
        searchFieldBackground: Color = this.searchFieldBackground,
        searchFieldMinimizeBackground: Color = this.searchFieldMinimizeBackground,
    ): AppColors = AppColors(
        isLight,
        primary,
        secondary,
        textPrimary,
        textSecondary,
        textInfo,
        background,
        backgroundFilter,
        searchFieldBackground,
        searchFieldMinimizeBackground,
        primaryIcon,
        secondaryIcon,
    )

    fun updateColorsFrom(other: AppColors) {
        primary = other.primary
        textPrimary = other.textPrimary
        textSecondary = other.textSecondary
    }
}

fun appLightColors(
    primary: Color = colorLightPrimary,
    secondary: Color = colorLightSecondary,
    textPrimary: Color = colorLightTextPrimary,
    textSecondary: Color = colorLightTextSecondary,
    textInfo: Color = colorLightTextInfo,
    background: Color = colorLightBackground,
    backgroundFilter: Color = colorLightBackgroundInfo,
    primaryIcon: Color = colorLightPrimaryIcon,
    secondaryIcon: Color = colorLightSecondaryIcon,
    searchFieldBackground: Color = colorLightSearchFieldBackground,
    searchFieldMinimizeBackground: Color = colorLightSearchFieldMinimizeBackground,
): AppColors = AppColors(
    isLightColors = true,
    primary = primary,
    secondary = secondary,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textInfo = textInfo,
    background = background,
    backgroundFilter = backgroundFilter,
    primaryIcon = primaryIcon,
    secondaryIcon = secondaryIcon,
    searchFieldBackground = searchFieldBackground,
    searchFieldMinimizeBackground = searchFieldMinimizeBackground,
)

fun appDarkColors(
    primary: Color = colorDarkPrimary,
    secondary: Color = colorDarkSecondary,
    textPrimary: Color = colorDarkTextPrimary,
    textSecondary: Color = colorDarkTextSecondary,
    textInfo: Color = colorDarkTextInfo,
    background: Color = colorDarkBackground,
    backgroundFilter: Color = colorDarkBackgroundInfo,
    primaryIcon: Color = colorDarkPrimaryIcon,
    secondaryIcon: Color = colorDarkSecondaryIcon,
    searchFieldBackground: Color = colorDarkSearchFieldBackground,
    searchFieldMinimizeBackground: Color = colorDarkSearchFieldMinimizeBackground,
): AppColors = AppColors(
    isLightColors = false,
    primary = primary,
    secondary = secondary,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textInfo = textInfo,
    background = background,
    backgroundFilter = backgroundFilter,
    primaryIcon = primaryIcon,
    secondaryIcon = secondaryIcon,
    searchFieldBackground = searchFieldBackground,
    searchFieldMinimizeBackground = searchFieldMinimizeBackground,
)

internal val LocalColors = staticCompositionLocalOf { appLightColors() }


/**
 * LIGHT
 * */
internal val colorLightPrimary = Color(0xFF444E72)
internal val colorLightSecondary = Color(0xFF444E72)
internal val colorLightTertiary = Color(0xFF444E72)
internal val colorLightTextPrimary = Color( 0xFFFFFFFF)
internal val colorLightTextSecondary = Color(0xFF444E72)
internal val colorLightTextInfo = Color(0xFF838BAA)
internal val colorLightBackground = Color(0xFF002762)
internal val colorLightBackgroundInfo = Color(0xFFFCFCFC)
internal val colorLightPrimaryIcon = Color(0xFF444E72)
internal val colorLightSecondaryIcon = Color(0xFFFFFFFF)

/**
 * Specific
 * */
internal val colorLightSearchFieldBackground = Color(0xFFFFFFFF)
internal val colorLightSearchFieldMinimizeBackground = Color(0xFFF1F4FF)


/**
 * DARK
 * */
internal val colorDarkPrimary = Color(0xFFE01818)
internal val colorDarkSecondary = Color(0xFFE01818)
internal val colorDarkTertiary = Color(0xFFE01818)
internal val colorDarkTextPrimary = Color(0xFFFFFFFF)
internal val colorDarkTextSecondary = Color(0xFFE76161)
internal val colorDarkTextInfo = Color(0xFFAA9583)
internal val colorDarkBackground = Color(0xFF7E0000)
internal val colorDarkBackgroundInfo = Color(0xFFFCFCFC)
internal val colorDarkPrimaryIcon = Color(0xFFE76161)
internal val colorDarkSecondaryIcon = Color(0xFFFFFFFF)

/**
 * Specific
 * */
internal val colorDarkSearchFieldBackground = Color(0xFFFFFFFF)
internal val colorDarkSearchFieldMinimizeBackground = Color(0xFFF1F4FF)
