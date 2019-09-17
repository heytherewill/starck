package com.heytherewill.starck.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range

val CameraCharacteristics.lensAreFrontFacing: Boolean
    get() = this.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT

val CameraCharacteristics.validShutterSpeeds: Range<Long>
    get() = this.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) ?: Range(0L, 0L)

val CameraCharacteristics.validSensorSensitivities: Range<Int>
    get() = this.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) ?: Range(0, 0)

val CameraCharacteristics.validApertures: FloatArray
    get() = this.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: FloatArray(0)

val CameraCharacteristics.streamConfigurationMap: StreamConfigurationMap?
    get() = this.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)