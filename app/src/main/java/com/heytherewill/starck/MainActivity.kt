package com.heytherewill.starck

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.heytherewill.starck.camera.CameraFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .replace(R.id.container, CameraFragment())
            .commit()
    }
}
