package com.heytherewill.starck.main

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Range
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.heytherewill.starck.extensions.checkCameraPermission
import com.heytherewill.starck.extensions.requestCameraPermission
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.MediaStore.Images
import com.heytherewill.starck.R


class MainActivity : AppCompatActivity(), CameraController.CameraControllerListener {

    private lateinit var viewModel: MainViewModel
    private lateinit var cameraController: CameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        cameraController = CameraController(this, this, cameraPreview)

        viewModel.aperture.observe(this, Observer(cameraController::setAperture))
        viewModel.shutterSpeed.observe(this, Observer(cameraController::setShutterSpeed))
        viewModel.sensorSensitivity.observe(this, Observer(cameraController::setSensorSensitivity))

        cameraShutter.setOnClickListener { cameraController.captureImage() }
        arrow.setOnClickListener { CameraOptionsBottomSheetFragment().show(supportFragmentManager, "BottomSheet") }
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
        if (!checkCameraPermission()) {
            requestCameraPermission()
            return
        }

        cameraController.openCamera()
    }

    override fun onImageTaken(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        Images.Media.insertImage(
            contentResolver,
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
