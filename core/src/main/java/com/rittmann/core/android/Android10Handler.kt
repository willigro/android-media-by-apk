package com.rittmann.core.android

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.exifinterface.media.ExifInterface
import com.rittmann.core.R
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
import java.util.concurrent.ExecutorService


@RequiresApi(Build.VERSION_CODES.Q)
class Android10Handler(
    context: Context,
    executorService: ExecutorService,
) : CentralHandler(context) {

    companion object {
        private val PERMISSIONS_STORAGE = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        private const val PERMISSIONS_CAMERA = Manifest.permission.CAMERA
    }

    private val cameraHandler: CameraHandler = CameraHandler(executorService)

    private var activityResultLauncherPermissions: ActivityResultLauncher<Array<String>>? = null
    private var activityResultLauncherSettings: ActivityResultLauncher<Intent>? = null
    private var activityResultLauncherCameraPermission: ActivityResultLauncher<String>? = null

    init {
        track()

        // TODO: mocking, remove me later
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val fileName = saveToInternalStorage(
                    BitmapFactory.decodeResource(context.resources, R.drawable.untitled)
                )
                track(
                    "mocking bitmap=${
                        fileName
                    }"
                )

                fileName?.also {
                    val bitmap = ThumbnailUtils.createImageThumbnail(
                        File(fileName),
                        Size(120, 120),
                        null,
                    )

                    track(bitmap)
                }

            } else {
                track("Cannot mock bitmap")
            }
        } catch (e: IOException) {
            track(e)
            e.printStackTrace()
        }
    }

    override fun version(): AndroidVersion = AndroidVersion.ANDROID_10

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

    override fun loadThumbnail(image: Image): Bitmap {
        return ThumbnailUtils.createImageThumbnail(
            File(image.uri.path!!),
            Size(200, 300),
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
        val file = if (storage == Storage.INTERNAL) {
            generateInternalFileToSave(name).apply {
                val path = bitmapExif.bitmap?.saveTo(this)

                Exif.saveExif(bitmapExif.exifInterface, path)
            }
        } else {
            saveToExternalStorage(bitmapExif, name)
        }

        when (storage) {
            Storage.INTERNAL -> {
                notifySavedInternalImage(file, storage)
            }

            Storage.EXTERNAL -> {
                notifySavedExternalImage(file)
            }
        }
    }

    private fun saveToExternalStorage(bitmapExif: BitmapExif, name: String): File {
        val relativeLocation =
            Environment.DIRECTORY_PICTURES + File.separator + EXTERNAL_DIRECTORY_APP_PATH

        val fileName = generateFileName(name)

        val contentValues = ContentValues()
        contentValues.put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            fileName,
        )
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg") // Content-Type
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

        val resolver = context.contentResolver

        val uri = uriToUriFile(
            uri = bitmapExif.saveTo(
                resolver = resolver,
                contentValues = contentValues,
            ),
        )

        return File(uri!!.path!!) .apply {
            track("path=${this.path}")
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        track(uri)
        return MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri,
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