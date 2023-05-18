package com.rittmann.mediacontrol.medias

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rittmann.components.ui.TextH1
import com.rittmann.core.android.AndroidVersion



@Composable
fun MediasScreenRoot(
    navController: NavController,
    viewModel: MediasViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsState().value

    when (uiState.androidHandler.version()) {
        AndroidVersion.ANDROID_9 -> TextH1(text = "Android 9")
        AndroidVersion.ANDROID_10 -> TextH1(text = "Android 10")
        AndroidVersion.ANDROID_11 -> TextH1(text = "Android 11")
        AndroidVersion.ANDROID_12 -> TextH1(text = "Android 12")
    }

    uiState.androidHandler.retrieveMedia()
}