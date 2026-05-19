package com.oddjobs.app.framestream

import android.content.Context
import android.graphics.BitmapFactory
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraXFrameCaptureEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    suspend fun start(config: FrameStreamConfig) {
        val provider = context.awaitCameraProvider()
        cameraProvider = provider

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(config.quality.jpegQuality())
            .build()

        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            capture
        )
        camera?.cameraControl?.enableTorch(config.torchEnabled)
        imageCapture = capture
    }

    suspend fun capture(config: FrameStreamConfig): FramePayload {
        val capture = imageCapture ?: throw IllegalStateException("ImageCapture is not bound")
        camera?.cameraControl?.enableTorch(config.torchEnabled)

        val directory = File(context.filesDir, "frame-stream").apply { mkdirs() }
        val outputFile = File(directory, "frame-${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        val result = suspendCancellableCoroutine<ImageCapture.OutputFileResults> { continuation ->
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(outputFileResults)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(outputFile.absolutePath, bounds)

        return FramePayload(
            file = outputFile,
            width = bounds.outWidth.coerceAtLeast(0),
            height = bounds.outHeight.coerceAtLeast(0),
            fileSizeBytes = outputFile.length(),
            description = result.savedUri?.toString() ?: outputFile.name
        )
    }

    fun stop() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
    }
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(this)
    return suspendCancellableCoroutine { continuation ->
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
}

private fun QualityMode.jpegQuality(): Int = when (this) {
    QualityMode.Balanced -> 80
    QualityMode.High -> 90
    QualityMode.Max -> 95
}
