package com.heytherewill.starck.extensions

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Log
import android.util.Size
import com.heytherewill.starck.main.CameraController
import java.lang.Long
import java.util.*

val StreamConfigurationMap.largestOutputSize: Size
    get() = Collections.max(
        Arrays.asList(*this.getOutputSizes(ImageFormat.JPEG)),
        CompareSizesByArea()
    )


fun StreamConfigurationMap.chooseOptimalPreviewSize(
    textureViewWidth: Int,
    textureViewHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
    aspectRatio: Size
): Size {

    val choices = getOutputSizes(SurfaceTexture::class.java)

    val bigEnough = ArrayList<Size>()
    val notBigEnough = ArrayList<Size>()

    val w = aspectRatio.width
    val h = aspectRatio.height
    for (option in choices) {
        if (option.width <= maxWidth && option.height <= maxHeight &&
            option.height == option.width * h / w
        ) {
            if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }
    }

    // Pick the smallest of those big enough. If there is no one big enough, pick the
    // largest of those not big enough.
    return when {
        bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
        notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
        else -> {
            Log.e(CameraController.tag, "Couldn't find any suitable preview size")
            choices[0]
        }
    }
}

class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size) =
        Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)

}