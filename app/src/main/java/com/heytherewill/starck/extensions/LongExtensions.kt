package com.heytherewill.starck.extensions

private val nanosInSeconds = 1000000000

fun Long.nanosToSeconds(): Long = this / nanosInSeconds
fun Long.secondsToNanos(): Long = this * nanosInSeconds
