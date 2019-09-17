package com.heytherewill.starck.camera

import android.util.Range
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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
    private val numberOfPicturesList = (2..50).toList()
    private val timeList = listOf(0, 1, 3, 5, 10)

    private val aperture = MutableLiveData<Float>()
    private val shutterSpeed = MutableLiveData<Long>()
    private val sensorSensitivity = MutableLiveData<Int>()

    val cameraConfiguration: LiveData<CameraConfiguration> by lazy {
        val mediatorLiveData = MediatorLiveData<CameraConfiguration>()

        mediatorLiveData.value = CameraConfiguration.empty

        mediatorLiveData.addSource(aperture) {
            mediatorLiveData.value = mediatorLiveData.value?.copy(aperture = it)
        }
        mediatorLiveData.addSource(shutterSpeed) {
            mediatorLiveData.value = mediatorLiveData.value?.copy(shutterSpeed = it)
        }
        mediatorLiveData.addSource(sensorSensitivity) {
            mediatorLiveData.value = mediatorLiveData.value?.copy(sensorSensitivity = it)
        }

        mediatorLiveData
    }

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

        val currentSensorSensitivity = sensorSensitivity.value ?: -1
        if (!validSensorSensitivities.contains(currentSensorSensitivity)) {
            sensorSensitivity.value = validSensorSensitivities.min()
        }
    }

    fun setShutterSpeedRange(shutterSpeeds: Range<Long>) {

        val validShutterSpeeds = shutterSpeedList.filter(shutterSpeeds::contains)
        _validShutterSpeeds.value = validShutterSpeeds

        if (validShutterSpeeds.isEmpty())
            return

        val currentShutterSpeed = shutterSpeed.value ?: -1
        if (!validShutterSpeeds.contains(currentShutterSpeed)) {
            shutterSpeed.value = validShutterSpeeds.min()
        }
    }

    fun setApertureRange(apertures: FloatArray) {
        _validApertures.value = apertures.toList()

        if (apertures.isEmpty())
            return

        val currentAperture = aperture.value ?: -1F
        if (!apertures.contains(currentAperture)) {
            aperture.value = apertures.min()
        }
    }

    fun setSensorSensitivity(sensorSensitivity: Int) {
        val validSensitivities = _validSensorSensitivities.value ?: return
        if (!validSensitivities.contains(sensorSensitivity))
            return

        this.sensorSensitivity.value = sensorSensitivity
    }

    fun setShutterSpeed(shutterSpeed: Long) {
        if (!shutterSpeedList.contains(shutterSpeed))
            return

        this.shutterSpeed.value = shutterSpeed
    }

    fun setAperture(aperture: Float) {
        val validApertures = _validApertures.value ?: return
        if (!validApertures.contains(aperture))
            return

        this.aperture.value = aperture
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

    data class CameraConfiguration(
        val aperture: Float,
        val shutterSpeed: Long,
        val sensorSensitivity: Int
    ) {
        companion object {
            val empty = CameraConfiguration(0F, 0, 0)
        }
    }
}