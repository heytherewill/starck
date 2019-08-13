package com.heytherewill.starck.main

sealed class CameraState {
    object Preview : CameraState()
    object WaitingLock : CameraState()
    object PictureTaken : CameraState()
}