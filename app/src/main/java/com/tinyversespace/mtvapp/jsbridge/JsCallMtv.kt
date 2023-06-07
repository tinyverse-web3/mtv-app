package com.tinyversespace.mtvapp.jsbridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.core.web.Callback
import com.tinyversespace.mtvapp.activities.FingerprintActivity

class JsCallMtv(private val context: Context) {

    private var activityResultCallback: ActivityResultCallback? = null

    @JavascriptInterface
    fun nativeNoArgAndNoCallback() {
        Toast.makeText(context, "调用原生无参数无回调方法", Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun nativeNoArgAndCallback(callback: Callback) {
        startFingerActivity(object : ActivityResultCallback {
            override fun onResult(result: String) {
                // 处理返回结果
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                callback.success()
            }
        })
    }

    @JavascriptInterface
    fun nativeArgAndNoCallback(params: String?) {
        Toast.makeText(context, params, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun nativeArgAndCallback(params: String?, callback: Callback) {
        Toast.makeText(context, params, Toast.LENGTH_SHORT).show()
        callback.success()
    }

    @JavascriptInterface
    fun nativeDeleteCallback(params: String?, callback: Callback) {
        Toast.makeText(context, params, Toast.LENGTH_SHORT).show()
        callback.success(true)
        Handler().postDelayed({ callback.error(1, "错误回调") }, 3000)
    }

    @JavascriptInterface
    fun nativeNoDeleteCallback(params: String?, callback: Callback) {
        Toast.makeText(context, params, Toast.LENGTH_SHORT).show()
        callback.success(false)
        Handler().postDelayed({ callback.error(1, "错误回调") }, 3000)
    }

    @JavascriptInterface
    fun nativeSyncCallback(): String {
        return "原生同步回调"
    }

    private fun startFingerActivity(callback: ActivityResultCallback){
        this.activityResultCallback = callback
        val intent = Intent(context, FingerprintActivity::class.java)
        if (context is Activity) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivityForResult(intent, REQUEST_CODE_SECOND_ACTIVITY)
        }
    }


    // 在回调方法中处理返回结果
    fun handleActivityResult(result: String) {
        activityResultCallback?.onResult(result)
    }


    private fun startIpfsActivity(callback: ActivityResultCallback){
        this.activityResultCallback = callback
        val intent = Intent(context, FingerprintActivity::class.java)
        if (context is Activity) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivityForResult(intent, REQUEST_CODE_SECOND_ACTIVITY)
        }
    }

    companion object {
        const val REQUEST_CODE_SECOND_ACTIVITY: Int = 1001
    }
}