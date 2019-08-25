package com.heytherewill.starck.extensions

import android.animation.Animator
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.view.isVisible
import kotlin.math.max


private const val immersiveFlagTimeout = 500L

fun View.startImmersiveMode() {
    postDelayed({
        systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }, immersiveFlagTimeout)
}

fun View.exitImmersiveMode() {
    postDelayed({
        systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }, immersiveFlagTimeout)
}

fun View.showWithCircularReveal(centerPointView: View, onAnimationEnded: (() -> Unit)) {

    this.post {
        val (x, y) = centerPointView.getCenterPositionInWindow()
        val initialRadius = 0.0f
        val finalRadius = max(x, y).toFloat()

        val animation =
            ViewAnimationUtils.createCircularReveal(this, x, y, initialRadius, finalRadius)
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
            override fun onAnimationStart(animation: Animator?) = Unit
            override fun onAnimationEnd(animation: Animator?) {
                onAnimationEnded()
                animation?.removeAllListeners()
            }
        })

        this.isVisible = true
        animation.start()
    }
}

private fun View.getCenterPositionInWindow(): Pair<Int, Int> {
    val positions = intArrayOf(0, 0)
    this.getLocationInWindow(positions)
    return Pair(positions[0] + this.measuredWidth / 2, positions[1] + this.measuredHeight / 2)
}