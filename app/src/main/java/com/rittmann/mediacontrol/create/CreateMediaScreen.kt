package com.rittmann.mediacontrol.create

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rittmann.components.theme.AppTheme
import com.rittmann.components.ui.MediaTextBody
import com.rittmann.components.ui.SimpleTextField
import com.rittmann.core.android.Storage
import com.rittmann.core.android.StorageUri
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.extensions.applyRandomFilter
import com.rittmann.core.extensions.getCameraProvider
import com.rittmann.core.extensions.toBitmapExif
import com.rittmann.core.tracker.track
import kotlinx.coroutines.flow.StateFlow


data class CreateMediaScreenArguments(
    val storageUri: StorageUri? = null,
    val mediaId: Long? = null,
)

@Composable
fun CreateMediaScreenRoot(
    navController: NavController,
    createMediaScreenArguments: CreateMediaScreenArguments? = null,
    createMediaViewModel: CreateMediaViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        createMediaViewModel.loadUri(createMediaScreenArguments)
    }

    val uiState = createMediaViewModel.uiState.collectAsState().value

    track("Stating=$uiState")

    when (uiState) {
        is CameraUiState.TakePicture -> CameraView(viewModel = createMediaViewModel)
        is CameraUiState.ShowNewPicture -> TakenImage(
            uiState = uiState,
            takeAgain = createMediaViewModel::takeAgain,
            saveImage = createMediaViewModel::saveImage,
            name = createMediaViewModel.name,
            setName = createMediaViewModel::setName,
        )

        is CameraUiState.ShowOldPicture -> OldImage(
            uiState = uiState,
            loadBitmapExif = createMediaViewModel::loadBitmapExif,
            updateImage = createMediaViewModel::updateImage,
            deleteImage = createMediaViewModel::deleteImage,
            name = createMediaViewModel.name,
            setName = createMediaViewModel::setName,
        )

        is CameraUiState.Saved, CameraUiState.Deleted -> {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }

        else -> {
            MediaTextBody(
                text = "Loading info",
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun CameraView(
    viewModel: CreateMediaViewModel,
) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        val clickOffset = remember {
            mutableStateOf<Circle?>(null)
        }

        val isInside = remember {
            mutableStateOf(false)
        }

        Canvas(
            modifier = Modifier
                .size(AppTheme.dimensions.createMediaScreenDimens.takePictureButtonSize)
                .padding(
                    bottom = AppTheme.dimensions.createMediaScreenDimens.takePictureButtonBottomPadding
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            clickOffset.value?.also { circle ->
                                if (circle.intersect(offset.x, offset.y)) {
                                    isInside.value = true
                                }
                            }
                        },
                        onTap = {
                            if (isInside.value) {
                                viewModel.takePhoto(imageCapture = imageCapture)
                            }

                            isInside.value = false
                        }
                    )
                }
        ) {
            drawCircle(
                color = Color.White,
                center = center,
                style = Stroke(6f)
            )

            val radius = (size.minDimension / 2f) / 2f

            drawCircle(
                color = if (isInside.value) Color.Red else Color.White,
                radius = radius,
                center = center,
            )

            clickOffset.value = Circle(
                x = center.x,
                y = center.y,
                radius = radius,
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun TakenImage(
    uiState: CameraUiState.ShowNewPicture,
    takeAgain: () -> Unit,
    saveImage: (BitmapExif, Storage) -> Unit,
    name: StateFlow<String>,
    setName: (String) -> Unit,
) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val showSaveButton = remember {
            mutableStateOf(false)
        }

        var bitmapExif by remember {
            mutableStateOf(uiState.image.image?.toBitmapExif())
        }

        val (
            containerImage,
            infoContainer,
            takeAgainContainer,
            buttonSave,
        ) = createRefs()

        val middleGuideline = createGuidelineFromTop(0.5f)

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.constrainAs(containerImage) {
                top.linkTo(parent.top)
                bottom.linkTo(middleGuideline)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
            }
        ) {
            ImageContainer(
                bitmap = bitmapExif?.bitmap,
                takeAgain = takeAgain,
                showSaveButton = showSaveButton,
            )
        }

        Button(
            modifier = Modifier.constrainAs(takeAgainContainer) {
                top.linkTo(middleGuideline)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            onClick = takeAgain,
        ) {
            MediaTextBody(text = "Take Again")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(infoContainer) {
                    top.linkTo(takeAgainContainer.bottom)
                    bottom.linkTo(buttonSave.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.fillToConstraints
                }
        ) {
            Column(modifier = Modifier.wrapContentSize()) {
                Button(
                    onClick = {
                        bitmapExif = bitmapExif?.copy(
                            bitmap = bitmapExif?.bitmap?.applyRandomFilter()
                        )
                    }
                ) {
                    MediaTextBody(text = "Apply filter")
                }

                ImageName(modifier = Modifier, name = name, setName = setName)

                bitmapExif?.exifInterface?.also { exifInterface ->
                    MediaTextBody(
                        text = exifInterface.getAttribute(ExifInterface.TAG_DATETIME).toString()
                    )
                    MediaTextBody(
                        text = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).toString()
                    )
                    MediaTextBody(
                        text = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH).toString()
                    )
                }
            }
        }

        Box(
            modifier = Modifier.constrainAs(buttonSave) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            if (showSaveButton.value) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppTheme.dimensions.paddingTopBetweenComponentsMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.weight(AppTheme.floats.sameWeight),
                        onClick = {
                            bitmapExif?.let { saveImage(it, Storage.INTERNAL) }
                        }
                    ) {
                        MediaTextBody(text = "Save Internal")
                    }

                    Button(
                        modifier = Modifier.weight(AppTheme.floats.sameWeight),
                        onClick = {
                            bitmapExif?.let { saveImage(it, Storage.EXTERNAL) }
                        }
                    ) {
                        MediaTextBody(text = "Save External")
                    }
                }
            }
        }
    }
}

