package com.tinyversespace.mtvapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.core.web.JsBridgeWebView
import com.core.web.base.BaseWebViewClient
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv


class MainActivityBackup : AppCompatActivity() {

    var webView: JsBridgeWebView? = null
    var bar: ProgressBar? = null
    val mimeType = "text/html"
    val encoding = "utf-8"
    private  var fileChooser: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultUri = data?.data
                fileChooser!!.onReceiveValue(arrayOf(resultUri!!))
                fileChooser = null
            } else {
                fileChooser!!.onReceiveValue(null)
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
        webView = findViewById<View>(R.id.webView) as JsBridgeWebView

        //设置允许http
        allowHttp(webView!!)

        //设置允许文件上传
        allUploadUploadFile(webView!!)


        //在此处添加提供给JS调用的接口，可以添加多个接口类每增加一个接口类：webView.addJavascriptInterface(new ToJSAPIClass(this))；
        //可以添加多行；
        val jsCallMtv = JsCallMtv(this)
        webView!!.addJavascriptInterface(jsCallMtv, "mtv-client")
        //val url = "https://service.tinyverse.space/test.html"
        var url = "http://192.168.3.181:5173"
        loadUrl(url)

        //String
        Toast.makeText(this, "调用原生无参数无回调方法", Toast.LENGTH_SHORT).show()
    }

    //重写onKeyDown(keyCode, event)方法 改写物理按键 返回的逻辑
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView!!.canGoBack()) {
                webView!!.goBack() //返回上一页面
                return true
            } else {
                System.exit(0) //退出程序
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
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooser = filePathCallback
                return true
            }

        }

        //出现net::ERR_CACHE_MISS错误提示
        //使用缓存的方式是基于导航类型。正常页面加载的情况下将缓存内容。当导航返回,
        //内容不会恢复（重新加载生成）,而只是从缓存中取回内容
        if (Build.VERSION.SDK_INT >= 19)
        {
            webView!!.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
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
    }

    private fun showFileChooser() {
        if (hasCameraPermission()) {
            openFileChooser()
        } else {
            requestCameraPermission()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        fileChooserLauncher.launch(intent)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            Companion.PERMISSIONS_REQUEST_CAMERA
        )
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Companion.PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser()
            } else {
                // 权限被拒绝
            }
        }
    }

    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CAMERA = 100
    }


}