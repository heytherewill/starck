package com.heytherewill.starck.extensions

import android.util.SparseIntArray
import android.view.Display
import android.view.Surface


private val orientations = lazy {
    val orientations = SparseIntArray()
    orientations.append(Surface.ROTATION_0, 90)
    orientations.append(Surface.ROTATION_90, 0)
    orientations.append(Surface.ROTATION_180, 270)
    orientations.append(Surface.ROTATION_270, 180)
    orientations
}

val Display.deviceOrientation: Int
    get() =
        orientations.value.get(rotation)