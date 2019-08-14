package com.heytherewill.starck.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView

class CameraPreviewTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    fun setupListener(
        onSurfaceTextureAvailable: () -> Unit,
        onSurfaceTextureSizeChanged: (width: Int, height: Int) -> Unit
    ) {

        surfaceTextureListener = CameraPreviewTextureListener(onSurfaceTextureAvailable, onSurfaceTextureSizeChanged)

    }

    fun setAspectRatio(width: Int, height: Int) {
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width > height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }

    private class CameraPreviewTextureListener(
        private val onSurfaceTextureAvailable: () -> Unit,
        private val onSurfaceTextureSizeChanged: (width: Int, height: Int) -> Unit
    ) : SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            onSurfaceTextureAvailable()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            onSurfaceTextureSizeChanged(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }
}
