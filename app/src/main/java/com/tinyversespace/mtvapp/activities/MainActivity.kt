package com.tinyversespace.mtvapp.activities

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import com.core.web.JsBridgeWebView
import com.core.web.base.BaseWebViewClient
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv

//import helloworld.Hello;
class MainActivity : Activity() {
    var webView: JsBridgeWebView? = null
    var bar: ProgressBar? = null
    val mimeType = "text/html"
    val encoding = "utf-8"
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
        //在此处添加提供给JS调用的接口，可以添加多个接口类每增加一个接口类：webView.addJavascriptInterface(new ToJSAPIClass(this))；
        //可以添加多行；
        val jsCallMtv = JsCallMtv(this)
        webView!!.addJavascriptInterface(jsCallMtv)
        val url = "https://mtv.tinyverse.space/test.html"
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
        }
        //出现net::ERR_CACHE_MISS错误提示
        //使用缓存的方式是基于导航类型。正常页面加载的情况下将缓存内容。当导航返回,
        //内容不会恢复（重新加载生成）,而只是从缓存中取回内容
        if (Build.VERSION.SDK_INT >= 19) {
            webView!!.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        webView!!.loadUrl(url!!)
    }
}