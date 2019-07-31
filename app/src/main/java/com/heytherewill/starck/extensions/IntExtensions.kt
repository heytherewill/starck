package com.heytherewill.starck.extensions

fun Int.clamp(minInclusive: Int, maxInclusive: Int): Int {
    if (minInclusive > maxInclusive)
        return this

    if (minInclusive > this)
        return minInclusive

    if (maxInclusive < this)
        return maxInclusive

    return this
}