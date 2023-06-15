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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rittmann.components.icons.MediaIcons
import com.rittmann.components.theme.AppTheme
import com.rittmann.components.ui.MediaTextBody
import com.rittmann.components.ui.MediaTextBodySmall
import com.rittmann.components.ui.SimpleTextField
import com.rittmann.components.ui.linkToSides
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

        is CameraUiState.UpdatingPicture -> UpdateImage(
            uiState = uiState,
            loadBitmapExif = createMediaViewModel::loadBitmapExif,
            updateImage = createMediaViewModel::updateImage,
            deleteImage = createMediaViewModel::deleteImage,
            name = createMediaViewModel.name,
            setName = createMediaViewModel::setName,
            navController = navController,
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
        val bitmapExif = remember {
            mutableStateOf(uiState.image.image?.toBitmapExif())
        }

        val (
            containerImage,
            containerOptions,
        ) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(containerImage) {
                    top.linkTo(parent.top)
                    bottom.linkTo(containerOptions.top)
                    linkToSides()
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                }
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            ImageContainer(
                bitmap = bitmapExif.value?.bitmap,
                takeAgain = takeAgain,
            )
        }

        TakenImageOptions(
            modifier = Modifier.constrainAs(containerOptions) {
                bottom.linkTo(parent.bottom)
                linkToSides()
            },
            takeAgain = takeAgain,
            bitmapExifState = bitmapExif,
            saveImage = saveImage,
            name = name,
            setName = setName,
        )
    }
}

@Composable
fun TakenImageOptions(
    modifier: Modifier,
    takeAgain: () -> Unit,
    bitmapExifState: MutableState<BitmapExif?>,
    saveImage: (BitmapExif, Storage) -> Unit,
    name: StateFlow<String>,
    setName: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                modifier = Modifier.weight(AppTheme.floats.sameWeight),
                onClick = takeAgain,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(imageVector = MediaIcons.Refresh, contentDescription = null)

                    MediaTextBodySmall(text = "Redo")
                }
            }

            IconButton(
                modifier = Modifier.weight(AppTheme.floats.sameWeight),
                onClick = {
                    bitmapExifState.value = bitmapExifState.value?.copy(
                        bitmap = bitmapExifState.value?.bitmap?.applyRandomFilter()
                    )
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(imageVector = MediaIcons.Add, contentDescription = null)

                    MediaTextBodySmall(text = "Filter")
                }
            }

            bitmapExifState.value?.also { bitmapExif ->
                IconButton(
                    modifier = Modifier.weight(AppTheme.floats.sameWeight),
                    onClick = {
                        saveImage(bitmapExif, Storage.INTERNAL)
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(imageVector = MediaIcons.ArrowDownward, contentDescription = null)

                        MediaTextBodySmall(text = "Internal")
                    }
                }

                IconButton(
                    modifier = Modifier.weight(AppTheme.floats.sameWeight),
                    onClick = {
                        saveImage(bitmapExif, Storage.EXTERNAL)
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(imageVector = MediaIcons.ArrowUpward, contentDescription = null)

                        MediaTextBodySmall(text = "External")
                    }
                }
            }
        }

        Column(modifier = Modifier.wrapContentSize()) {
            ImageName(modifier = Modifier, name = name, setName = setName)

            bitmapExifState.value?.exifInterface?.also { exifInterface ->
                MediaTextBody(
                    text = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                        .toString()
                )
                MediaTextBody(
                    text = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
                        .toString()
                )
                MediaTextBody(
                    text = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                        .toString()
                )
            }
        }
    }
}

@Composable
fun UpdateImage(
    uiState: CameraUiState.UpdatingPicture,
    loadBitmapExif: (media: Image) -> BitmapExif?,
    updateImage: (BitmapExif) -> Unit,
    deleteImage: (Image) -> Unit,
    name: StateFlow<String>,
    setName: (String) -> Unit,
    navController: NavController,
) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val bitmapExif = remember {
            mutableStateOf(loadBitmapExif(uiState.image))
        }

        val (
            containerImage,
            optionsContainer,
        ) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(containerImage) {
                    top.linkTo(parent.top)
                    bottom.linkTo(optionsContainer.top)
                    linkToSides()
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                }
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (bitmapExif.value?.bitmap != null) {
                ImageContainer(
                    bitmap = bitmapExif.value?.bitmap,
                    takeAgain = {
                        navController.popBackStack()
                    },
                )
                Image(
                    bitmap = bitmapExif.value?.bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        UpdateImageOptions(
            modifier = Modifier.constrainAs(optionsContainer) {
                bottom.linkTo(parent.bottom)
                linkToSides()
            },
            uiState = uiState,
            bitmapExif = bitmapExif,
            updateImage = updateImage,
            deleteImage = deleteImage,
            name = name,
            setName = setName,
        )
    }
}

@Composable
fun UpdateImageOptions(
    modifier: Modifier,
    uiState: CameraUiState.UpdatingPicture,
    bitmapExif: MutableState<BitmapExif?>,
    updateImage: (BitmapExif) -> Unit,
    deleteImage: (Image) -> Unit,
    name: StateFlow<String>,
    setName: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            IconButton(
                modifier = Modifier.weight(AppTheme.floats.sameWeight),
                onClick = {
                    bitmapExif.value = bitmapExif.value?.copy(
                        bitmap = bitmapExif.value?.bitmap?.applyRandomFilter()
                    )
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(imageVector = MediaIcons.Add, contentDescription = null)

                    MediaTextBodySmall(text = "Filter")
                }
            }

            IconButton(
                modifier = Modifier.weight(AppTheme.floats.sameWeight),
                onClick = {
                    bitmapExif.value?.let { updateImage(it) }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(imageVector = MediaIcons.ArrowDownward, contentDescription = null)

                    MediaTextBodySmall(text = "Save")
                }
            }

            IconButton(
                modifier = Modifier.weight(AppTheme.floats.sameWeight),
                onClick = {
                    deleteImage(uiState.image)
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(imageVector = MediaIcons.Delete, contentDescription = null)

                    MediaTextBodySmall(text = "Delete")
                }
            }
        }

        Column(modifier = Modifier.wrapContentSize()) {
            ImageName(modifier = Modifier, name = name, setName = setName)

            bitmapExif.value?.exifInterface?.also { exifInterface ->
                MediaTextBody(
                    text = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                        .toString()
                )
                MediaTextBody(
                    text = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
                        .toString()
                )
                MediaTextBody(
                    text = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                        .toString()
                )
            }
        }
    }
}

@Composable
fun ImageContainer(
    bitmap: Bitmap?,
    takeAgain: () -> Unit,
) {
    if (bitmap == null) {
        takeAgain()
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
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
