package com.heytherewill.starck.edit

import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heytherewill.starck.R

class EditRecyclerAdapter : RecyclerView.Adapter<EditViewHolder>() {

    private val mainThreadHandler = Handler()
    private val backgroundThread = HandlerThread("EditRecyclerAdapter", Thread.MIN_PRIORITY)
    private val backgroundHandler by lazy {
        backgroundThread.start()
        Handler(backgroundThread.looper)
    }

    private var stackImages = listOf<StackImage>()

    override fun getItemCount(): Int = stackImages.size

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): EditViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.fragment_edit_preview, parent, false)

        return EditViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: EditViewHolder, position: Int) =
        viewHolder.bind(stackImages[position])

    fun update(newImages: List<StackImage>) {
        mainThreadHandler.removeCallbacks(null)
        backgroundHandler.removeCallbacks(null)

        backgroundHandler.post {
            val diffUtilCallback = StackItemDiffUtilCallback(stackImages, newImages)
            val diffUtilResult = DiffUtil.calculateDiff(diffUtilCallback)
            mainThreadHandler.post {
                stackImages = newImages
                diffUtilResult.dispatchUpdatesTo(this)
            }
        }
    }

    class StackItemDiffUtilCallback(private val oldItems: List<StackImage>, private val newItems: List<StackImage>) :
        DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}