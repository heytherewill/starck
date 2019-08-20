package com.heytherewill.starck.extensions

private val nanosInSeconds = 1000000000f

fun Long.nanosToSeconds(): Float = this / nanosInSeconds