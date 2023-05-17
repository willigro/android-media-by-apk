package com.rittmann.mediacontrol.medias

import com.rittmann.core.android.AndroidHandler
import com.rittmann.core.lifecycle.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class MediasViewModel @Inject constructor(
    androidHandler: AndroidHandler,
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<MediasUiState> = MutableStateFlow(
        MediasUiState.InitialState(androidHandler)
    )
    val uiState: StateFlow<MediasUiState>
        get() = _uiState
}

sealed class MediasUiState(
    val androidHandler: AndroidHandler
) {
    class InitialState(androidHandler: AndroidHandler) : MediasUiState(androidHandler)
}