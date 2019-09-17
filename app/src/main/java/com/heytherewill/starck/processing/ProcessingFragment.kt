package com.heytherewill.starck.processing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.saveImageToGallery
import kotlinx.android.synthetic.main.processing_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProcessingFragment : Fragment() {

    private val args: ProcessingFragmentArgs by navArgs()

    private lateinit var viewModel: ProcessingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.processing_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ProcessingViewModel::class.java)

        val glide = Glide.with(this)

        GlobalScope.launch(Dispatchers.IO) {
            val imageProcessor = ImageProcessor()

            val bitmaps = args.imagesToProcess
                .map { glide.asBitmap().load(it).submit().get() }
                .toTypedArray()

            val stack = imageProcessor.stackBitmaps(bitmaps)

            share.post {
                stackedImage.setImageBitmap(stack)
                share.isVisible = true
                stackedImage.isVisible = true

                processingImagesText.isVisible = false
                processingStatusText.isVisible = false

                val imageUrl = saveImageToGallery(stack)

                share.setOnClickListener {

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, imageUrl)
                        type = "image/jpeg"
                    }

                    startActivity(Intent.createChooser(intent, resources.getText(R.string.send_to)))
                }
            }
        }
    }
}
