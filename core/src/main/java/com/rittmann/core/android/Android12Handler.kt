package com.rittmann.core.android

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import com.rittmann.core.data.Image
import java.util.*
import kotlinx.coroutines.flow.StateFlow

class Android12Handler: AndroidHandler {

    override val permissionStatusResult: ConflatedEventBus<PermissionStatusResult> = ConflatedEventBus(PermissionStatusResult())
    override val queueExecution: Queue<QueueExecution> = LinkedList()
    override var lastExecution: QueueExecution = QueueExecution.NONE

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_12
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

    override fun pictureSaved(): StateFlow<Image?> {
        TODO("Not yet implemented")
    }

    override fun pictureTaken(): StateFlow<ImageProxy?> {
        TODO("Not yet implemented")
    }

    override fun loadThumbnailFor(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmapFor(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun takePhoto(
        imageCapture: ImageCapture
    ) {
        TODO("Not yet implemented")
    }

    override fun savePicture(bitmap: Bitmap) {
        TODO("Not yet implemented")
    }
}