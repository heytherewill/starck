package com.heytherewill.starck.camera

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.checkCameraPermission
import com.heytherewill.starck.extensions.requestCameraPermission
import com.heytherewill.starck.extensions.showWithCircularReveal
import com.heytherewill.starck.extensions.startImmersiveMode
import kotlinx.android.synthetic.main.fragment_camera.*

class CameraFragment : Fragment(), CameraController.CameraControllerListener {

    private lateinit var viewModel: CameraViewModel
    private lateinit var cameraController: CameraController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        viewModel = ViewModelProviders.of(activity).get(CameraViewModel::class.java)
        cameraController = CameraController(activity, this, cameraPreview)

        viewModel.aperture.observe(this, Observer { cameraController.aperture = it })
        viewModel.shutterSpeed.observe(this, Observer { cameraController.shutterSpeed = it })
        viewModel.sensorSensitivity.observe(
            this,
            Observer { cameraController.sensorSensitivity = it })
        viewModel.numberOfPictures.observe(
            this,
            Observer { cameraController.numberOfPictures = it })

        cameraShutter.setOnClickListener {

            arrow.isEnabled = false
            cameraShutter.isEnabled = false
            captureInProgressOverlay.showWithCircularReveal(cameraShutter) {
                captureInProgressWarning.isVisible = true
                cameraController.takePicture()
            }
        }

        val settingsViewHeight = lazy {
            childFragmentManager.findFragmentById(R.id.cameraSettings)?.view?.height ?: 0
        }
        shutterControls.setupSlidingTouchListener(0, settingsViewHeight)
    }

    override fun onResume() {
        super.onResume()

        arrow.isEnabled = true
        cameraShutter.isEnabled = true
        captureInProgressWarning.isVisible = false
        captureInProgressOverlay.isVisible = false

        if (cameraPreview.isAvailable) {
            openCameraIfPossible()
        } else {
            cameraPreview.setupListener(
                this::openCameraIfPossible,
                cameraController::configureTransform
            )
        }

        cameraShutter.startImmersiveMode()
    }

    override fun onPause() {
        super.onPause()
        cameraController.closeCamera()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        cameraController.configureTransform(
            cameraPreview.measuredWidth,
            cameraPreview.measuredHeight
        )
    }

    private fun openCameraIfPossible() {
        val activity = activity ?: return

        if (!activity.checkCameraPermission()) {
            activity.requestCameraPermission()
            return
        }

        cameraController.openCamera()
    }

    override fun onImageTaken(image: Image) {
        val activity = activity ?: return

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        val imageUrl = MediaStore.Images.Media.insertImage(
            activity.contentResolver,
            bitmapImage,
            "Image Stack",
            "Created with Starck"
        )

        sessionUrls.add(imageUrl)

        if (sessionUrls.size < viewModel.numberOfPictures.value ?: 2)
            return

        onCaptureFinished()
    }

    private fun onCaptureFinished() {

        val processingArgs = CameraFragmentDirections
            .actionCameraFragmentToProcessingFragment(sessionUrls.toTypedArray())

        sessionUrls.clear()

        Navigation.findNavController(requireActivity(), R.id.fragmentContainer)
            .navigate(processingArgs)
    }

    private val sessionUrls = mutableListOf<String>()

    override fun onCameraCharacteristicsInitialized(
        sensitivityRange: Range<Int>,
        shutterSpeedRange: Range<Long>,
        validApertures: FloatArray
    ) {
        viewModel.setShutterSpeedRange(shutterSpeedRange)
        viewModel.setSensorSensitivityRange(sensitivityRange)
        viewModel.setApertureRange(validApertures)
    }
}
