package com.rittmann.components.ui

import android.os.CountDownTimer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.rittmann.components.R
import com.rittmann.components.theme.AppShapes
import com.rittmann.components.theme.AppTheme
import kotlinx.coroutines.launch

@Preview
@Composable
fun SearchTextFieldPreview() {
    SearchTextField(
        modifier = Modifier,
        text = "",
        hint = "",
        onTextChanged = {},
        fetch = {},
    )
}

// TODO: create a state to wrap these args
@Composable
fun SearchTextField(
    modifier: Modifier,
    text: String,
    hint: String,
    onTextChanged: (String) -> Unit,
    fetch: (String) -> Unit,
) {
    SearchOutlinedTextField(
        modifier = modifier,
        fetch = fetch,
        text = text,
        hint = hint,
        onTextChanged = onTextChanged,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchOutlinedTextField(
    modifier: Modifier,
    text: String,
    hint: String,
    onTextChanged: (String) -> Unit,
    fetch: (String) -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    val searchDelay = SearchDelay(fetch)

    OutlinedTextField(
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
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = AppTheme.colors.textSecondary,
            focusedBorderColor = AppTheme.colors.primary,
            unfocusedBorderColor = AppTheme.colors.primary,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            onTextChanged(it)
            searchDelay.restart(it)
        },
        placeholder = {
            TextBody(
                text = hint,
                color = AppTheme.colors.textInfo,
            )
        },
        trailingIcon = {
            if (text.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        id = R.string.content_description_clear_search
                    ),
                    tint = AppTheme.colors.primaryIcon,
                    modifier = Modifier.clickable {
                        onTextChanged("")
                        searchDelay.restart("")
                    }
                )
            }
        },
        shape = AppShapes.large,
    )
}

class SearchDelay(
    private val callback: (String) -> Unit,
) {
    var value = ""

    private val timer = object : CountDownTimer(300L, 300L) {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            callback(value)
        }
    }

    fun restart(value: String) {
        cancel()
        start(value)
    }

    private fun start(value: String) {
        this.value = value
        timer.start()
    }

    private fun cancel() {
        this.value = ""
        timer.cancel()
    }
}