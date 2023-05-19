package com.rittmann.core.android

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.rittmann.core.data.Image
import com.rittmann.core.tracker.track
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


interface AndroidHandler {
    val permissionStatusResult: ConflatedEventBus<PermissionStatusResult>
    fun version(): AndroidVersion = AndroidVersion.ANDROID_9

    // TODO make all implement it
    fun loadInternalMedia() {}
    fun loadExternalMedia() {}

    fun registerPermissions(componentActivity: ComponentActivity)
    fun requestPermissions(permissionStatusResult: PermissionStatusResult)
    fun requestStoragePermissions()
    fun requestCameraPermissions()
    fun mediaList(): StateFlow<List<Image>>
    fun cameraIsAvailable(): StateFlow<Boolean>
    fun loadThumbnailFor(media: Image): Bitmap
    fun loadBitmapFor(media: Image): Bitmap
    fun takePhoto(
        imageCapture: ImageCapture,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    )
}

object AndroidHandlerFactory {

    fun create(context: Context, executor: ExecutorService): AndroidHandler {
        val sdk = android.os.Build.VERSION.SDK_INT
        return when {
            sdk == android.os.Build.VERSION_CODES.Q -> Android10Handler()
            sdk == android.os.Build.VERSION_CODES.R -> Android11Handler()
            sdk >= android.os.Build.VERSION_CODES.S -> Android12Handler()
            else -> Android9Handler(context, executor)
        }
    }
}

enum class AndroidVersion {
    ANDROID_9, ANDROID_10, ANDROID_11, ANDROID_12
}

enum class QueueExecution {
    RETRIEVE_INTERNAL_MEDIA,
    RETRIEVE_EXTERNAL_MEDIA,
}

class ConflatedEventBus<T : Any>(initialValue: T? = null) {
    private val state = MutableStateFlow(Pair(Integer.MIN_VALUE, initialValue))

    val flow = state

    fun send(data: T) {
        track(data)
        state.value = Pair(state.value.first + 1, data)
    }
}

data class PermissionStatusResult(
    val permission: String? = null,
    val isDenied: Boolean = false,
)