package com.heytherewill.starck.main

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.heytherewill.starck.extensions.*
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private const val maxPreviewWidth = 1920
private const val maxPreviewHeight = 1080

class CameraController(
    private val activity: Activity,
    private val textureView: CameraPreviewTextureView,
    private val onImageTaken: (Image) -> Unit
) {
    private var state: CameraState = CameraState.Preview

    private lateinit var cameraId: String
    private var sensorOrientation = 0
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val cameraOpenCloseLock = Semaphore(1)

    private lateinit var previewSize: Size
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imageReader: ImageReader? = null
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        backgroundHandler?.post {
            val image = it.acquireNextImage()
            onImageTaken(image)
            image.close()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraController.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraController.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (state) {
                is CameraState.Preview -> Unit
                is CameraState.WaitingLock -> capturePicture(result)
                is CameraState.WaitingPreCapture -> {
                    if (result.autoExposureIsInPreCaptureState) {
                        state = CameraState.WaitingNonPreCapture
                    }
                }
                is CameraState.WaitingNonPreCapture -> {
                    if (result.autoExposureIsInCaptureState) {
                        state = CameraState.PictureTaken
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
                return
            }

            if (afState != CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED && afState != CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)
                return

            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
            if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                state = CameraState.PictureTaken
                captureStillPicture()
            } else {
                runPrecaptureSequence()
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {

            cameraId = manager.cameraIdList.firstOrNull {
                val characteristics = manager.getCameraCharacteristics(it)
                !characteristics.lensAreFrontFacing
            } ?: return

            val characteristics = manager.getCameraCharacteristics(cameraId)

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

            val largest = map.largestOutputSize
            imageReader = ImageReader
                .newInstance(largest.width, largest.height, ImageFormat.JPEG, 2)
                .apply { setOnImageAvailableListener(onImageAvailableListener, backgroundHandler) }

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            val displayRotation = activity.windowManager.defaultDisplay.rotation

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val swappedDimensions = areDimensionsSwapped(displayRotation)

            val displaySize = Point()
            activity.windowManager.defaultDisplay.getSize(displaySize)
            val rotatedPreviewWidth = if (swappedDimensions) height else width
            val rotatedPreviewHeight = if (swappedDimensions) width else height
            val maxPreviewWidth = (if (swappedDimensions) displaySize.y else displaySize.x).clamp(0, maxPreviewWidth)
            val maxPreviewHeight =
                (if (swappedDimensions) displaySize.x else displaySize.y).clamp(0, maxPreviewHeight)

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            previewSize = map.chooseOptimalPreviewSize(
                rotatedPreviewWidth, rotatedPreviewHeight,
                maxPreviewWidth, maxPreviewHeight,
                largest
            )

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.width, previewSize.height)
            } else {
                textureView.setAspectRatio(previewSize.height, previewSize.width)
            }

        } catch (e: CameraAccessException) {
            Log.e(tag, e.toString())
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(tag, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    @RequiresPermission(android.Manifest.permission.CAMERA)
    fun openCamera() {
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startBackgroundThread()
        setUpCameraOutputs(textureView.width, textureView.height)
        configureTransform(textureView.width, textureView.height)
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
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
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
            stopBackgroundThread()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(tag, e.toString())
        }

    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(
                Arrays.asList(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        val camera = cameraDevice ?: return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {

                            previewRequestBuilder.setFocalDistanceToInfinity()

                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
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

    fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
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

    fun startCapture() {
        try {
            previewRequestBuilder.lockFocus()

            state = CameraState.WaitingLock
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(tag, e.toString())
        }

    }

    private fun runPrecaptureSequence() {
        try {
            previewRequestBuilder.trigger()

            state = CameraState.WaitingPreCapture
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(tag, e.toString())
        }

    }

    private fun captureStillPicture() {
        try {
            val camera = cameraDevice ?: return
            val surface = imageReader?.surface?: return

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                .apply {
                    addTarget(surface)

                    // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                    // We have to take that into account and rotate JPEG properly.
                    // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                    // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                    val jpegOrientation =
                        (activity.windowManager.defaultDisplay.deviceOrientation + sensorOrientation + 270) % 360

                    set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)

                    // Use the same AE and AF modes as the preview.
                    setFocalDistanceToInfinity()
                }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder.build(), captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(tag, e.toString())
        }
    }

    private fun unlockFocus() {
        try {
            previewRequestBuilder.unlockFocus()
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)

            // After this, the camera will go back to the normal state of preview.
            state = CameraState.Preview
            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(tag, e.toString())
        }
    }

    companion object {
        const val tag = "CameraController"
    }
}