package com.rittmann.core.android

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.Cursor
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
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.camera.CameraHandler
import com.rittmann.core.data.BitmapExif
import com.rittmann.core.data.Image
import com.rittmann.core.exif.Exif
import com.rittmann.core.extensions.arePermissionsGranted
import com.rittmann.core.extensions.arePermissionsGrated
import com.rittmann.core.extensions.saveTo
import com.rittmann.core.tracker.track
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.flow.MutableStateFlow


class Android9Handler(
    private val context: Context,
    executorService: ExecutorService,
) : AndroidHandler {

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

    override val permissionStatusResult: ConflatedEventBus<PermissionStatusResult> =
        ConflatedEventBus()
    override val queueExecution: Queue<QueueExecution> = LinkedList()
    override var lastExecution: QueueExecution = QueueExecution.NONE
    override val cameraIsAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val imageSaved: MutableStateFlow<Image?> = MutableStateFlow(null)
    override val imageProxyTaken: MutableStateFlow<ImageProxy?> = MutableStateFlow(null)
    override val imageLoadedFromUri: MutableStateFlow<Image?> = MutableStateFlow(null)
    override val mediaImageList: MutableStateFlow<List<Image>> = MutableStateFlow(arrayListOf())
    override val mediaDeleted: MutableStateFlow<Image?> = MutableStateFlow(null)

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_9

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

    override fun loadThumbnail(media: Image): Bitmap {
        if (media.id == null) {
            return loadBitmap(media)
        }

        return MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            media.id,
            MediaStore.Images.Thumbnails.MINI_KIND,
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

    override fun deleteImage(media: Image) {
        track(media)
        if (media.uri.path == null) return

        when (media.storage) {
            Storage.INTERNAL -> {
                val file = File(media.uri.path!!)

                if (file.exists()) {
                    if (file.delete()) {
                        execute(lastExecution)

                        mediaDeleted.value = media
                    }
                }
            }

            Storage.EXTERNAL -> {
                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                )

                val selection = "${MediaStore.Images.Media._ID} = ?"
                val selectionArgs = arrayOf(media.id.toString())

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

                            mediaDeleted.value = media
                        }
                    }
                }
            }
        }
    }

    override fun disposeCameraMembers() {
        track()
        cameraIsAvailable.value = false
        imageProxyTaken.value = null
        imageSaved.value = null
        imageLoadedFromUri.value = null
        mediaDeleted.value = null
    }

    private fun getRealExternalPathFromUri(context: Context, contentUri: Uri): String? {
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

    private fun loadBitmap(uri: Uri): Bitmap {
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

    private fun generateFileName(name: String): String =
        if (name.isEmpty()) {
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US,
            ).format(System.currentTimeMillis()) + ".jpeg"
        } else {
            "$name.jpeg"
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

    private fun executeNextOnQueue() {
        track(queueExecution)
        execute(queueExecution.remove())
    }

    private fun execute(queueExecution: QueueExecution) {
        track(queueExecution)
        when (queueExecution) {
            QueueExecution.RETRIEVE_INTERNAL_MEDIA -> loadInternalMedia()
            QueueExecution.RETRIEVE_EXTERNAL_MEDIA -> loadExternalMedia()
            else -> {}
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