package com.heytherewill.starck.camera

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.*

class CameraFragment : Fragment() {

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
        cameraController = CameraController(cameraPreview)

        viewModel.cameraConfiguration.observe(this, Observer {
            GlobalScope.launch { cameraController.reloadPreview(it) }
        })

        cameraShutter.setOnClickListener {

            arrow.isEnabled = false
            cameraShutter.isEnabled = false
            captureInProgressOverlay.showWithCircularReveal(cameraShutter) {
                captureInProgressWarning.isVisible = true

                GlobalScope.launch {
                    val numberOfPictures = viewModel.numberOfPictures.value ?: 2

                    val imageUrls = mutableListOf<String>()

                    for (i in 1..numberOfPictures) {
                        val image =
                            withContext(Dispatchers.Default) { cameraController.takePicture() }
                                ?: return@launch

                        val imageUrl = saveImage(image)
                        image.close()
                        imageUrls.add(imageUrl)
                    }

                    cameraController.resumePreview()

                    val processingArgs = CameraFragmentDirections
                        .actionCameraFragmentToProcessingFragment(imageUrls.toTypedArray())

                    Navigation.findNavController(requireActivity(), R.id.fragmentContainer)
                        .navigate(processingArgs)
                }
            }
        }

        shutterControls.setupSlidingTouchListener(0, lazy {
            childFragmentManager.findFragmentById(R.id.cameraSettings)?.view?.height ?: 0
        })
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
                this::openCameraIfPossible
            ) { width: Int, height: Int ->
                cameraController.configureTransform(
                    requireActivity().windowManager.defaultDisplay.rotation, width, height
                )
            }
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
            requireActivity().windowManager.defaultDisplay.rotation,
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

        MainScope().launch {

            val openedCameraCharacteristics = cameraController.openCamera(requireActivity())
            if (openedCameraCharacteristics == null) {
                activity.finish()
                return@launch
            }

            viewModel.setApertureRange(openedCameraCharacteristics.validApertures)
            viewModel.setShutterSpeedRange(openedCameraCharacteristics.validShutterSpeeds)
            viewModel.setSensorSensitivityRange(openedCameraCharacteristics.validSensorSensitivities)
        }
    }

    private fun saveImage(image: Image): String {
        val activity = requireActivity()

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

        return MediaStore.Images.Media.insertImage(
            activity.contentResolver,
            bitmapImage,
            "Image Stack",
            "Created with Starck"
        )
    }
}
