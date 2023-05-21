package com.rittmann.mediacontrol.create

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.Storage
import com.rittmann.core.tracker.track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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

    private var jobCameraIsAvailable: Job? = null
    private var jobImageSaved: Job? = null
    private var jobImageProxyTaken: Job? = null

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    init {
        androidHandler.requestCameraPermissions()

        jobCameraIsAvailable = viewModelScope.launch {
            androidHandler.cameraIsAvailable.collectLatest {
                _uiState.value = CameraUiState.TakePicture
            }
        }

        jobImageSaved = viewModelScope.launch {
            androidHandler.imageSaved.collectLatest { image ->
                image?.also {
                    _uiState.value = CameraUiState.Saved
                }
            }
        }

        jobImageProxyTaken = viewModelScope.launch {
            androidHandler.imageProxyTaken.collectLatest { image ->
                image?.also {
                    _uiState.value = CameraUiState.ShowPicture(image)
                }
            }
        }
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

    fun saveImage(bitmap: Bitmap, storage: Storage) {
        androidHandler.savePicture(bitmap, storage, _name.value)
    }

    override fun onCleared() {
        track()
        super.onCleared()
        androidHandler.disposeCameraMembers()

        jobCameraIsAvailable?.cancel()
        jobImageSaved?.cancel()
        jobImageProxyTaken?.cancel()
    }
}

sealed class CameraUiState {
    object Start : CameraUiState()
    object Saved : CameraUiState()
    object TakePicture : CameraUiState()
    class ShowPicture(val image: ImageProxy) : CameraUiState()
}