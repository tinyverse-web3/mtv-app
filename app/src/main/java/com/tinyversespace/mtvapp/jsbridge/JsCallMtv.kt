package com.tinyversespace.mtvapp.jsbridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.core.web.Callback
import com.core.web.CallbackBean
import com.tinyversespace.mtvapp.activities.FingerprintActivity
import com.tinyversespace.mtvapp.activities.QrcodeScanActivity

class JsCallMtv(private val context: Context) {


    @JavascriptInterface
    fun nativeNoArgAndNoCallback() {
        Toast.makeText(context, "调用原生无参数无回调方法", Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun nativeNoArgAndCallback(callback: Callback) {
//        startFingerActivity(object : ActivityResultCallback {
//            override fun onResult(result: String) {
//                // 处理返回结果
//                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
//                callback.success()
//            }
//        })
       startQrcodeScanActivity(callback)
        // 处理返回结果
        Toast.makeText(context, "调用原生无参数有回调方法", Toast.LENGTH_SHORT).show()
        callback.success()
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

    private fun startFingerActivity(callback: Callback){
        requestCodeMap[REQUEST_CODE_QRCODE_SCAN] = callback
        val intent = Intent(context, FingerprintActivity::class.java)
        if (context is Activity) {
            intent.putExtra("request_code", REQUEST_CODE_QRCODE_SCAN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun startQrcodeScanActivity(callback: Callback){
        //QrcodeScanActivity.startSelf(context)
        requestCodeMap[REQUEST_CODE_QRCODE_SCAN] = callback
        val intent = Intent(context, QrcodeScanActivity::class.java)
        if (context is Activity) {
            intent.putExtra("request_code", REQUEST_CODE_QRCODE_SCAN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }


    companion object {
        const val REQUEST_CODE_FINGER_ACTIVITY: String = "1000"
        const val REQUEST_CODE_QRCODE_SCAN: String = "1002"
        val requestCodeMap = HashMap<String, Callback>()
    }
}