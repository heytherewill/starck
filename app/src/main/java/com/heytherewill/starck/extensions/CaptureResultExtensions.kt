package com.heytherewill.starck.extensions

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult

val CaptureResult.autoExposureIsInPreCaptureState: Boolean
    get() {
        val aeState = get(CaptureResult.CONTROL_AE_STATE)
        return aeState == null ||
                aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
    }

val CaptureResult.autoExposureIsInCaptureState: Boolean
    get() {
        val aeState = get(CaptureResult.CONTROL_AE_STATE)
        return aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE
    }