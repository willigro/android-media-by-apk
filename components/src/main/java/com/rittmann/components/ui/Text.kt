package com.rittmann.components.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun PreviewText() {
    Column {
        MediaTextBody(text = "Testing Body")
    }
}

@Composable
fun MediaTextH1(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun MediaTextH2(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

@Composable
fun MediaTextBody(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
        maxLines = maxLines,
        textAlign = textAlign,
    )
}