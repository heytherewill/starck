package com.heytherewill.starck.main

import android.util.Range
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.heytherewill.starck.extensions.nanosToSeconds

class MainViewModel : ViewModel() {

    private val isoList = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400, 10000)
    private val shutterSpeedList = (1L..10L).toList()
    private val numberOfPicturesList = (2..8).toList()
    private val timeList = listOf(0, 3, 5, 10)

    private val _sensorSensitivity = MutableLiveData<Int>()
    val sensorSensitivity: LiveData<Int> get () = _sensorSensitivity

    private val _shutterSpeed = MutableLiveData<Long>()
    val shutterSpeed: LiveData<Long> get () = _shutterSpeed

    private val _aperture = MutableLiveData<Float>()
    val aperture: LiveData<Float> get () = _aperture

    private val _numberOfPictures = MutableLiveData<Int>().apply { value = 2 }
    val numberOfPictures: LiveData<Int> get () = _numberOfPictures

    private val _timerDelay = MutableLiveData<Int>().apply { value = 0 }
    val timerDelay: LiveData<Int> get () = _timerDelay

    private val _validSensorSensitivities = MutableLiveData<List<Int>>()
    val validSensorSensitivities: LiveData<List<Int>> get () =  _validSensorSensitivities
    private val _validApertures = MutableLiveData<List<Float>>()
    val validApertures: LiveData<List<Float>> get () = _validApertures
    private val _validShutterSpeeds= MutableLiveData<List<Long>>()
    val validShutterSpeeds: LiveData<List<Long>> get () =  _validShutterSpeeds
    val validNumberOfPictures : LiveData<List<Int>> by lazy { MutableLiveData<List<Int>>().apply { value = numberOfPicturesList } }
    val validTimerDelays : LiveData<List<Int>> by lazy { MutableLiveData<List<Int>>().apply { value = timeList } }

    fun setSensorSensitivityRange(range: Range<Int>) {
        val validIsos = isoList.filter(range::contains)
        _validSensorSensitivities.value = validIsos

        val currentIso = _sensorSensitivity.value ?: return
        if (!validIsos.contains(currentIso)) {
            _sensorSensitivity.value = range.lower
        }
    }

    fun setShutterSpeedRange(rangeInNanoSeconds: Range<Long>) {
        val maxTimeInSeconds = rangeInNanoSeconds.upper.nanosToSeconds()
        val rangeInSeconds = Range(1L, maxTimeInSeconds)

        val validShutterSpeeds = shutterSpeedList.filter(rangeInSeconds::contains)
        _validShutterSpeeds.value = validShutterSpeeds

        val currentShutterSpeed = _shutterSpeed.value ?: return
        if (!validShutterSpeeds.contains(currentShutterSpeed)) {
            _shutterSpeed.value = rangeInSeconds.lower
        }
    }

    fun setApertureRange(apertures: FloatArray) {
        _validApertures.value = apertures.toList()

        if (apertures.size == 1) {
            _aperture.value = apertures.first()
        }
    }

    fun setSensorSensitivity(iso: Int) {
        val validSensitivities = _validSensorSensitivities.value ?: return
        if (!validSensitivities.contains(iso))
            return

        _sensorSensitivity.value = iso
    }

    fun setShutterSpeed(shutterSpeed: Long) {
        if (!shutterSpeedList.contains(shutterSpeed))
            return

        _shutterSpeed.value = shutterSpeed
    }

    fun setAperture(aperture: Float) {
        val validApertures = _validApertures.value ?: return
        if (!validApertures .contains(aperture))
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