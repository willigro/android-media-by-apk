package com.rittmann.core.android

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.data.StorageUri
import com.rittmann.core.tracker.track
import java.util.*
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.flow.MutableStateFlow


interface AndroidHandler {
    val permissionStatusResult: ConflatedEventBus<PermissionStatusResult>
    val queueExecution: Queue<QueueExecution>
    var lastExecution: QueueExecution
    val cameraIsAvailable: MutableStateFlow<Boolean>
    val imageSaved: MutableStateFlow<Image?>
    val imageProxyTaken: MutableStateFlow<ImageProxy?>
    val imageLoadedFromUri: MutableStateFlow<Image?>
    val mediaImageList: MutableStateFlow<List<Image>>
    val mediaDeleted: MutableStateFlow<Image?>

    fun version(): AndroidVersion = AndroidVersion.ANDROID_9

    // TODO make all implement it

    fun registerPermissions(componentActivity: ComponentActivity)
    fun requestPermissions(permissionStatusResult: PermissionStatusResult)
    fun requestStoragePermissions()
    fun requestCameraPermissions()
    fun loadInternalMedia() {}
    fun loadExternalMedia() {}
    fun loadMedia(storageUri: StorageUri, mediaId: Long?)
    fun loadThumbnail(image: Image): Bitmap?
    fun loadBitmap(image: Image): Bitmap?
    fun loadBitmapExif(image: Image): BitmapExif?
    fun takePhoto(imageCapture: ImageCapture)
    fun savePicture(bitmapExif: BitmapExif, storage: Storage, name: String)
    fun updateImage(bitmapExif: BitmapExif, storageUri: StorageUri, mediaId: Long?, name: String)
    fun deleteImage(image: Image)
    fun disposeCameraMembers()
}

object AndroidHandlerFactory {

    fun create(context: Context, executor: ExecutorService): AndroidHandler {
        return when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> Android10Handler(context)
            Build.VERSION.SDK_INT == Build.VERSION_CODES.R -> Android11Handler(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> Android12Handler(context)
            else -> Android9Handler(context, executor)
        }
    }
}

enum class AndroidVersion {
    ANDROID_9, ANDROID_10, ANDROID_11, ANDROID_12
}

enum class QueueExecution {
    NONE,
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

enum class Storage(val value: String) {
    INTERNAL("0"), EXTERNAL("1")
}

fun MutableStateFlow<List<Image>>.delete(image: Image) {
    val list = this.value

    val index = list.indexOfFirst { it.name == image.name }

    if (index != -1) {
        val arr = arrayListOf<Image>()
        arr.addAll(list)
        arr.removeAt(index)

        this.value = arr
    }
}

fun MutableStateFlow<List<Image>>.update(image: Image, predicate: (Image) -> Boolean) {
    val list = this.value

    val index = list.indexOfFirst(predicate)

    if (index != -1) {
        val arr = arrayListOf<Image>()
        arr.addAll(list)
        arr[index] = image

        this.value = arr
    }
}