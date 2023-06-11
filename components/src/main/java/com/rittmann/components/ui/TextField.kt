package com.rittmann.components.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.KeyboardType
import com.rittmann.components.theme.AppShapes
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SimpleTextField(
    modifier: Modifier = Modifier,
    text: String,
    hint: String,
    onTextChanged: (String) -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    TextField(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        value = text,
//        colors = TextFieldDefaults.outlinedTextFieldColors(
//            textColor = AppTheme.colors.textPrimary,
//            focusedBorderColor = AppTheme.colors.primary,
//            unfocusedBorderColor = AppTheme.colors.primary,
//        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            onTextChanged(it)
        },
        placeholder = {
            MediaTextBody(
                text = hint,
            )
        },
        shape = AppShapes.large,
    )
}