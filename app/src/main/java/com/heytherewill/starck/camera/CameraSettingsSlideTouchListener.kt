package com.heytherewill.starck.camera

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.heytherewill.starck.extensions.clamp

class SlideGestureListener(
    private val view: View,
    private val minMargin: Int,
    private val maxMargin: Lazy<Int>
) : GestureDetector.SimpleOnGestureListener() {

    private var currentDirection: Direction = Direction.None

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {

        val flingDirection = if (velocityY < 0) Direction.Up else Direction.Down

        if (flingDirection != currentDirection)
            return super.onFling(e1, e2, velocityX, velocityY)

        view.layoutParams = view.layoutParams
            .let { it as ViewGroup.MarginLayoutParams }
            .apply {
                bottomMargin = if (flingDirection == Direction.Up) maxMargin.value else minMargin
            }

        return super.onFling(e1, e2, velocityX, velocityY)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {

        val firstY = e1?.y ?: return false
        val secondY = e2?.y ?: return false

        val deltaY = firstY.minus(secondY).toInt()

        currentDirection = when {
            deltaY > 0 -> Direction.Up
            deltaY < 0 -> Direction.Down
            else -> currentDirection
        }

        view.layoutParams = view.layoutParams
            .let { it as ViewGroup.MarginLayoutParams }
            .apply { bottomMargin = bottomMargin.plus(deltaY).clamp(minMargin, maxMargin.value) }

        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    private sealed class Direction {
        object None : Direction()
        object Up : Direction()
        object Down : Direction()
    }
}

fun View.setupSlidingTouchListener(minMargin: Int, maxMargin: Lazy<Int>) {
    val gestureListener = SlideGestureListener(this, minMargin, maxMargin)
    val gestureRecognizer = GestureDetector(context, gestureListener)

    setOnTouchListener { v, event ->
        gestureRecognizer.onTouchEvent(event)
        true
    }
}
