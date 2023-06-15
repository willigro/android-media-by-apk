package com.rittmann.core.android

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.R
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.exif.Exif
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow

@RequiresApi(Build.VERSION_CODES.Q)
class Android10Handler(
    private val context: Context,
) : AndroidHandler {

    companion object {
        const val INTERNAL_DIRECTORY = "imageDir"
    }

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

    init {
        track()

        // TODO: mocking, remove me later
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                track(
                    "mocking bitmap=${
                        saveToInternalStorage(
                            BitmapFactory.decodeResource(context.resources, R.drawable.untitled)
                        )
                    }"
                )
            } else {
                track("Cannot mock bitmap")
            }
        } catch (e: IOException) {
            track(e)
            e.printStackTrace()
        }
    }

    override fun loadInternalMedia() {
        queueExecution.clear()

        val cw = ContextWrapper(context)

        val directory: File = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)
        val files = directory.listFiles()

        val imageList = mutableListOf<Image>()

        files?.filter { file ->
            file.canRead()
        }?.map { file ->
            imageList += Image(
                uri = Uri.fromFile(file),
                name = file.name,
                id = null,
                storage = Storage.INTERNAL,
            )
        } ?: listOf()

        mediaImageList.value = imageList

        lastExecution = QueueExecution.RETRIEVE_INTERNAL_MEDIA

        track("imageList=$imageList")
    }

    override fun loadExternalMedia() {

    }

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10
    override fun registerPermissions(componentActivity: ComponentActivity) {

    }

    override fun requestPermissions(permissionStatusResult: PermissionStatusResult) {

    }

    override fun requestStoragePermissions() {

    }

    override fun requestCameraPermissions() {

    }

    override fun loadMedia(storageUri: StorageUri, mediaId: Long?) {

    }

    override fun loadThumbnail(media: Image): Bitmap {
        track(media)

        return context.contentResolver.loadThumbnail(
            media.uri,
            Size(100, 100),
            null,
        )
    }

    override fun loadBitmap(media: Image): Bitmap {
        return loadBitmap(media.uri)
    }

    override fun loadBitmapExif(media: Image): BitmapExif? {
        return try {
            if (media.uri.path == null) return null

            val exifInterface = ExifInterface(File(media.uri.path!!))

            BitmapExif(
                bitmap = Exif.fixBitmapOrientation(exifInterface, loadBitmap(media.uri)),
                exifInterface = exifInterface,
            )
        } catch (e: IOException) {
            track(e)
            null
        }
    }

    override fun takePhoto(
        imageCapture: ImageCapture
    ) {

    }

    override fun savePicture(bitmapExif: BitmapExif, storage: Storage, name: String) {

    }

    override fun updateImage(
        bitmapExif: BitmapExif,
        storageUri: StorageUri,
        mediaId: Long?,
        name: String
    ) {

    }

    override fun deleteImage(media: Image) {

    }

    override fun disposeCameraMembers() {

    }

    private fun loadBitmap(uri: Uri): Bitmap {
        track(uri)
        return MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri,
        )
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap): String? {
        val cw = ContextWrapper(context)

        val directory: File = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

        val myPath = File(directory, "profile.jpg")

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(myPath)

            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: Exception) {
            track(e)
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                track(e)
                e.printStackTrace()
            }
        }
        return directory.absolutePath
    }
}