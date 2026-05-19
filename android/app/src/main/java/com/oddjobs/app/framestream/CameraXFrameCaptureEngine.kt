package com.oddjobs.app.framestream

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.ExifInterface
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraXFrameCaptureEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    suspend fun start(config: FrameStreamConfig) {
        val provider = context.awaitCameraProvider()
        withContext(Dispatchers.Main.immediate) {
            releaseBindingsOnMainThread()
            cameraProvider = provider

            val previewUseCase = Preview.Builder().build()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(config.quality.jpegQuality())
                .build()

            val texture = SurfaceTexture(0).apply {
                setDefaultBufferSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
            }
            val previewSurface = Surface(texture)

            previewUseCase.setSurfaceProvider { request ->
                request.provideSurface(
                    previewSurface,
                    ContextCompat.getMainExecutor(context)
                ) {
                }
            }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                capture
            )
            imageCapture = capture
            surfaceTexture = texture
            surface = previewSurface
        }
    }

    suspend fun capture(config: FrameStreamConfig): FramePayload {
        val capture = imageCapture ?: throw IllegalStateException("ImageCapture is not bound")
        val outputFile = prepareOutputFile()
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

        optimizeOutputFile(outputFile, config)

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
        val clearBindings = {
            releaseBindingsOnMainThread()
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            clearBindings()
        } else {
            Handler(Looper.getMainLooper()).post(clearBindings)
        }
    }

    private fun prepareOutputFile(): File {
        val directory = File(context.filesDir, "frame-stream").apply { mkdirs() }
        directory.listFiles()?.forEach { existingFile ->
            if (existingFile.isFile) {
                existingFile.delete()
            }
        }
        return File(directory, "frame-latest.jpg")
    }

    private fun releaseBindingsOnMainThread() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        cameraProvider = null
        surface?.release()
        surfaceTexture?.release()
        surface = null
        surfaceTexture = null
    }

    private fun optimizeOutputFile(outputFile: File, config: FrameStreamConfig?) {
        val qualityMode = config?.quality ?: QualityMode.High
        val profile = qualityMode.compressionProfile()
        val originalBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(outputFile.absolutePath, originalBounds)

        val originalWidth = originalBounds.outWidth
        val originalHeight = originalBounds.outHeight
        if (originalWidth <= 0 || originalHeight <= 0) {
            return
        }

        val originalOrientation = ExifInterface(outputFile.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationDegrees = originalOrientation.toRotationDegrees()
        val isQuarterTurn = rotationDegrees == 90f || rotationDegrees == 270f
        val rotatedWidth = if (isQuarterTurn) originalHeight else originalWidth
        val rotatedHeight = if (isQuarterTurn) originalWidth else originalHeight
        val targetSize = scaledDimensions(rotatedWidth, rotatedHeight, profile.maxLongEdge)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(rotatedWidth, rotatedHeight, targetSize.first, targetSize.second)
        }
        val decodedBitmap = BitmapFactory.decodeFile(outputFile.absolutePath, decodeOptions) ?: return
        val rotatedBitmap = decodedBitmap.rotateIfNeeded(rotationDegrees)
        if (rotatedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        val finalBitmap = if (rotatedBitmap.width == targetSize.first && rotatedBitmap.height == targetSize.second) {
            rotatedBitmap
        } else {
            val scaled = Bitmap.createScaledBitmap(
                rotatedBitmap,
                targetSize.first,
                targetSize.second,
                true
            )
            if (scaled !== rotatedBitmap) {
                rotatedBitmap.recycle()
            }
            scaled
        }

        val tempFile = File(outputFile.parentFile, "frame-latest-compressed.jpg")
        FileOutputStream(tempFile).use { outputStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, profile.jpegQuality, outputStream)
        }
        finalBitmap.recycle()

        if (!outputFile.delete()) {
            tempFile.delete()
            return
        }
        if (!tempFile.renameTo(outputFile)) {
            tempFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
        }
    }
}

private const val PREVIEW_WIDTH = 1280
private const val PREVIEW_HEIGHT = 720

private data class CompressionProfile(
    val maxLongEdge: Int,
    val jpegQuality: Int
)

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

private fun QualityMode.jpegQuality(): Int = compressionProfile().jpegQuality

private fun QualityMode.compressionProfile(): CompressionProfile = when (this) {
    QualityMode.Balanced -> CompressionProfile(maxLongEdge = 1280, jpegQuality = 74)
    QualityMode.High -> CompressionProfile(maxLongEdge = 1600, jpegQuality = 80)
    QualityMode.Max -> CompressionProfile(maxLongEdge = 1920, jpegQuality = 86)
}

private fun scaledDimensions(width: Int, height: Int, maxLongEdge: Int): Pair<Int, Int> {
    val currentLongEdge = maxOf(width, height)
    if (currentLongEdge <= maxLongEdge) {
        return width to height
    }

    val scale = maxLongEdge.toFloat() / currentLongEdge.toFloat()
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return scaledWidth to scaledHeight
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int
): Int {
    var inSampleSize = 1
    if (height > targetHeight || width > targetWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun Int.toRotationDegrees(): Float = when (this) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
}

private fun Bitmap.rotateIfNeeded(rotationDegrees: Float): Bitmap {
    if (rotationDegrees == 0f) {
        return this
    }

    val matrix = Matrix().apply { postRotate(rotationDegrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
