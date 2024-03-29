package com.heytherewill.starck.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.map
import com.heytherewill.starck.extensions.nanosToSeconds
import com.heytherewill.starck.extensions.onProgressChangedListener
import kotlinx.android.synthetic.main.fragment_camera_options_bottom_sheet.*

class CameraSettingsFragment : Fragment() {

    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_camera_options_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(requireActivity()).get(CameraViewModel::class.java)

        val sensorSensitivityUiComponents =
            CameraOptionUiComponents(
                sensorSensitivityIcon,
                sensorSensitivitySeekBar,
                sensorSensitivityText
            )
        val apertureUiComponents =
            CameraOptionUiComponents(apertureIcon, apertureSeekBar, apertureText)
        val shutterSpeedUiComponents =
            CameraOptionUiComponents(shutterSpeedIcon, shutterSpeedSeekBar, shutterSpeedText)
        val numberOfPicturesUiComponents =
            CameraOptionUiComponents(
                numberOfPicturesIcon,
                numberOfPictureSeekBar,
                numberOfPicturesText
            )
        val timerUiComponents =
            CameraOptionUiComponents(timerDelayIcon, timerDelaySeekBar, timerDelayText)



        viewModel.validSensorSensitivities.observe(
            this,
            valueListObserver(
                sensorSensitivityUiComponents,
                viewModel.cameraConfiguration.map { it.sensorSensitivity }.value,
                viewModel::setSensorSensitivity
            )
        )

        viewModel.validShutterSpeeds.observe(
            this,
            valueListObserver(
                shutterSpeedUiComponents,
                viewModel.cameraConfiguration.map { it.shutterSpeed }.value,
                viewModel::setShutterSpeed
            )
        )

        viewModel.validApertures.observe(
            this,
            valueListObserver(
                apertureUiComponents,
                viewModel.cameraConfiguration.map { it.aperture }.value,
                viewModel::setAperture
            )
        )

        viewModel.validNumberOfPictures.observe(
            this,
            valueListObserver(
                numberOfPicturesUiComponents,
                viewModel.numberOfPictures.value,
                viewModel::setNumberOfPictures
            )
        )

        viewModel.validTimerDelays.observe(
            this,
            valueListObserver(
                timerUiComponents,
                viewModel.timerDelay.value,
                viewModel::setTimerDelay
            )
        )

        viewModel.cameraConfiguration.observe(
            this,
            Observer {
                apertureText.text = getString(R.string.f_number, it.aperture)
                sensorSensitivityText.text = it.sensorSensitivity.toString()
                shutterSpeedText.text =
                    getString(R.string.formatted_seconds, it.shutterSpeed.nanosToSeconds())
            }
        )

        viewModel.numberOfPictures.observe(
            this,
            Observer { numberOfPicturesText.text = it.toString() })

        viewModel.timerDelay.observe(this, Observer { timer ->
            timerDelayText.text =
                if (timer == 0) getString(R.string.off) else getString(
                    R.string.formatted_integer_seconds,
                    timer
                )
        })
    }

    private fun <T> valueListObserver(
        uiComponents: CameraOptionUiComponents,
        initialValue: T?,
        progressChangedCallback: (T) -> Unit
    ): Observer<List<T>> =
        Observer { validValues ->

            if (validValues.size <= 1) {
                uiComponents.hideAll()
                return@Observer
            }

            uiComponents.showAll()
            uiComponents.seekBar.max = validValues.size - 1
            uiComponents.seekBar.progress = initialValue?.let(validValues::indexOf) ?: 0
            uiComponents.seekBar.onProgressChangedListener { selectedIndex ->
                val value = validValues[selectedIndex]
                progressChangedCallback(value)
            }
        }

    private data class CameraOptionUiComponents(
        val icon: ImageView,
        val seekBar: SeekBar,
        val infoDisplay: TextView
    )

    private fun CameraOptionUiComponents.hideAll() {
        icon.isVisible = false
        seekBar.isVisible = false
        infoDisplay.isVisible = false
    }

    private fun CameraOptionUiComponents.showAll() {
        icon.isVisible = true
        seekBar.isVisible = true
        infoDisplay.isVisible = true
    }
}
