package com.rittmann.mediacontrol.medias

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rittmann.components.theme.AppTheme
import com.rittmann.components.ui.MediaTextBody
import com.rittmann.components.ui.MediaTextH1
import com.rittmann.components.ui.MediaTextH2
import com.rittmann.core.android.AndroidVersion
import com.rittmann.core.data.Image
import com.rittmann.mediacontrol.navigation.Navigation


@Composable
fun MediasScreenRoot(
    navController: NavController,
    viewModel: MediasViewModel = hiltViewModel(),
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (
            toolbarContainer,
            optionsContainer,
            mediasContainer,
            buttonCreate,
        ) = createRefs()

        val uiState = viewModel.uiState.collectAsState().value

        ToolbarTitle(
            modifier = Modifier.constrainAs(toolbarContainer) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            uiState = uiState,
        )

        Row(modifier = Modifier
            .constrainAs(optionsContainer) {
                top.linkTo(toolbarContainer.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
            .fillMaxWidth()
        ) {
            MediaTextH2(
                text = "Internal",
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        viewModel.loadInternalMedia()
                    },
                textAlign = TextAlign.Center,
            )
            MediaTextH2(
                text = "External",
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        viewModel.loadExternalMedia()
                    },
                textAlign = TextAlign.Center,
            )
        }

        MediasList(
            modifier = Modifier.constrainAs(mediasContainer) {
                top.linkTo(optionsContainer.bottom)
                bottom.linkTo(buttonCreate.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
            },
            navController = navController,
            uiState = uiState,
            loadBitmapFor = viewModel::loadBitmapFor,
        )

        Button(
            modifier = Modifier.constrainAs(buttonCreate) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            onClick = {
                navController.navigate(Navigation.Create.destination)
            },
        ) {
            MediaTextBody(text = "Create")
        }
    }
}

@Composable
fun ToolbarTitle(
    modifier: Modifier,
    uiState: MediasUiState,
) {
    val text = when (uiState.androidVersion) {
        AndroidVersion.ANDROID_9 -> "Android 9"
        AndroidVersion.ANDROID_10 -> "Android 10"
        AndroidVersion.ANDROID_11 -> "Android 11"
        AndroidVersion.ANDROID_12 -> "Android 12"
    }

    MediaTextH1(text = text, modifier = modifier)
}

@Composable
fun MediasList(
    modifier: Modifier,
    navController: NavController,
    uiState: MediasUiState,
    loadBitmapFor: (media: Image) -> Bitmap,
) {
    val list = uiState.mediaList.collectAsState().value

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
    ) {
        items(list) { media ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(
                            Navigation.Update.transformDestination(
                                media.uri.toString(),
                                media.storage,
                                media.id,
                            )
                        )
                    },
            ) {
                val bitmap = loadBitmapFor(media)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .height(AppTheme.dimensions.mediaDimens.thumbnailHeight),
                )

                MediaTextBody(
                    text = media.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.dimensions.paddingTopBetweenComponentsSmall),
                )
            }
        }
    }
}
