package com.rittmann.components.ui.alert

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rittmann.components.R
import com.rittmann.components.theme.AppTheme
import com.rittmann.components.ui.MediaTextBody
import com.rittmann.components.ui.MediaTextH2


open class AlertCompose(var title: String, var message: String) {

    companion object {
        var defaultTitle: String? = null
    }

    var showDialog = mutableStateOf(false)

    var confirmCallback: (() -> Unit)? = null

    fun dismiss() {
        showDialog.value = false
    }

    open fun updateAndShow(
        title: String? = null,
        message: String? = null,
        confirmCallback: (() -> Unit)? = null
    ) {
        title?.also {
            this.title = title
        }
        message?.also {
            this.message = message
        }
        this.confirmCallback = confirmCallback
        showDialog.value = true
    }
}

class AlertComposeOk(
    title: String = defaultTitle ?: "",
    message: String = defaultTitle ?: ""
) : AlertCompose(title, message) {

    @Composable
    fun PresentDialog() {
        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = ::dismiss,
                title = {
                    MediaTextBody(text = title)
                },
                text = {
                    MediaTextBody(text = message)
                },
                confirmButton = {
                    Button(onClick = {
                        dismiss()
                        confirmCallback?.invoke()
                    }) {
                        Text(text = stringResource(id = R.string.dialog_ok))
                    }
                }
            )
        }

    }
}

class AlertComposeConfirm(
    title: String = defaultTitle ?: "",
    message: String = defaultTitle ?: "",
) : AlertCompose(title, message) {

    @Composable
    fun PresentDialog() {
        if (showDialog.value)
            NoPaddingAlertDialog(
                onDismissRequest = ::dismiss,
                title = {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppTheme.dimensions.dialogDimens.titleMinHeight)
                            .padding(
                                start = AppTheme.dimensions.paddingMedium,
                                end = AppTheme.dimensions.paddingMedium,
                            )
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MediaTextH2(
                            text = title,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = AppTheme.dimensions.dialogDimens.messageMinHeight)
                            .padding(
                                start = AppTheme.dimensions.paddingMedium,
                                end = AppTheme.dimensions.paddingMedium,
                            )
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MediaTextBody(
                            text = message,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                confirmButton = {
                    Text(
                        text = stringResource(id = R.string.dialog_confirm),
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
//                        color = AppTheme.colors.primary,
                        modifier = Modifier
                            .clickable {
                                dismiss()
                                confirmCallback?.invoke()
                            }
                            .padding(
                                horizontal = AppTheme.dimensions.paddingLarge,
                                vertical = AppTheme.dimensions.paddingMedium,
                            )
                    )
                },
                dismissButton = {
                    Text(
                        text = stringResource(id = R.string.dialog_cancel),
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = Color.Gray,
                        modifier = Modifier
                            .clickable {
                                dismiss()
                            }
                            .padding(
                                horizontal = AppTheme.dimensions.paddingLarge,
                                vertical = AppTheme.dimensions.paddingMedium,
                            )
                    )
                }
            )
    }
}

@Composable
fun NoPaddingAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            contentColor = contentColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                title?.let {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                        val textStyle = MaterialTheme.typography.titleSmall
                        ProvideTextStyle(textStyle, it)
                    }
                }
                text?.let {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                        val textStyle = MaterialTheme.typography.titleSmall
                        ProvideTextStyle(textStyle, it)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                    //.padding(all = 8.dp)
                ) {
                    Row(
//                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .weight(1f)
                        ) {
                            dismissButton?.invoke()
                        }
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .weight(1f)
                        ) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}