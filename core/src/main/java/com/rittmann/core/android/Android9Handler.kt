package com.rittmann.core.android

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
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
import com.rittmann.core.data.Image
import com.rittmann.core.extensions.arePermissionsGranted
import com.rittmann.core.extensions.arePermissionsGrated
import com.rittmann.core.extensions.saveTo
import com.rittmann.core.tracker.track
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
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
    override val mediaImageList: MutableStateFlow<List<Image>> = MutableStateFlow(arrayListOf())

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
            imageList += Image(uri = Uri.fromFile(file), name = file.name, id = null)
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
                imageList += Image(uri = contentUri, name = name, id = id)
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

    override fun loadThumbnailFor(media: Image): Bitmap {
        if (media.id == null) {
            return loadBitmapFor(media)
        }

        return MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            media.id,
            MediaStore.Images.Thumbnails.MINI_KIND,
            null,
        )
    }

    override fun loadBitmapFor(media: Image): Bitmap {
        return MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            media.uri,
        )
    }

    override fun takePhoto(
        imageCapture: ImageCapture
    ) {
        cameraHandler.takePhoto(
            imageCapture = imageCapture,
            onImageCaptured = {
                imageProxyTaken.value = it
//                Exif.handleExif(Exif.proxyToExif(it))
            },
            onError = {
                track(it)
            },
        )
    }

    override fun savePicture(bitmap: Bitmap, storage: Storage, name: String) {
        track(storage)
        when (storage) {
            Storage.INTERNAL -> {
                val file = generateInternalFileToSave(name)

                bitmap.saveTo(file)

                Image(uri = Uri.fromFile(file), name = file.name, id = null).apply {
                    imageSaved.tryEmit(this)
                    Exif.loadExif(context, uri)
                }

                if (lastExecution == QueueExecution.RETRIEVE_INTERNAL_MEDIA) {
                    execute(lastExecution)
                }
            }
            Storage.EXTERNAL -> {
                val file = generateExternalFileToSave(name)

                bitmap.saveTo(file)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.toString()),
                    null
                ) { path, uri ->
                    track("path=$path, uri=$uri, ${Uri.fromFile(file)}")

                    Image(uri = uri, name = file.name, id = null).apply {
                        track("Saving image=$this")
                        imageSaved.tryEmit(this)
                        Exif.loadExif(context, uri)
                    }

                    if (lastExecution == QueueExecution.RETRIEVE_EXTERNAL_MEDIA) {
                        execute(lastExecution)
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

object Exif {

    fun fixBitmapOrientation(exifInterface: ExifInterface?, bitmap: Bitmap?): Bitmap? {
        val matrix = Matrix()

        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_FLIP_HORIZONTAL")
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
                track("ExifInterface.ORIENTATION_ROTATE_180")
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_FLIP_VERTICAL")
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_TRANSPOSE")
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
                track("ExifInterface.ORIENTATION_ROTATE_90")
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
                track("ExifInterface.ORIENTATION_TRANSVERSE")
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(-90f)
                track("ExifInterface.ORIENTATION_ROTATE_270")
            }
            ExifInterface.ORIENTATION_UNDEFINED -> {
                matrix.setRotate(-90f)
                track("ExifInterface.ORIENTATION_UNDEFINED")
            }
        }

        if (bitmap == null) return null

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    fun proxyToExif(proxy: ImageProxy): ExifInterface {
        val bb = proxy.planes[0].buffer
        try {
            val buffer = ByteArray(bb.remaining())
            bb.get(buffer)
            return ExifInterface(ByteArrayInputStream(buffer))
        } finally {
            bb.clear()
        }
    }

    fun getOrientation(context: Context, photoUri: Uri): Int {
        var cursor: Cursor? = context.contentResolver.query(
            photoUri,
            arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
            null,
            null,
            null
        )
        if (cursor?.count != 1) {
            cursor?.close()
            return -1
        }
        cursor.moveToFirst()
        val orientation: Int = cursor.getInt(0)
        cursor.close()
        cursor = null
        return orientation
    }

    fun loadExif(context: Context, uri: Uri) {
        track(getOrientation(context, uri))

        var gfgIn: InputStream? = null
        try {
            gfgIn = context.contentResolver.openInputStream(uri)
            val exifInterface = gfgIn?.let {
                ExifInterface(it)
            }
            // handleExif(exifInterface)
            // Extract the EXIF tag here
        } catch (e: IOException) {
            track(e)
            // Handle any errors
        } finally {
            if (gfgIn != null) {
                try {
                    gfgIn.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }
}