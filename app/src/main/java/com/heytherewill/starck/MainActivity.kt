package com.heytherewill.starck

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.heytherewill.starck.camera.CameraFragment
import com.heytherewill.starck.extensions.startImmersiveMode
import kotlinx.android.synthetic.main.activity_main.*

private const val immersiveFlagTimeout = 500L

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .replace(R.id.container, CameraFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        container.postDelayed({
            container.startImmersiveMode()
        }, immersiveFlagTimeout)
    }
}
