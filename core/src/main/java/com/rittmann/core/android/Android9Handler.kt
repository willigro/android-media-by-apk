package com.rittmann.core.android

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.camera.CameraHandler
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.data.StorageUri
import com.rittmann.core.exif.Exif
import com.rittmann.core.extensions.arePermissionsGranted
import com.rittmann.core.extensions.arePermissionsGrated
import com.rittmann.core.extensions.saveTo
import com.rittmann.core.tracker.track
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ExecutorService


class Android9Handler(
    context: Context,
    executorService: ExecutorService,
) : CentralHandler(context) {

    private val cameraHandler: CameraHandler = CameraHandler(executorService)

    companion object {
        private val PERMISSIONS_STORAGE = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        private const val PERMISSIONS_CAMERA = Manifest.permission.CAMERA
    }

    private var activityResultLauncherPermissions: ActivityResultLauncher<Array<String>>? = null
    private var activityResultLauncherSettings: ActivityResultLauncher<Intent>? = null
    private var activityResultLauncherCameraPermission: ActivityResultLauncher<String>? = null

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_9

    override fun registerPermissions(componentActivity: ComponentActivity) {
        registerLauncherStoragePermissions(componentActivity)
        registerLauncherSettings(componentActivity)
        registerLauncherCameraPermissions(componentActivity)
    }

    override fun requestPermissions(permissionStatusResult: PermissionStatusResult) {
        track(permissionStatusResult)
        when (permissionStatusResult.permission) {
            in PERMISSIONS_STORAGE -> {
                requestStoragePermissions()
            }

            PERMISSIONS_CAMERA -> {
                requestCameraPermissions()
            }
        }
    }

    override fun requestStoragePermissions() {
        activityResultLauncherPermissions?.launch(
            PERMISSIONS_STORAGE.toTypedArray()
        )
    }

    override fun requestCameraPermissions() {
        activityResultLauncherCameraPermission?.launch(
            PERMISSIONS_CAMERA
        )
    }

    override fun loadInternalMedia() {
        queueExecution.clear()

        if (checkStoragePermissionsAndScheduleExecutionCaseNeeded(
                QueueExecution.RETRIEVE_INTERNAL_MEDIA
            ).isDenied
        ) {
            return
        }

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

        track(imageList)
    }

    override fun loadExternalMedia() {
        queueExecution.clear()

        if (checkStoragePermissionsAndScheduleExecutionCaseNeeded(
                QueueExecution.RETRIEVE_EXTERNAL_MEDIA
            ).isDenied
        ) {
            return
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
        )

        val query = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            null,
        )

        val imageList = mutableListOf<Image>()

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    collection,
                    id,
                )

                imageList += Image(
                    uri = contentUri,
                    name = name,
                    id = id,
                    storage = Storage.EXTERNAL,
                )
            }
        }

        mediaImageList.value = imageList

        lastExecution = QueueExecution.RETRIEVE_EXTERNAL_MEDIA

        track(imageList)
    }

    override fun loadMedia(storageUri: StorageUri, mediaId: Long?) {
        Uri.parse(storageUri.uri)?.also { uri ->
            if (uri.path == null) return

            when (storageUri.storage) {
                Storage.INTERNAL -> {
                    val file = File(uri.path!!)

                    imageLoadedFromUri.value = Image(
                        uri = uri,
                        name = file.name,
                        id = mediaId,
                        storage = storageUri.storage,
                    )
                }

                Storage.EXTERNAL -> {
                    getRealExternalPathFromUri(context, uri)?.also { path ->
                        val file = File(path)

                        track("uri.path=${uri.path}, file=${file}")

                        imageLoadedFromUri.value = Image(
                            uri = Uri.fromFile(file),
                            name = file.name,
                            id = mediaId,
                            storage = storageUri.storage,
                        )
                    }
                }
            }
        }
    }

    override fun loadThumbnail(image: Image): Bitmap? {
        track(image)

        try {
            if (image.id == null) {
                return loadBitmap(image)
            }

            return MediaStore.Images.Thumbnails.getThumbnail(
                context.contentResolver,
                image.id,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null,
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
    }

    override fun loadBitmap(image: Image): Bitmap? {
        return loadBitmap(image.uri)
    }

    override fun loadBitmapExif(image: Image): BitmapExif? {
        return try {
            if (image.uri.path == null) return null

            val exifInterface = ExifInterface(File(image.uri.path!!))

            BitmapExif(
                bitmap = Exif.fixBitmapOrientation(exifInterface, loadBitmap(image.uri)),
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
        cameraHandler.takePhoto(
            imageCapture = imageCapture,
            onImageCaptured = {
                imageProxyTaken.value = it
            },
            onError = {
                track(it)
            },
        )
    }

    override fun savePicture(bitmapExif: BitmapExif, storage: Storage, name: String) {
        val file = if (storage == Storage.INTERNAL) {
            generateInternalFileToSave(name)
        } else {
            generateExternalFileToSave(name)
        }

        val path = bitmapExif.bitmap?.saveTo(file)

        Exif.saveExif(bitmapExif.exifInterface, path)

        when (storage) {
            Storage.INTERNAL -> {
                notifySavedInternalImage(file, storage)
            }

            Storage.EXTERNAL -> {
                notifySavedExternalImage(file)
            }
        }
    }

    override fun updateImage(
        bitmapExif: BitmapExif,
        storageUri: StorageUri,
        mediaId: Long?,
        name: String,
    ) {
        val uri = uriToUriFile(
            uri = Uri.parse(storageUri.uri)
        ) ?: return

        val file = File(uri.path!!)

        val oldName = file.name

        val newFile = if (storageUri.storage == Storage.INTERNAL) {
            generateInternalFileToSave(name)
        } else {
            generateExternalFileToSave(name)
        }

        file.renameTo(newFile)

        val path = bitmapExif.bitmap?.saveTo(newFile)

        Exif.saveExif(bitmapExif.exifInterface, path)

        when (storageUri.storage) {
            Storage.INTERNAL -> {
                Image(
                    uri = Uri.fromFile(newFile),
                    name = newFile.name,
                    id = null,
                    storage = Storage.INTERNAL,
                ).apply {
                    if (lastExecution == QueueExecution.RETRIEVE_INTERNAL_MEDIA) {
                        mediaImageList.update(this) {
                            it.name == oldName
                        }
                    }

                    imageSaved.tryEmit(this)
                }
            }

            Storage.EXTERNAL -> {
                val id = getMediaId(uri.path!!)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id ?: 0L,
                )

                contentUri.path?.let {
                    if (context.contentResolver.update(
                            contentUri,
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DATA, path)
                                put(MediaStore.Images.Media.DISPLAY_NAME, newFile.name)
                            },
                            null,
                            null
                        ) > 0
                    ) {
                        deleteThumbnail(id, contentUri)

                        scanFileAndNotifySavedImage(
                            file = newFile,
                            mediaId = id,
                        ) { image ->
                            if (lastExecution == QueueExecution.RETRIEVE_EXTERNAL_MEDIA) {
                                mediaImageList.update(image) {
                                    it.name == oldName
                                }
                            }

                            imageSaved.tryEmit(image)
                        }
                    }
                }
            }
        }
    }

    override fun deleteImage(image: Image) {
        track(image)
        if (image.uri.path == null) return

        when (image.storage) {
            Storage.INTERNAL -> {
                val file = File(image.uri.path!!)

                if (file.exists()) {
                    if (file.delete()) {
                        if (lastExecution == QueueExecution.RETRIEVE_INTERNAL_MEDIA) {
                            mediaImageList.delete(image)
                        }

                        mediaDeleted.value = image
                    }
                }
            }

            Storage.EXTERNAL -> {
                val mediaId = getMediaId(image.uri.path!!)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    mediaId ?: 0L
                )

                contentUri.path?.let {
                    if (context.contentResolver.delete(contentUri, null, null) > 0) {
                        if (lastExecution == QueueExecution.RETRIEVE_EXTERNAL_MEDIA) {
                            mediaImageList.delete(image)
                        }

                        mediaDeleted.value = image
                    }
                }
            }
        }
    }

    private fun deleteThumbnail(mediaId: Long?, contentUri: Uri) {
        track("data=$mediaId, contentUri=$contentUri")

        val thumbnails = context.contentResolver.query(
            MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
            null,
            MediaStore.Images.Thumbnails.IMAGE_ID + "=?",
            arrayOf(mediaId.toString()),
            null
        )

        thumbnails?.use {
            thumbnails.moveToFirst()
            while (!thumbnails.isAfterLast) {
                val idIndex = thumbnails.getColumnIndex(MediaStore.Images.Thumbnails._ID)
                val dataIndex = thumbnails.getColumnIndex(MediaStore.Images.Thumbnails.DATA)

                val thumbnailId = thumbnails.getLong(idIndex)
                val path = thumbnails.getString(dataIndex)

                val file = File(path)

                if (file.delete()) {
                    context.contentResolver.delete(
                        MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Thumbnails._ID + "=?",
                        arrayOf(thumbnailId.toString())
                    )
                }

                thumbnails.moveToNext()
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        track(uri)
        return try {
            MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                uri,
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun generateExternalFileToSave(name: String): File {
        track(name)
        val directory: String = Environment.getExternalStorageDirectory().toString()

        val myDir = File(directory)

        if (!myDir.exists()) {
            myDir.mkdirs()
        }

        return File(
            directory,
            generateFileName(name),
        )
    }

    private fun checkStoragePermissionsAndScheduleExecutionCaseNeeded(
        execution: QueueExecution,
    ): PermissionStatusResult {
        val hasPermission = PERMISSIONS_STORAGE.arePermissionsGrated(context)

        if (hasPermission.isDenied) {
            requestStoragePermissions()
            queueExecution.add(execution)
        }

        track("hasPermission=$hasPermission, execution=$execution")

        return hasPermission
    }

    private fun registerLauncherSettings(componentActivity: ComponentActivity) {
        activityResultLauncherSettings = componentActivity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val permissionStatus = PERMISSIONS_STORAGE.arePermissionsGrated(context)

            if (permissionStatus.isDenied) {
                this.permissionStatusResult.send(permissionStatus)
            } else {
                executeNextOnQueue()
            }

            track(result)
        }
    }

    private fun registerLauncherStoragePermissions(componentActivity: ComponentActivity) {
        activityResultLauncherPermissions = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val permissionStatus = permissions.entries.arePermissionsGranted()

            if (permissionStatus.isDenied) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    track("try check each permission")
                    for (permission in PERMISSIONS_STORAGE) {
                        if (componentActivity.shouldShowRequestPermissionRationale(
                                permission
                            ).not()
                        ) {
                            track("blocked, open settings")
                            // Permission was blocked, request for the settings
                            openSettingsScreen()

                            return@registerForActivityResult
                        }
                    }

                    track("permission was just denied")
                    // Permission was not blocked, try request again
                    this.permissionStatusResult.send(permissionStatus)
                } else {
                    track("normal, open settings")
                    // Permission was blocked, request for the settings
                    openSettingsScreen()
                }
            } else {
                executeNextOnQueue()
            }
        }
    }

    private fun registerLauncherCameraPermissions(componentActivity: ComponentActivity) {
        activityResultLauncherCameraPermission = componentActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {
                cameraIsAvailable.value = true
            } else {
                permissionStatusResult.send(
                    PermissionStatusResult(permission = PERMISSIONS_CAMERA, isDenied = true)
                )
            }

            track("camera=$isGranted")
        }
    }

    private fun openSettingsScreen() {
        activityResultLauncherSettings?.launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}