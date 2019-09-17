package com.heytherewill.starck.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations

fun <TIn, TOut> LiveData<TIn>.map(mapFn: (TIn) -> TOut): LiveData<TOut> =
    Transformations.map(this, mapFn)