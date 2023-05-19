package com.rittmann.mediacontrol.create

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.tracker.track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class CreateMediaViewModel @Inject constructor(
    private val androidHandler: AndroidHandler,
) : ViewModel() {

    private val _uiState: MutableStateFlow<CameraUiState> = MutableStateFlow(
        CameraUiState.Idle
    )
    val uiState: StateFlow<CameraUiState>
        get() = _uiState

    init {
        androidHandler.requestCameraPermissions()

        viewModelScope.launch {
            androidHandler.cameraIsAvailable().collectLatest {
                _uiState.value = CameraUiState.TakePicture
            }
        }

        viewModelScope.launch {
            androidHandler.pictureSaved().collectLatest { image ->
                track(image)
                image?.also {
                    // TODO, fow a while I'm going to reset, but later it will need to change
                    _uiState.value = CameraUiState.Idle
                }
            }
        }

        viewModelScope.launch {
            androidHandler.pictureTaken().collectLatest { image ->
                track(image)
                image?.also { _uiState.value = CameraUiState.ShowPicture(image) }
            }
        }
    }

    fun takePhoto(
        imageCapture: ImageCapture,
    ) {
        track()
        androidHandler.takePhoto(imageCapture)
    }

    fun takeAgain() {
        track()
        _uiState.value = CameraUiState.TakePicture
    }

    fun saveImage(bitmap: Bitmap) {
        track()
        androidHandler.savePicture(bitmap)
    }
}

sealed class CameraUiState {
    object Idle : CameraUiState()
    object TakePicture : CameraUiState()
    class ShowPicture(val image: ImageProxy) : CameraUiState()
}