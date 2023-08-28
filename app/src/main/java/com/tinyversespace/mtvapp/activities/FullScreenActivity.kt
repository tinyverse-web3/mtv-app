package com.tinyversespace.mtvapp.activities

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.utils.language.MultiLanguageService

class FullScreenActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        // 获取传递过来的图片Uri
        imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI))

        imageView = findViewById(R.id.imageViewFullScreen)

        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish() // 关闭当前的 FullScreenActivity
        }

        // 加载并显示图片
        Glide.with(this)
            .load(imageUri)
            .into(imageView)

        // 设置全屏展示
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.1f, 10.0f) // 控制缩放的最小和最大值

            imageView.scaleX = scaleFactor
            imageView.scaleY = scaleFactor
            return true
        }
    }

    fun onCloseClick(view: View) {
        // 关闭全屏展示的 Activity
        finish()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
