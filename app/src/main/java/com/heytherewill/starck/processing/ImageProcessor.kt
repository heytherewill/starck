package com.heytherewill.starck.processing

import android.graphics.Bitmap

class ImageProcessor {

    init {
        System.loadLibrary("imageprocessor-lib")
    }

    fun stackBitmaps(images: Array<Bitmap>): Bitmap =
        images.first().run {
            val stackedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            stackBitmaps(stackedImage, images)
            stackedImage
        }

    private external fun stackBitmaps(stack: Bitmap, bitmaps: Array<Bitmap>)
}