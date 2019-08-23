package com.heytherewill.starck.edit

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heytherewill.starck.R

class EditViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private lateinit var stackImage: StackImage
    private val imageView = itemView.findViewById<ImageView>(R.id.previewImageView)
    private val selectedBackground = itemView.findViewById<View>(R.id.selectedBackground)

    fun bind(stackImage: StackImage) {
        this.stackImage = stackImage

        Glide.with(imageView)
            .load(stackImage.imageUrl)
            .centerCrop()
            .into(imageView)

        selectedBackground.isVisible = false
    }
}