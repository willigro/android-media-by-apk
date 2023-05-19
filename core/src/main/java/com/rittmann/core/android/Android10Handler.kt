package com.rittmann.core.android

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rittmann.core.data.Image
import java.util.*
import kotlinx.coroutines.flow.StateFlow

class Android10Handler: AndroidHandler {

    private val queueExecution: Queue<QueueExecution> = LinkedList()

    override val permissionStatusResult: ConflatedEventBus<PermissionStatusResult> = ConflatedEventBus(PermissionStatusResult())

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10
    override fun registerPermissions(componentActivity: ComponentActivity) {
        TODO("Not yet implemented")
    }

    override fun requestPermissions(permissionStatusResult: PermissionStatusResult) {
        TODO("Not yet implemented")
    }

    override fun requestStoragePermissions() {
        TODO("Not yet implemented")
    }

    override fun requestCameraPermissions() {
        TODO("Not yet implemented")
    }

    override fun mediaList(): StateFlow<List<Image>> {
        TODO("Not yet implemented")
    }

    override fun cameraIsAvailable(): StateFlow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun loadThumbnailFor(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmapFor(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun takePhoto(
        imageCapture: ImageCapture,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        TODO("Not yet implemented")
    }
}