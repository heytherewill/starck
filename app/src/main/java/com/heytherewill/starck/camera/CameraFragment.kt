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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.checkCameraPermission
import com.heytherewill.starck.extensions.requestCameraPermission
import kotlinx.android.synthetic.main.fragment_camera.*

class CameraFragment : Fragment(), CameraController.CameraControllerListener {

    private lateinit var viewModel: CameraViewModel
    private lateinit var cameraController: CameraController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        viewModel = ViewModelProviders.of(activity).get(CameraViewModel::class.java)
        cameraController = CameraController(activity, this, cameraPreview)

        viewModel.aperture.observe(this, Observer(cameraController::setAperture))
        viewModel.shutterSpeed.observe(this, Observer(cameraController::setShutterSpeed))
        viewModel.sensorSensitivity.observe(this, Observer(cameraController::setSensorSensitivity))

        cameraShutter.setOnClickListener { cameraController.captureImage() }
        arrow.setOnClickListener { CameraSettingsFragment()
            .show(requireActivity().supportFragmentManager, "BottomSheet") }
    }

    override fun onResume() {
        super.onResume()

        if (cameraPreview.isAvailable) {
            openCameraIfPossible()
        } else {
            cameraPreview.setupListener(this::openCameraIfPossible, cameraController::configureTransform)
        }
    }

    override fun onPause() {
        super.onPause()
        cameraController.closeCamera()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        cameraController.configureTransform(cameraPreview.measuredWidth, cameraPreview.measuredHeight)
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
        MediaStore.Images.Media.insertImage(
            activity.contentResolver,
            bitmapImage,
            "Image Stack",
            "Created with Starck"
        )
    }

    override fun onCameraCharacteristicsInitialized(sensitivityRange: Range<Int>, shutterSpeedRange: Range<Long>, validApertures: FloatArray) {
        viewModel.setShutterSpeedRange(shutterSpeedRange)
        viewModel.setSensorSensitivityRange(sensitivityRange)
        viewModel.setApertureRange(validApertures)
    }
}