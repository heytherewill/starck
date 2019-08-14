package com.heytherewill.starck.extensions

import android.widget.SeekBar

fun SeekBar.onProgressChangedListener(progressCallback: (Int) -> Unit) {
    setOnSeekBarChangeListener(SeekBarChangeListener(progressCallback))
}

private class SeekBarChangeListener(private val progressCallback: (Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        progressCallback(progress)
    }
}