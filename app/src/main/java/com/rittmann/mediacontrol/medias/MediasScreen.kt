package com.rittmann.mediacontrol.medias

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rittmann.components.theme.AppTheme
import com.rittmann.components.ui.MediaTextBody
import com.rittmann.components.ui.MediaTextBodySmall
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
        modifier = modifier.padding(
            AppTheme.dimensions.paddingSmall,
        ),
        columns = GridCells.Fixed(3),
    ) {
        items(list) { media ->
            ConstraintLayout(
                modifier = Modifier
                    .padding(
                        AppTheme.dimensions.mediaScreenDimens.thumbnailPadding,
                    )
                    .wrapContentSize()
                    .clickable {
                        navController.navigate(
                            Navigation.Update.transformDestination(
                                media.uri.toString(),
                                media.storage,
                                media.id,
                            )
                        )
                    }
            ) {
                val (image, name) = createRefs()

                val bitmap = loadBitmapFor(media)

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .constrainAs(image) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                )

                MediaTextBodySmall(
                    text = media.name,
                    modifier = Modifier
                        .constrainAs(name) {
                            bottom.linkTo(image.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .padding(AppTheme.dimensions.mediaScreenDimens.thumbnailNamePadding),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
