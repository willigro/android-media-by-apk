package com.rittmann.mediacontrol.create

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.Storage
import com.rittmann.core.android.StorageUri
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.tracker.track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

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
            androidHandler.imageProxyTaken.collectLatest { imageProxy ->
                imageProxy?.also {
                    _uiState.value = CameraUiState.ShowNewPicture(imageProxy)
                }
            }
        }

        viewModelScope.launch {
            androidHandler.imageLoadedFromUri.collectLatest { image ->
                image?.also {
                    _name.value = image.name

                    _uiState.value = CameraUiState.ShowOldPicture(image)
                }
            }
        }
    }

    override fun onCleared() {
        track()
        super.onCleared()
        androidHandler.disposeCameraMembers()
    }

    fun setName(name: String) {
        _name.value = name
    }

    fun takePhoto(
        imageCapture: ImageCapture,
    ) {
        androidHandler.takePhoto(imageCapture)
    }

    fun takeAgain() {
        _uiState.value = CameraUiState.TakePicture
    }

    fun saveImage(bitmapExif: BitmapExif, storage: Storage) {
        androidHandler.savePicture(bitmapExif, storage, _name.value)
    }

    fun loadBitmapExif(media: Image): BitmapExif? {
        return androidHandler.loadBitmapExif(media)
    }

    fun loadUri(storageUri: StorageUri?) {
        storageUri?.also {
            androidHandler.loadMedia(storageUri)
        }
    }
}

sealed class CameraUiState {
    object Start : CameraUiState()
    object Saved : CameraUiState()
    object TakePicture : CameraUiState()
    class ShowNewPicture(val image: ImageProxy) : CameraUiState()
    class ShowOldPicture(val image: Image) : CameraUiState()
}