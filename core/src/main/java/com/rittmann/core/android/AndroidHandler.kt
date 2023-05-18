package com.rittmann.core.android

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LiveData
import com.rittmann.core.data.Image
import com.rittmann.core.tracker.track
import java.util.LinkedList
import java.util.Queue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull


interface AndroidHandler {
    val permissionIsDenied: ConflatedEventBus<Boolean>
    fun version(): AndroidVersion = AndroidVersion.ANDROID_9

    // TODO make all implement it
    fun loadMedia() {}

    fun registerPermissions(componentActivity: ComponentActivity)
    fun requestPermissions()
    fun permissionObserver(): LiveData<Unit>
    fun mediaList(): StateFlow<List<Image>>
    fun loadThumbnailFor(media: Image): Bitmap
    fun loadBitmapFor(media: Image): Bitmap
}

object AndroidHandlerFactory {

    fun create(context: Context): AndroidHandler {
        val sdk = android.os.Build.VERSION.SDK_INT
        return when {
            sdk == android.os.Build.VERSION_CODES.Q -> Android10Handler()
            sdk == android.os.Build.VERSION_CODES.R -> Android11Handler()
            sdk >= android.os.Build.VERSION_CODES.S -> Android12Handler()
            else -> Android9Handler(context)
        }
    }
}

enum class AndroidVersion {
    ANDROID_9, ANDROID_10, ANDROID_11, ANDROID_12
}

enum class QueueExecution {
    RETRIEVE_MEDIA
}

class ConflatedEventBus<T : Any>(initialValue: T? = null) {
    private val state = MutableStateFlow(Pair(Integer.MIN_VALUE, initialValue))

    val flow = state

    fun send(data: T) {
        track(data)
        state.value = Pair(state.value.first + 1, data)
    }
}