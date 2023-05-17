package com.rittmann.components.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.rittmann.components.theme.AppTheme

@Preview
@Composable
fun PreviewText() {
    Column {
        TextBody(text = "Testing Body")
    }
}

@Composable
fun TextH1(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTheme.typography.h1Bold,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun TextH2(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTheme.typography.h2Bold,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun TextBody(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTheme.typography.body.copy(fontWeight = fontWeight),
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun TextBodyMedium(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    TextBody(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight.Medium,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun TextBodyBold(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    TextBody(
        modifier = modifier,
        text = text,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun TextCaption(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTheme.typography.caption,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun TextCaptionBold(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = AppTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTheme.typography.captionBold,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}