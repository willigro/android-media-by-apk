package com.rittmann.core.camera

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import java.io.File
import java.util.concurrent.ExecutorService

class CameraHandler(private val executorService: ExecutorService) {
    fun takePhoto(
        file: File,
        imageCapture: ImageCapture,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            executorService,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(file)
                    onImageCaptured(savedUri)
                }
            }
        )
    }

    fun takePhoto(
        imageCapture: ImageCapture,
        onImageCaptured: (ImageProxy) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        imageCapture.takePicture(
            executorService,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    onImageCaptured(image)
                }
            }
        )
    }
}