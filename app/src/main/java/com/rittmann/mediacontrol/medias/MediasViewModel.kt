package com.rittmann.mediacontrol.medias

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.android.AndroidVersion
import com.rittmann.core.data.Image
import com.rittmann.core.lifecycle.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class MediasViewModel @Inject constructor(
    private val androidHandler: AndroidHandler,
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<MediasUiState> = MutableStateFlow(
        MediasUiState.InitialState(androidHandler.version())
    )
    val uiState: StateFlow<MediasUiState>
        get() = _uiState

    init {
        androidHandler.loadInternalMedia()

        viewModelScope.launch {
            androidHandler.mediaList().collectLatest {
                _uiState.value.mediaList.value = it
            }
        }
    }

    fun loadBitmapFor(media: Image): Bitmap {
        return androidHandler.loadThumbnailFor(media)
    }

    fun loadInternalMedia() {
        androidHandler.loadInternalMedia()
    }

    fun loadExternalMedia() {
        androidHandler.loadExternalMedia()
    }
}

sealed class MediasUiState(
    val androidVersion: AndroidVersion,
    val mediaList: MutableStateFlow<List<Image>> = MutableStateFlow(arrayListOf()),
) {
    class InitialState(androidVersion: AndroidVersion) : MediasUiState(androidVersion)
}