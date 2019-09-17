package com.heytherewill.starck.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.provider.MediaStore
import androidx.fragment.app.Fragment

fun Fragment.saveImageToGallery(image: Image): String {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    return saveImageToGallery(bitmapImage)
}

fun Fragment.saveImageToGallery(image: Bitmap): String =
    MediaStore.Images.Media.insertImage(
        requireActivity().contentResolver,
        image,
        "Image Stack",
        "Created with Starck"
    )