@Composable
fun OldImage(
    uiState: CameraUiState.ShowOldPicture,
    loadBitmapExif: (media: Image) -> BitmapExif?,
    updateImage: (BitmapExif) -> Unit,
    deleteImage: (Image) -> Unit,
    name: StateFlow<String>,
    setName: (String) -> Unit,
) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        var bitmapExif by remember {
            mutableStateOf(loadBitmapExif(uiState.image))
        }

        val (
            containerImage,
            infoContainer,
            buttonSave,
        ) = createRefs()

        val middleGuideline = createGuidelineFromTop(0.5f)

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.constrainAs(containerImage) {
                top.linkTo(parent.top)
                bottom.linkTo(middleGuideline)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
            }
        ) {
            if (bitmapExif?.bitmap != null) {
                Image(
                    bitmap = bitmapExif?.bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(infoContainer) {
                    top.linkTo(containerImage.bottom)
                    bottom.linkTo(buttonSave.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.fillToConstraints
                }
        ) {
            Column(modifier = Modifier.wrapContentSize()) {
                Button(
                    onClick = {
                        bitmapExif = bitmapExif?.copy(
                            bitmap = bitmapExif?.bitmap?.applyRandomFilter()
                        )
                    }
                ) {
                    MediaTextBody(text = "Apply filter")
                }

                ImageName(modifier = Modifier, name = name, setName = setName)

                bitmapExif?.exifInterface?.also { exifInterface ->
                    MediaTextBody(
                        text = exifInterface.getAttribute(ExifInterface.TAG_DATETIME).toString()
                    )
                    MediaTextBody(
                        text = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).toString()
                    )
                    MediaTextBody(
                        text = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH).toString()
                    )
                }
            }
        }

        Box(
            modifier = Modifier.constrainAs(buttonSave) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppTheme.dimensions.paddingTopBetweenComponentsMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(AppTheme.floats.sameWeight),
                    onClick = {
                        bitmapExif?.let { updateImage(it) }
                    }
                ) {
                    MediaTextBody(text = "Save")
                }

                Button(
                    modifier = Modifier.weight(AppTheme.floats.sameWeight),
                    onClick = {
                        deleteImage(uiState.image)
                    }
                ) {
                    MediaTextBody(text = "Delete")
                }
            }
        }
    }
}

@Composable
fun ImageContainer(
    bitmap: Bitmap?,
    takeAgain: () -> Unit,
    showSaveButton: MutableState<Boolean>,
) {
    if (bitmap == null) {
        takeAgain()
    } else {
        showSaveButton.value = true

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun ImageName(modifier: Modifier, name: StateFlow<String>, setName: (String) -> Unit) {
    val text = name.collectAsState().value
    SimpleTextField(
        modifier = modifier,
        text = text,
        hint = "Name",
        onTextChanged = setName,
    )
}
