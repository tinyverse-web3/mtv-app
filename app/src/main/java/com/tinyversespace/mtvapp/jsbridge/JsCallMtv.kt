package com.tinyversespace.mtvapp.jsbridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.core.web.Callback
import com.core.web.JsCallback
import com.tinyversespace.mtvapp.activities.BiometricLoginActivity
import com.tinyversespace.mtvapp.activities.FingerprintActivity
import com.tinyversespace.mtvapp.activities.MainActivity
import com.tinyversespace.mtvapp.activities.QrcodeScanActivity
import com.tinyversespace.mtvapp.biometric.AppUser

class JsCallMtv(private val context: Context) {

    @JavascriptInterface
    fun nativeNoArgAndNoCallback() {
        Toast.makeText(context, "调用原生无参数无回调方法", Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun nativeNoArgAndCallback(callback: Callback) { //测试使用
//        startFingerActivity(object : ActivityResultCallback {
//            override fun onResult(result: String) {
//                // 处理返回结果
//                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
//                callback.success()
//            }
//        })
//       startQrcodeScanActivity(callback)
        takePhoto()
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

    private fun startFingerActivity(callback: Callback) {
        requestCodeMap[REQUEST_CODE_QRCODE_SCAN] = callback
        val intent = Intent(context, FingerprintActivity::class.java)
        if (context is Activity) {
            intent.putExtra("request_code", REQUEST_CODE_QRCODE_SCAN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun startQrcodeScanActivity(callback: Callback) {
        //QrcodeScanActivity.startSelf(context)
        requestCodeMap[REQUEST_CODE_QRCODE_SCAN] = callback
        val mainActivity = context as MainActivity
        if (context is Activity) {
            val intent = Intent(context, QrcodeScanActivity::class.java)
            intent.putExtra("request_code", REQUEST_CODE_QRCODE_SCAN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun startBiometric(callback: Callback) {
        requestCodeMap[REQUEST_CODE_BIOMETRIC_VERIFY] = callback
        val mainActivity = context as MainActivity
        if (context is Activity) {
            // 进行生物识别验证
            val intent = Intent(context, BiometricLoginActivity::class.java)
            intent.putExtra("request_code", REQUEST_CODE_BIOMETRIC_VERIFY)
            context.startActivity(intent)
            context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // 添加淡入淡出动画效果
        }
    }

    @JavascriptInterface
    fun isBiometricsSetUp(callback: Callback) {
        requestCodeMap[REQUEST_CODE_IS_BIOMETRIC_SET_UP] = callback
        val mainActivity = context as MainActivity
        if (context is Activity) {
            // 应用是否开启了生物识别验证
            val intent = Intent(context, BiometricLoginActivity::class.java)
            intent.putExtra("request_code", REQUEST_CODE_IS_BIOMETRIC_SET_UP)
            context.startActivity(intent)
            context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // 添加淡入淡出动画效果
        }
    }

    @JavascriptInterface
    fun setupBiometrics(params: String, callback: Callback) {
        requestCodeMap[REQUEST_CODE_SET_UP_BIOMETRIC] = callback
        AppUser.fakeToken = params //清空之前的用户token
        if (context is Activity) {
            // 设置生物识别验证
            val intent = Intent(context, BiometricLoginActivity::class.java)
            intent.putExtra("request_code", REQUEST_CODE_SET_UP_BIOMETRIC)
            context.startActivity(intent)
            context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // 添加淡入淡出动画效果
        }
    }

    @JavascriptInterface
    fun takePhoto() {
        val mainActivity = context as MainActivity
        if (context is Activity) {
            // 进行拍照
            mainActivity.takePhoto()
        }
    }


    companion object {
        const val REQUEST_CODE_FINGER_ACTIVITY: String = "1000"
        const val REQUEST_CODE_QRCODE_SCAN: String = "1002"
        const val REQUEST_CODE_BIOMETRIC_VERIFY: String = "1003"
        const val REQUEST_CODE_SET_UP_BIOMETRIC: String = "1004"
        const val REQUEST_CODE_IS_BIOMETRIC_SET_UP: String = "1005"
        val requestCodeMap = HashMap<String, Callback>()
    }
}