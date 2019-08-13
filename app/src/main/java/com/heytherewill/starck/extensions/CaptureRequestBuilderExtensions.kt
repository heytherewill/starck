package com.heytherewill.starck.extensions

import android.hardware.camera2.CaptureRequest

fun CaptureRequest.Builder.setFocalDistanceToInfinity() {
    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
    set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f)
}

fun CaptureRequest.Builder.setShutterSpeed(shutterSpeed: Long) {
    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
    set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeed)
}

fun CaptureRequest.Builder.setSensorSensitivity(sensitivity: Int) {
    set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity)
}

fun CaptureRequest.Builder.setAperture(aperture: Float) {
    set(CaptureRequest.LENS_APERTURE, aperture)
}