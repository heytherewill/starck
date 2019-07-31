package com.heytherewill.starck.extensions

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest

fun CaptureRequest.Builder.lockFocus() {
    set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
}

fun CaptureRequest.Builder.trigger() {
    set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
}

fun CaptureRequest.Builder.unlockFocus() {
    set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
}