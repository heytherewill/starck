package com.heytherewill.starck.camera

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.heytherewill.starck.extensions.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

private const val maxPreviewWidth = 1920
private const val maxPreviewHeight = 1080
private const val maxPreviewShutterSpeed = 70000000L

private const val tag = "CameraController"

class CameraController(private val textureView: CameraPreviewTextureView) {

    private var cameraId: String? = null
    private var sensorOrientation = 0
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val cameraOpenCloseLock = Semaphore(1)

    private lateinit var previewSize: Size
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var imageReader: ImageReader? = null
    private var cameraConfiguration = CameraViewModel.CameraConfiguration.empty

    private var cameraDispatcher: CoroutineDispatcher? = null
    private var cameraScope: CoroutineScope? = null
    private var cameraThread: HandlerThread? = null

    @RequiresPermission(android.Manifest.permission.CAMERA)
    suspend fun openCamera(activity: Activity): CameraCharacteristics? =
        suspendCoroutine { continuation ->
            val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            cameraThread = HandlerThread("CameraController").apply { start() }
            cameraDispatcher = Handler(cameraThread?.looper).asCoroutineDispatcher()
            cameraScope = cameraDispatcher?.run { CoroutineScope(this) }

            try {
                val cameraId = manager.cameraIdList.firstOrNull {
                    val characteristics = manager.getCameraCharacteristics(it)
                    !characteristics.lensAreFrontFacing
                }

                if (cameraId == null) {
                    continuation.resume(null)
                    return@suspendCoroutine
                }

                val characteristics = manager.getCameraCharacteristics(cameraId)
                val map = characteristics.streamConfigurationMap

                if (map == null) {
                    continuation.resume(null)
                    return@suspendCoroutine
                }

                val largest = map.largestOutputSize
                val swappedDimensions = when (activity.windowManager.defaultDisplay.rotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> sensorOrientation == 90 || sensorOrientation == 270
                    Surface.ROTATION_90, Surface.ROTATION_270 -> sensorOrientation == 0 || sensorOrientation == 180
                    else -> false
                }
                val rotatedPreviewWidth =
                    if (swappedDimensions) textureView.height else textureView.width
                val rotatedPreviewHeight =
                    if (swappedDimensions) textureView.width else textureView.height

                this.cameraId = cameraId
                this.sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                this.imageReader = ImageReader
                    .newInstance(largest.width, largest.height, ImageFormat.JPEG, 2)

                this.previewSize = activity.windowManager.defaultDisplay.size.run {
                    val maxPreviewWidth =
                        (if (swappedDimensions) y else x).clamp(0, maxPreviewWidth)
                    val maxPreviewHeight =
                        (if (swappedDimensions) x else y).clamp(0, maxPreviewHeight)

                    map.chooseOptimalPreviewSize(
                        rotatedPreviewWidth, rotatedPreviewHeight,
                        maxPreviewWidth, maxPreviewHeight,
                        largest
                    )
                }


                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.width, previewSize.height)
                } else {
                    textureView.setAspectRatio(previewSize.height, previewSize.width)
                }

                configureTransform(
                    activity.windowManager.defaultDisplay.rotation,
                    textureView.width,
                    textureView.height
                )

