package com.rittmann.mediacontrol.medias

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rittmann.components.theme.AppTheme
import com.rittmann.components.ui.TextH1
import com.rittmann.core.android.AndroidVersion
import com.rittmann.core.data.Image


@Composable
fun MediasScreenRoot(
    navController: NavController,
    viewModel: MediasViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val uiState = viewModel.uiState.collectAsState().value

        ToolbarTitle(uiState = uiState)

        MediasList(uiState = uiState, loadBitmapFor = viewModel::loadBitmapFor)
    }
}

@Composable
fun ToolbarTitle(uiState: MediasUiState) {
    when (uiState.androidVersion) {
        AndroidVersion.ANDROID_9 -> TextH1(text = "Android 9")
        AndroidVersion.ANDROID_10 -> TextH1(text = "Android 10")
        AndroidVersion.ANDROID_11 -> TextH1(text = "Android 11")
        AndroidVersion.ANDROID_12 -> TextH1(text = "Android 12")
    }
}

@Composable
fun MediasList(uiState: MediasUiState, loadBitmapFor: (media: Image) -> Bitmap) {
    val list = uiState.mediaList.collectAsState().value

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(list) { media ->
            val bitmap = loadBitmapFor(media)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .height(AppTheme.dimensions.mediaDimens.thumbnailHeight),
            )
        }
    }
}
