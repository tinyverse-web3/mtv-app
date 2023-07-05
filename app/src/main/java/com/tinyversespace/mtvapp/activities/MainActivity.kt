package com.tinyversespace.mtvapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.core.web.JsBridgeWebView
import com.core.web.base.BaseWebViewClient
import com.tinyversespace.mtvapp.BuildConfig
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    var webView: JsBridgeWebView? = null
    var bar: ProgressBar? = null
    val mimeType = "text/html"
    val encoding = "utf-8"
    private var fileChooser: ValueCallback<Array<Uri>>? = null
    private lateinit var popupWindow: PopupWindow
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE: Int = 10001
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 权限已授予，执行相机或相册操作
                showFileChooserDialog()
            } else {
                // 权限被拒绝
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 拍照成功，处理照片文件
                val selectedFiles = arrayOf(photoUri)
                fileChooser?.onReceiveValue(selectedFiles)
                fileChooser = null
            } else {
                // 用户取消了操作
                fileChooser?.onReceiveValue(null)
                fileChooser = null
            }
        }

    private val chooseFromGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 从相册选择成功，处理选择的文件
                val data = result.data
                val selectedFiles = arrayOf(data?.data!!)
                fileChooser?.onReceiveValue(selectedFiles)
                fileChooser = null
            } else {
                // 用户取消了操作
                fileChooser?.onReceiveValue(null)
                fileChooser = null
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //将屏幕设置为全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        //去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_webview)
        bar = findViewById<View>(R.id.progressBar1) as ProgressBar
        webView = findViewById<View>(R.id.webView) as JsBridgeWebView

        //设置允许http
        allowHttp(webView!!)

        //设置允许文件上传
        allUploadUploadFile(webView!!)


        //在此处添加提供给JS调用的接口，可以添加多个接口类每增加一个接口类：webView.addJavascriptInterface(new ToJSAPIClass(this))；
        //可以添加多行；
        val jsCallMtv = JsCallMtv(this)
        webView!!.addJavascriptInterface(jsCallMtv, "mtv-client")
        val url = "https://service.tinyverse.space/test.html"
        //var url = "http://192.168.3.181:5173"
        //var url = "https://webcam-test.com/"
        //val url = "https://dragonir.github.io/h5-scan-qrcode/#/"
        loadUrl(url)
    }

    //重写onKeyDown(keyCode, event)方法 改写物理按键 返回的逻辑
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView!!.canGoBack()) {
                webView!!.goBack() //返回上一页面
                return true
            } else {
                exitProcess(0) //退出程序
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun loadUrl(url: String?) {
        val ws = webView!!.settings
        ws.javaScriptEnabled = true //开启JavaScript支持
        ws.domStorageEnabled = true //增加访问web storage支持
        webView!!.webViewClient = object : BaseWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                webView!!.loadUrl(request.url.toString())
                return true
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler,
                error: SslError?
            ) {
                Log.d(TAG, "onReceivedSslError!")
                handler.proceed() // Ignore SSL certificate errors
            }
        }
        //添加进度条
        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {
                    bar!!.visibility = View.INVISIBLE
                } else {
                    if (View.INVISIBLE == bar!!.visibility) {
                        bar!!.visibility = View.VISIBLE
                    }
                    bar!!.progress = newProgress
                }
                super.onProgressChanged(view, newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooser = filePathCallback

                // 请求权限
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)

                return true
            }

        }

        //出现net::ERR_CACHE_MISS错误提示
        //使用缓存的方式是基于导航类型。正常页面加载的情况下将缓存内容。当导航返回,
        //内容不会恢复（重新加载生成）,而只是从缓存中取回内容
//        webView!!.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webView!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView!!.loadUrl(url!!)
    }


    private fun allowHttp(appWebView: JsBridgeWebView){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        appWebView.settings.allowFileAccessFromFileURLs = true
        appWebView.settings.allowUniversalAccessFromFileURLs = true
    }

    private fun allUploadUploadFile(appWebView: JsBridgeWebView){
        appWebView.settings.allowFileAccess = true
        appWebView.settings.allowContentAccess = true
        appWebView.settings.allowFileAccessFromFileURLs = true
        appWebView.settings.allowUniversalAccessFromFileURLs = true
        appWebView.settings.domStorageEnabled = true
        appWebView.settings.mediaPlaybackRequiresUserGesture = false
    }


    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun showFileChooserDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_file_chooser, null)
        val btnCamera = view.findViewById<Button>(R.id.btn_camera)
        val btnGallery = view.findViewById<Button>(R.id.btn_gallery)

        btnCamera.setOnClickListener {
            // 拍照
            takePhoto()
            popupWindow.dismiss()
        }

        btnGallery.setOnClickListener {
            // 从相册选择
            chooseFromGallery()
            popupWindow.dismiss()
        }

        popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(webView, Gravity.BOTTOM, 0, 0)
    }

    private fun takePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            try {
                photoFile = createImageFile()
                photoUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    photoFile
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePictureLauncher.launch(cameraIntent)
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    private fun chooseFromGallery() {
       // val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        chooseFromGalleryLauncher.launch(galleryIntent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun requestCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 相机权限未授予，需要进行请求
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // 如果之前用户拒绝过权限请求，可以在这里给出一些解释说明
                // 可以显示一个对话框或者弹出一个提示，说明需要相机权限的原因
            }
            // 请求相机权限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            // 相机权限已经授予，可以进行相机操作
        }
    }


}