                // Wait for camera to open - 2.5 seconds is sufficient
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw RuntimeException("Time out waiting to lock camera opening.")
                }

                cameraScope?.launch {

                    val cameraDevice = manager.openCameraAsync(cameraId)

                    this@CameraController.cameraDevice = cameraDevice

                    if (cameraDevice == null) {
                        continuation.resume(null)
                    } else {
                        cameraDevice.createCameraPreviewSession()
                        continuation.resume(characteristics)
                    }

                    cameraOpenCloseLock.release()
                }

            } catch (e: CameraAccessException) {
                Log.e(tag, e.toString())
            } catch (e: InterruptedException) {
                throw RuntimeException("Interrupted while trying to lock camera opening.", e)
            }
        }

    fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            cameraThread?.quitSafely()
            cameraDispatcher = null
            cameraScope = null
            cameraThread = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    fun configureTransform(rotation: Int, viewWidth: Int, viewHeight: Int) {
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

        textureView.setTransform(matrix)
    }

    suspend fun takePicture(): Image? = suspendCoroutine { continuation ->

        val camera = cameraDevice
        val surface = imageReader?.surface
        val captureSession = captureSession

        if (camera == null || surface == null || captureSession == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        cameraScope?.launch {

            try {
                imageReader?.setOnImageAvailableListener({
                    val image = it.acquireNextImage()
                    continuation.resume(image)
                }, null)

                val captureBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                        addTarget(surface)
                        prepareRequestBuilder(false)
                    }

                captureSession.capture(
                    captureBuilder.build(),
                    object : CameraCaptureSession.CaptureCallback() {},
                    null
                )

            } catch (e: CameraAccessException) {
                Log.e(tag, e.toString())
            }
        }
    }

    fun reloadPreview(configuration: CameraViewModel.CameraConfiguration) =
        cameraScope?.launch {

            cameraConfiguration = configuration

            previewRequestBuilder?.apply {
                prepareRequestBuilder(true)
                captureSession?.setRepeatingRequest(build(), null, null)
            }
        }

    fun resumePreview() {
        val previewRequest = previewRequestBuilder?.build() ?: return
        cameraScope?.launch {
            try {
                captureSession?.setRepeatingRequest(previewRequest, null, null)
            } catch (e: CameraAccessException) {
                Log.e(tag, e.toString())
            }
        }
    }

    private fun CaptureRequest.Builder.prepareRequestBuilder(preparingForPreview: Boolean) {
        setFocalDistanceToInfinity()
        setAperture(cameraConfiguration.aperture)
        setSensorSensitivity(cameraConfiguration.sensorSensitivity)

        val maxValidShutterSpeed =
            if (preparingForPreview) maxPreviewShutterSpeed else cameraConfiguration.shutterSpeed
        val shutterSpeed = max(cameraConfiguration.shutterSpeed, maxValidShutterSpeed)
        setShutterSpeed(shutterSpeed)
    }

    private fun CameraDevice.createCameraPreviewSession() {

        try {
            val texture = textureView.surfaceTexture ?: return
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            val surface = Surface(texture)
            val previewRequestBuilder = createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            createCaptureSession(
                listOf(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {

                        captureSession = cameraCaptureSession
                        try {

                            previewRequestBuilder.prepareRequestBuilder(true)

                            val previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(
                                previewRequest,
                                null,
                                null
                            )

                            this@CameraController.previewRequestBuilder = previewRequestBuilder

                        } catch (e: CameraAccessException) {
                            Log.e(tag, e.toString())
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(tag, e.toString())
        }
    }

    @RequiresPermission(android.Manifest.permission.CAMERA)
    private suspend fun CameraManager.openCameraAsync(cameraId: String): CameraDevice? =
        suspendCoroutine { continuation ->
            this.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    continuation.resume(cameraDevice)
                }

                override fun onDisconnected(cameraDevice: CameraDevice) {
                    cameraDevice.close()
                    continuation.resume(null)
                }

                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    onDisconnected(cameraDevice)
                }
            }, null)
        }

    private val StreamConfigurationMap.largestOutputSize: Size
        get() = Collections.max(
            listOf(*this.getOutputSizes(ImageFormat.JPEG)),
            CompareSizesByArea()
        )

    private fun StreamConfigurationMap.chooseOptimalPreviewSize(
        textureViewWidth: Int,
        textureViewHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        aspectRatio: Size
    ): Size {

        val choices = getOutputSizes(SurfaceTexture::class.java)

        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()

        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                option.height == option.width * h / w
            ) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        return when {
            bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> {
                Log.e("CameraController", "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }

    private class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size) =
            signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)

    }
}