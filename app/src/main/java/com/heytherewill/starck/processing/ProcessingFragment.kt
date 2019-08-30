package com.heytherewill.starck.processing

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide

import com.heytherewill.starck.R
import com.heytherewill.starck.edit.EditFragmentArgs
import kotlinx.android.synthetic.main.processing_fragment.*

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


        val thread = Thread {

            val imageProcessor = ImageProcessor()

            val glide = Glide.with(this)

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

                val imageUrl = MediaStore.Images.Media.insertImage(
                    requireActivity().contentResolver,
                    stack,
                    "Image Stack",
                    "Created with Starck"
                )


                share.setOnClickListener{

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, imageUrl)
                        type = "image/jpeg"
                    }

                    startActivity(Intent.createChooser(intent, resources.getText(R.string.send_to)))
                }
            }
        }

        thread.start()
    }
}
