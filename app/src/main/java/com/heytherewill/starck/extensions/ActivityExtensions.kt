package com.heytherewill.starck.extensions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

const val cameraRequestCode = 100

fun Activity.checkCameraPermission(): Boolean =
    ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

fun Activity.requestCameraPermission() {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequestCode)
}