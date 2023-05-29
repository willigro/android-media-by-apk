package com.rittmann.core.android

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
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

        const val INTERNAL_DIRECTORY = "imageDir"
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

        val selection = ""
        val selectionArgs = arrayOf<String>()

        val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
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
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
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

    override fun loadImageFromUri(storageUri: StorageUri) {
        val uri = uriToUriFile(
            uri = Uri.parse(storageUri.uri)
        ) ?: return

        if (uri.path == null) return

        imageLoadedFromUri.value = Image(
            uri = uri,
            name = File(uri.path!!).name,
            id = storageUri.mediaId,
            storage = storageUri.storage,
        )
    }

    override fun loadThumbnail(image: Image): Bitmap {
        track(image)

        if (image.id == null) {
            return loadBitmap(image)
        }

        return MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            image.id,
            MediaStore.Images.Thumbnails.MINI_KIND,
            null,
        )
    }

    override fun loadBitmap(image: Image): Bitmap {
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
        track(storage)
        when (storage) {
            Storage.INTERNAL -> {
                val file = generateInternalFileToSave(name)

                track(
                    "Saving exif=${
                        bitmapExif.exifInterface?.getAttribute(ExifInterface.TAG_DATETIME)
                            .toString()
                    }"
                )

                val path = bitmapExif.bitmap?.saveTo(file)

                Exif.saveExif(bitmapExif.exifInterface, path)

                if (lastExecution == QueueExecution.RETRIEVE_INTERNAL_MEDIA) {
                    execute(lastExecution)
                }

                Image(
                    uri = Uri.fromFile(file),
                    name = file.name,
                    id = null,
                    storage = storage,
                ).apply {
                    imageSaved.tryEmit(this)
                }
            }

            Storage.EXTERNAL -> {
                val file = generateExternalFileToSave(name)

                val savedPath = bitmapExif.bitmap?.saveTo(file)

                Exif.saveExif(bitmapExif.exifInterface, savedPath)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.toString()),
                    null
                ) { path, uri ->
                    track("path=$path, uri=$uri, ${Uri.fromFile(file)}")

                    if (lastExecution == QueueExecution.RETRIEVE_EXTERNAL_MEDIA) {
                        execute(lastExecution)
                    }

                    Image(
                        uri = uri,
                        name = file.name,
                        id = null,
                        storage = storage,
                    ).apply {
                        track("Saving image=$this")
                        imageSaved.tryEmit(this)
                    }
                }
            }
        }
    }

    override fun updateImage(
        bitmapExif: BitmapExif,
        storageUri: StorageUri,
        name: String,
    ) {
        when (storageUri.storage) {
            Storage.INTERNAL -> {
                val file = File(Uri.parse(storageUri.uri).path!!)

                val newFile = generateInternalFileToSave(
                    if (name.contains(".")) {
                        name.split(".")[0]
                    } else {
                        name
                    }
                )

                file.renameTo(newFile)

                val path = bitmapExif.bitmap?.saveTo(newFile)

                Exif.saveExif(bitmapExif.exifInterface, path)

                if (lastExecution == QueueExecution.RETRIEVE_INTERNAL_MEDIA) {
                    execute(lastExecution)
                }

                Image(
                    uri = Uri.fromFile(file),
                    name = newFile.name,
                    id = null,
                    storage = Storage.EXTERNAL,
                ).apply {
                    imageSaved.tryEmit(this)
                }
            }

            Storage.EXTERNAL -> {
                val file = File(getRealExternalPathFromUri(context, Uri.parse(storageUri.uri))!!)

                val newFile = generateExternalFileToSave(
                    name
                )

                val newName = newFile.name

                file.renameTo(newFile)

                val path = bitmapExif.bitmap?.saveTo(newFile)

                Exif.saveExif(bitmapExif.exifInterface, path)

                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                )

                val selection = "${MediaStore.Images.Media._ID} = ?"
                val selectionArgs = arrayOf(storageUri.mediaId.toString())

                val query = context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                )

                query?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(idColumn)

                        val contentUri: Uri = ContentUris.withAppendedId(
                            collection,
                            id
                        )

                        contentUri.path?.let {
                            if (context.contentResolver.update(
                                    contentUri,
                                    ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, newName)
                                        put(MediaStore.Images.Media.DATA, path)
                                    },
                                    null,
                                    null
                                ) > 0
                            ) {
                                deleteThumbnail(storageUri.mediaId, contentUri)

                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(newFile.toString()),
                                    null
                                ) { _, _ ->
                                    execute(lastExecution)

                                    Image(
                                        uri = contentUri,
                                        name = newName,
                                        id = storageUri.mediaId,
                                        storage = Storage.EXTERNAL,
                                    ).apply {
                                        track("Saving image=$this")
                                        imageSaved.tryEmit(this)
                                    }
                                }
                            }
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
                        execute(lastExecution)

                        mediaDeleted.value = image
                    }
                }
            }

            Storage.EXTERNAL -> {
                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                )

                val selection = "${MediaStore.Images.Media._ID} = ?"
                val selectionArgs = arrayOf(image.id.toString())

                val query = context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                )

                query?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(idColumn)

                        val contentUri: Uri = ContentUris.withAppendedId(
                            collection,
                            id
                        )

                        contentUri.path?.let {
                            if (context.contentResolver.delete(contentUri, null, null) > 0) {
                                execute(lastExecution)
                            }

                            mediaDeleted.value = image
                        }
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

    private fun loadBitmap(uri: Uri): Bitmap {
        track(uri)
        return MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri,
        )
    }

    private fun generateInternalFileToSave(name: String): File {
        val cw = ContextWrapper(context)

        val directory = cw.getDir(INTERNAL_DIRECTORY, Context.MODE_PRIVATE)

        return File(
            directory,
            generateFileName(name),
        )
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