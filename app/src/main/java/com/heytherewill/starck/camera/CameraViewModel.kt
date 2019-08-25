package com.heytherewill.starck.camera

import android.util.Range
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    private val sensorSensitivities = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400, 10000)
    private val shutterSpeedList =
        listOf(
            500000000,
            1000000000,
            1500000000,
            2000000000,
            2500000000,
            3000000000,
            3500000000,
            4000000000
        )
    private val numberOfPicturesList = (2..8).toList()
    private val timeList = listOf(0, 1, 3, 5, 10)

    private val _sensorSensitivity = MutableLiveData<Int>()
    val sensorSensitivity: LiveData<Int> get() = _sensorSensitivity

    private val _shutterSpeed = MutableLiveData<Long>()
    val shutterSpeed: LiveData<Long> get() = _shutterSpeed

    private val _aperture = MutableLiveData<Float>()
    val aperture: LiveData<Float> get() = _aperture

    private val _numberOfPictures = MutableLiveData<Int>().apply { value = 2 }
    val numberOfPictures: LiveData<Int> get() = _numberOfPictures

    private val _timerDelay = MutableLiveData<Int>().apply { value = 0 }
    val timerDelay: LiveData<Int> get() = _timerDelay

    private val _validSensorSensitivities = MutableLiveData<List<Int>>()
    val validSensorSensitivities: LiveData<List<Int>> get() = _validSensorSensitivities

    private val _validApertures = MutableLiveData<List<Float>>()
    val validApertures: LiveData<List<Float>> get() = _validApertures

    private val _validShutterSpeeds = MutableLiveData<List<Long>>()
    val validShutterSpeeds: LiveData<List<Long>> get() = _validShutterSpeeds

    val validNumberOfPictures: LiveData<List<Int>> = MutableLiveData<List<Int>>()
        .apply { value = numberOfPicturesList }

    val validTimerDelays: LiveData<List<Int>> = MutableLiveData<List<Int>>()
        .apply { value = timeList }

    fun setSensorSensitivityRange(range: Range<Int>) {
        val validSensorSensitivities = sensorSensitivities.filter(range::contains)
        _validSensorSensitivities.value = validSensorSensitivities

        if (validSensorSensitivities.isEmpty())
            return

        val currentSensorSensitivity = _sensorSensitivity.value ?: -1
        if (!validSensorSensitivities.contains(currentSensorSensitivity)) {
            _sensorSensitivity.value = validSensorSensitivities.min()
        }
    }

    fun setShutterSpeedRange(shutterSpeeds: Range<Long>) {

        val validShutterSpeeds = shutterSpeedList.filter(shutterSpeeds::contains)
        _validShutterSpeeds.value = validShutterSpeeds

        if (validShutterSpeeds.isEmpty())
            return

        val currentShutterSpeed = _shutterSpeed.value ?: -1
        if (!validShutterSpeeds.contains(currentShutterSpeed)) {
            _shutterSpeed.value = validShutterSpeeds.min()
        }
    }

    fun setApertureRange(apertures: FloatArray) {
        _validApertures.value = apertures.toList()

        if (apertures.isEmpty())
            return

        val currentAperture = _aperture.value ?: -1F
        if (!apertures.contains(currentAperture)) {
            _aperture.value = apertures.min()
        }
    }

    fun setSensorSensitivity(sensorSensitivity: Int) {
        val validSensitivities = _validSensorSensitivities.value ?: return
        if (!validSensitivities.contains(sensorSensitivity))
            return

        _sensorSensitivity.value = sensorSensitivity
    }

    fun setShutterSpeed(shutterSpeed: Long) {
        if (!shutterSpeedList.contains(shutterSpeed))
            return

        _shutterSpeed.value = shutterSpeed
    }

    fun setAperture(aperture: Float) {
        val validApertures = _validApertures.value ?: return
        if (!validApertures.contains(aperture))
            return

        _aperture.value = aperture

    }

    fun setNumberOfPictures(numberOfPictures: Int) {
        if (!numberOfPicturesList.contains(numberOfPictures))
            return

        _numberOfPictures.value = numberOfPictures
    }

    fun setTimerDelay(timer: Int) {
        if (!timeList.contains(timer))
            return

        _timerDelay.value = timer
    }
}