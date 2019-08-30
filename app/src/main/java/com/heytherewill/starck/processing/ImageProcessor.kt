package com.heytherewill.starck.processing

import android.graphics.Bitmap

class ImageProcessor {

    init {
        System.loadLibrary("imageprocessor-lib")
    }

    fun stackBitmaps( images: Array<Bitmap>) : Bitmap {

        val stackedImage = images.first().run {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        stackBitmaps(stackedImage, images)

        return stackedImage
    }

    private external fun stackBitmaps(stack: Bitmap, bitmaps: Array<Bitmap>)
}