package com.rittmann.mediacontrol.create

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.AndroidVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class CreateMediaViewModel @Inject constructor(
    private val androidHandler: AndroidHandler,
) : ViewModel() {

    private val _uiState: MutableStateFlow<CameraUiState> = MutableStateFlow(
        CameraUiState.InitialState(androidHandler.version())
    )
    val uiState: StateFlow<CameraUiState>
        get() = _uiState

    init {
        androidHandler.requestCameraPermissions()

        viewModelScope.launch {
            androidHandler.cameraIsAvailable().collectLatest {
                _uiState.value.cameraIsAvailable.value = it
            }
        }
    }

    fun takePhoto(
        imageCapture: ImageCapture,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        androidHandler.takePhoto(
            imageCapture,
            onImageCaptured,
            onError,
        )
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

sealed class CameraUiState(
    val androidVersion: AndroidVersion,
    val cameraIsAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false),
) {
    class InitialState(androidVersion: AndroidVersion) : CameraUiState(androidVersion)
}