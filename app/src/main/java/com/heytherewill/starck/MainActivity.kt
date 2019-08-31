package com.heytherewill.starck

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.heytherewill.starck.camera.CameraFragmentDirections.Companion.actionCameraFragmentToProcessingFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.containsMultipleImages) {
            val images = intent.getImageUrls()
            val processingNavigationDirections = actionCameraFragmentToProcessingFragment(images)

            Navigation.findNavController(this, R.id.fragmentContainer)
                .navigate(processingNavigationDirections)
        }
    }

    private val Intent.containsMultipleImages: Boolean
        get() =
            Intent.ACTION_SEND_MULTIPLE == action && hasExtra(Intent.EXTRA_STREAM)

    private fun Intent.getImageUrls(): Array<String> =
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).map { it.toString() }.toTypedArray()
}
