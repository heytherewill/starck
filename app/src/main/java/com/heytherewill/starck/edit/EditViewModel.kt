package com.heytherewill.starck.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;

class EditViewModel : ViewModel() {

    private val _stackImages: MutableLiveData<List<StackImage>> = MutableLiveData()
    val stackImages: LiveData<List<StackImage>> = _stackImages

    fun setImagesToProcess(imagesToProcess: Array<String>) {
        _stackImages.value = imagesToProcess.map { StackImage(it, true) }
    }
}

data class StackImage(val imageUrl: String, val isIncludedInStack: Boolean)