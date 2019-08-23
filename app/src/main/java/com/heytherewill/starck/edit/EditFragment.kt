package com.heytherewill.starck.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.heytherewill.starck.R
import com.heytherewill.starck.extensions.exitImmersiveMode
import kotlinx.android.synthetic.main.fragment_edit.*

class EditFragment : Fragment() {

    private val args: EditFragmentArgs by navArgs()

    private lateinit var viewModel: EditViewModel
    private lateinit var glide: RequestManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_edit, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(EditViewModel::class.java)
        viewModel.setImagesToProcess(args.imagesToProcess)

        glide = Glide.with(this)

        val adapter = EditRecyclerAdapter()
        previewRecyclerView.adapter = adapter
        previewRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)


        viewModel.stackImages.observe(this, Observer { images ->
            adapter.update(images)

            images.firstOrNull()?.also {
                glide.load(it.imageUrl).into(baseImageView)
                glide.load(it.imageUrl).into(selectedImageView)
            }
        })

        activity.let { it as? AppCompatActivity }?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()

        previewRecyclerView.exitImmersiveMode()
    }
}
