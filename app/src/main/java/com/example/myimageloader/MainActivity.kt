package com.example.myimageloader

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
            var imageView: ImageView = findViewById(R.id.iv_test)
            MyImageLoader.isWriteToLocal().with(this).load("https://profile.csdnimg.cn/1/F/9/1_m0_52051799").into(imageView)
    }
}