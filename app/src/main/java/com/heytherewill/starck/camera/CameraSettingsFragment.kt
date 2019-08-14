package com.heytherewill.starck.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.onProgressChangedListener
import kotlinx.android.synthetic.main.fragment_camera_options_bottom_sheet.*

class CameraSettingsFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_camera_options_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(requireActivity()).get(CameraViewModel::class.java)

        val isoUiComponents = CameraOptionUiComponents(iso, isoSlider, isoDisplay)
        val apertureUiComponents = CameraOptionUiComponents(aperture, apertureSlider, apertureDisplay)
        val shutterSpeedUiComponents = CameraOptionUiComponents(shutterSpeed, shutterSpeedSlider, shutterSpeedDisplay)
        val numberOfPicturesUiComponents = CameraOptionUiComponents(photos, photosSlider, photosDisplay)
        val timerUiComponents = CameraOptionUiComponents(timer, timerSlider, timerDisplay)

        viewModel.validSensorSensitivities.observe(this,
            valueListObserver(isoUiComponents, viewModel.sensorSensitivity.value, viewModel::setSensorSensitivity))

        viewModel.validShutterSpeeds.observe(this,
            valueListObserver(shutterSpeedUiComponents, viewModel.shutterSpeed.value, viewModel::setShutterSpeed))

        viewModel.validApertures.observe(this,
            valueListObserver(apertureUiComponents, viewModel.aperture.value, viewModel::setAperture))

        viewModel.validNumberOfPictures.observe(this,
            valueListObserver(numberOfPicturesUiComponents, viewModel.numberOfPictures.value, viewModel::setNumberOfPictures))

        viewModel.validTimerDelays.observe(this,
            valueListObserver(timerUiComponents, viewModel.timerDelay.value, viewModel::setTimerDelay))

        viewModel.sensorSensitivity.observe(this, Observer { isoDisplay.text = it.toString() })
        viewModel.shutterSpeed.observe(this, Observer { shutterSpeedDisplay.text = getString(R.string.formatted_seconds, it) })
        viewModel.aperture.observe(this, Observer { apertureDisplay.text = getString(R.string.f_number, it)})
        viewModel.numberOfPictures.observe(this, Observer { photosDisplay.text = it.toString() })
        viewModel.timerDelay.observe(this, Observer { timer ->
            timerDisplay.text = if (timer == 0) getString(R.string.off) else  getString(R.string.formatted_seconds, timer)
        })
    }

    private fun <T> valueListObserver(
        uiComponents: CameraOptionUiComponents,
        initialValue: T?,
        progressChangedCallback: (T) -> Unit) : Observer<List<T>> =
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
