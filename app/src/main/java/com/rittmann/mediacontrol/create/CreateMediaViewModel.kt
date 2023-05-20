package com.rittmann.mediacontrol.create

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rittmann.core.android.AndroidHandler
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
        CameraUiState.Start
    )
    val uiState: StateFlow<CameraUiState>
        get() = _uiState

    init {
        androidHandler.requestCameraPermissions()

        viewModelScope.launch {
            androidHandler.cameraIsAvailable.collectLatest {
                _uiState.value = CameraUiState.TakePicture
            }
        }

        viewModelScope.launch {
            androidHandler.imageSaved.collectLatest { image ->
                image?.also {
                    _uiState.value = CameraUiState.Saved
                }
            }
        }

        viewModelScope.launch {
            androidHandler.imageProxyTaken.collectLatest { image ->
                image?.also {
                    _uiState.value = CameraUiState.ShowPicture(image)
                }
            }
        }
    }

    fun takePhoto(
        imageCapture: ImageCapture,
    ) {
        androidHandler.takePhoto(imageCapture)
    }

    fun takeAgain() {
        _uiState.value = CameraUiState.TakePicture
    }

    fun saveImage(bitmap: Bitmap) {
        androidHandler.savePicture(bitmap)
    }
}

sealed class CameraUiState {
    object Start : CameraUiState()
    object Saved : CameraUiState()
    object TakePicture : CameraUiState()
    class ShowPicture(val image: ImageProxy) : CameraUiState()
}