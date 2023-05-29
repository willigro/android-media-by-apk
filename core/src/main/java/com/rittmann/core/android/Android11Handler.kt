package com.rittmann.core.android

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.data.StorageUri
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow

class Android11Handler: AndroidHandler {

    override val permissionStatusResult: ConflatedEventBus<PermissionStatusResult> = ConflatedEventBus(PermissionStatusResult())
    override val queueExecution: Queue<QueueExecution> = LinkedList()
    override var lastExecution: QueueExecution = QueueExecution.NONE
    override val cameraIsAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val imageSaved: MutableStateFlow<Image?> = MutableStateFlow(null)
    override val imageProxyTaken: MutableStateFlow<ImageProxy?> = MutableStateFlow(null)
    override val imageLoadedFromUri: MutableStateFlow<Image?> = MutableStateFlow(null)
    override val mediaImageList: MutableStateFlow<List<Image>> = MutableStateFlow(arrayListOf())
    override val mediaDeleted: MutableStateFlow<Image?> = MutableStateFlow(null)

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_11
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

    override fun loadImageFromUri(storageUri: StorageUri) {
        TODO("Not yet implemented")
    }

    override fun loadThumbnail(image: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmap(image: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmapExif(image: Image): BitmapExif? {
        TODO("Not yet implemented")
    }

    override fun takePhoto(
        imageCapture: ImageCapture
    ) {
        TODO("Not yet implemented")
    }

    override fun savePicture(bitmapExif: BitmapExif, storage: Storage, name: String) {
        TODO("Not yet implemented")
    }

    override fun updateImage(
        bitmapExif: BitmapExif,
        storageUri: StorageUri,
        name: String
    ) {
        TODO("Not yet implemented")
    }

    override fun deleteImage(image: Image) {
        TODO("Not yet implemented")
    }

    override fun disposeCameraMembers() {
        TODO("Not yet implemented")
    }
}