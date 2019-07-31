package com.heytherewill.starck.main

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.checkCameraPermission
import com.heytherewill.starck.extensions.requestCameraPermission
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var cameraController: CameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraController = CameraController(this, cameraPreview, this::onImageTaken)

        cameraShutter.setOnClickListener {
            cameraController.startCapture()
        }
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

    private fun onImageTaken(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmapImage,
            "Image Stack",
            "Created with Starck"
        )
    }
}
