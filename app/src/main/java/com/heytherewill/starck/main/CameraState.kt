package com.heytherewill.starck.main

sealed class CameraState {
    object Preview : CameraState()
    object WaitingLock : CameraState()
    object WaitingPreCapture : CameraState()
    object WaitingNonPreCapture : CameraState()
    object PictureTaken : CameraState()
}