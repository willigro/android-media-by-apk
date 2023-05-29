package com.rittmann.core.android

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.data.StorageUri
import com.rittmann.core.tracker.track
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow

open class CentralHandler : AndroidHandler {

    override val permissionStatusResult: ConflatedEventBus<PermissionStatusResult> =
        ConflatedEventBus(PermissionStatusResult())
    override val queueExecution: Queue<QueueExecution> = LinkedList()
    override var lastExecution: QueueExecution = QueueExecution.NONE
    override val cameraIsAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val imageSaved: MutableStateFlow<Image?> = MutableStateFlow(null)
    override val imageProxyTaken: MutableStateFlow<ImageProxy?> = MutableStateFlow(null)
    override val imageLoadedFromUri: MutableStateFlow<Image?> = MutableStateFlow(null)
    override val mediaImageList: MutableStateFlow<List<Image>> = MutableStateFlow(arrayListOf())
    override val mediaDeleted: MutableStateFlow<Image?> = MutableStateFlow(null)

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

    override fun loadImageFromUri(storageUri: StorageUri) {
        TODO("Not yet implemented")
    }

    override fun loadThumbnail(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmap(media: Image): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadBitmapExif(media: Image): BitmapExif? {
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

    override fun deleteImage(media: Image) {
        TODO("Not yet implemented")
    }

    override fun disposeCameraMembers() {
        track()
        cameraIsAvailable.value = false
        imageProxyTaken.value = null
        imageSaved.value = null
        imageLoadedFromUri.value = null
        mediaDeleted.value = null
    }

    protected fun executeNextOnQueue() {
        track(queueExecution)
        execute(queueExecution.remove())
    }

    protected fun execute(queueExecution: QueueExecution) {
        track(queueExecution)
        when (queueExecution) {
            QueueExecution.RETRIEVE_INTERNAL_MEDIA -> loadInternalMedia()
            QueueExecution.RETRIEVE_EXTERNAL_MEDIA -> loadExternalMedia()
            else -> {}
        }
    }

    protected fun generateFileName(name: String): String =
        if (name.isEmpty()) {
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US,
            ).format(System.currentTimeMillis()) + ".jpeg"
        } else {
            val n = if (name.contains(".")) {
                name.split(".")[0]
            } else {
                name
            }

            "$n.jpeg"
        }
}