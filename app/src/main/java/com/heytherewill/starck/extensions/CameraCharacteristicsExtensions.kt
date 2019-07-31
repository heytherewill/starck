package com.heytherewill.starck.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata

val CameraCharacteristics.lensAreFrontFacing: Boolean
    get() = this.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
