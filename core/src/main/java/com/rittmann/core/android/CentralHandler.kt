package com.rittmann.core.android

import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.data.StorageUri
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow

open class CentralHandler(
    protected val context: Context
) : AndroidHandler {

    companion object {
        const val INTERNAL_DIRECTORY = "imageDir"
        const val EXTERNAL_DIRECTORY_APP_PATH = "imageDir"
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

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10
    override fun registerPermissions(componentActivity: ComponentActivity) {

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

    override fun loadMedia(storageUri: StorageUri, mediaId: Long?) {
        TODO("Not yet implemented")
    }

    override fun loadThumbnail(image: Image): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun loadBitmap(image: Image): Bitmap? {
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
        mediaId: Long?,
        name: String
    ) {
        TODO("Not yet implemented")
    }

    override fun deleteImage(image: Image) {
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

    protected fun uriToUriFile(uri: Uri): Uri? {
        track(uri)
        track(uri.path)

        if (uri.toString().contains("file://")) {
            return uri
        }

        return getRealExternalPathFromUri(context, uri)?.let { path ->
            val file = File(path)

            Uri.fromFile(file)
        }
    }

    protected fun getRealExternalPathFromUri(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)?.let { index ->
                cursor.moveToFirst()
                cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }

    protected fun generateInternalFileToSave(name: String): File {
        val cw = ContextWrapper(context)

        val directory = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

        return File(
            directory,
            generateFileName(name),
        )
    }

    protected fun scanFileAndNotifySavedImage(
        file: File,
        mediaId: Long?,
        scanned: (Image) -> Unit
    ) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.toString()),
            null
        ) { path, uri ->
            track("path=$path, uri=$uri")
            Image(
                uri = uri,
                name = file.name,
                id = mediaId ?: getMediaId(path.orEmpty()),
                storage = Storage.EXTERNAL,
            ).apply {
                scanned(this)
            }
        }
    }

    protected fun getMediaId(data: String): Long? {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
        )

        val selection = "${MediaStore.Images.Media.DATA} = ?"
        val selectionArgs = arrayOf(data)

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null,
        )

        return query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            if (cursor.moveToFirst()) {
                cursor.getLong(idColumn)
            } else {
                null
            }
        }
    }

    protected fun notifySavedExternalImage(file: File) {
        scanFileAndNotifySavedImage(
            file = file,
            mediaId = null,
        ) { image ->
            if (lastExecution == QueueExecution.RETRIEVE_EXTERNAL_MEDIA) {
                mediaImageList.value += image
            }

            imageSaved.tryEmit(image)
        }
    }

    protected fun notifySavedInternalImage(file: File, storage: Storage) {
        val image = Image(
            uri = Uri.fromFile(file),
            name = file.name,
            id = null,
            storage = storage,
        )

        if (lastExecution == QueueExecution.RETRIEVE_INTERNAL_MEDIA) {
            mediaImageList.value += image
        }

        imageSaved.tryEmit(image)
    }

    // TODO maybe I need to remove it
    protected fun saveToInternalStorage(bitmapImage: Bitmap): String? {
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
        return myPath.absolutePath
    }
}