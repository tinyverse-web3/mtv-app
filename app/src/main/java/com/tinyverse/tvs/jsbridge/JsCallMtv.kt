package com.tinyverse.tvs.jsbridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.core.web.Callback
import com.core.web.CallbackBean
import com.tinyverse.tvs.BuildConfig
import com.tinyverse.tvs.R
import com.tinyverse.tvs.activities.BiometricLoginActivity
import com.tinyverse.tvs.activities.FingerprintActivity
import com.tinyverse.tvs.activities.GoogleLoginActivity
import com.tinyverse.tvs.activities.MainActivity
import com.tinyverse.tvs.activities.QrcodeScanActivity
import com.tinyverse.tvs.biometric.AppUser
import com.tinyverse.tvs.utils.GeneralUtils
import com.tinyverse.tvs.utils.language.LanguageType
import com.tinyverse.tvs.utils.language.MultiLanguageService
import java.lang.Exception


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
    fun clearBiometrics(callback: Callback) {
        GeneralUtils.clearBiometricConfig(context)
        callback.success(CallbackBean(0,  context.getString(R.string.prompt_info_clear_bio_set_ok), "success"), false)

    }

    @JavascriptInterface
    fun setupLanguage(params: String, callback: Callback) {
        requestCodeMap[REQUEST_CODE_SET_UP_LANGUAGE] = callback
        if (context is Activity) {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            var selectedLanguage = params.trim()
            if(selectedLanguage.isNullOrEmpty()){
                callback.success(CallbackBean(-1, context.getString(R.string.language_switch_failed), "failed"), false)
                return
            }
            var savedLanguage = sharedPrefs.getString("language", null)
            if(!savedLanguage.isNullOrEmpty() && savedLanguage == selectedLanguage){
                callback.success(CallbackBean(0,  context.getString(R.string.language_switch_successfully), "success"), false)
                return
            }
            val editor = sharedPrefs.edit()
            editor.putString("language", params.trim()) // 'selectedLanguage' 是用户选择的新语言
            editor.apply()
            //val handler = Handler(Looper.getMainLooper())
            //GeneralUtils.showToast(context, context.getString(R.string.toast_language_switched))
            callback.success(CallbackBean(0,  context.getString(R.string.language_switch_successfully), "success"), false)

            when(selectedLanguage){
                "en" -> {
                    MultiLanguageService.changeLanguage(context, LanguageType.LANGUAGE_EN)
                    restartCurrentActivity(context)
                }
                "zh-CN" -> {
                    MultiLanguageService.changeLanguage(context, LanguageType.LANGUAGE_ZH_CN)
                    restartCurrentActivity(context)
                }
            }
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

    //访问外部链接
    @JavascriptInterface
    fun accessLink(params: String, callback: Callback) {
        if (context is Activity) {
            if(params.isNullOrEmpty()){
                callback.success(CallbackBean(-1, context.getString(R.string.jscall_access_link_url_error), "failed"), false)
                return
            }
            var url = params.trim()
            val intent = Intent(Intent.ACTION_VIEW)
            try{
                val uri = Uri.parse(url)
                intent.data = uri
                context.startActivity(intent)
                callback.success(CallbackBean(0, context.getString(R.string.jscall_access_link_url_ok), "success"), false)
            }catch (e : Exception){
                e.printStackTrace()
                callback.success(CallbackBean(-2, context.getString(R.string.jscall_access_link_url_error), "failed"), false)
            }
        }
    }

    @JavascriptInterface
    fun getDownloadStatus(callback: Callback) {//通过callback返回下文件载状态
        requestCodeMap[REQUEST_CODE_GET_DOWNLOAD_STATUS] = callback
    }

    @JavascriptInterface
    fun getAppVersion(callback: Callback) {//通过callback返回当前App版本号
       val versionName = BuildConfig.VERSION_NAME ?: "1.0.0"
        callback.success(CallbackBean(0, context.getString(R.string.app_version_number), versionName), false)
    }

  @JavascriptInterface
  fun startGoogleLogin(callback: Callback) {
    requestCodeMap[REQUEST_CODE_GOOGLE_SIGN_IN] = callback
    val mainActivity = context as MainActivity
    if (context is Activity) {
      // 进行Google登录
      val intent = Intent(context, GoogleLoginActivity::class.java)
      intent.putExtra("request_code", REQUEST_CODE_GOOGLE_SIGN_IN)
      context.startActivity(intent)
      context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // 添加淡入淡出动画效果
    }
  }

  @JavascriptInterface
  fun startGoogleLogout(callback: Callback) {
    requestCodeMap[REQUEST_CODE_GOOGLE_SIGN_OUT] = callback
    val mainActivity = context as MainActivity
    if (context is Activity) {
      // 进行Google登出
      val intent = Intent(context, GoogleLoginActivity::class.java)
      intent.putExtra("request_code", REQUEST_CODE_GOOGLE_SIGN_OUT)
      context.startActivity(intent)
      context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // 添加淡入淡出动画效果
    }
  }


    // 在当前的 Activity 中执行重启操作
    private fun restartCurrentActivity(context: Context) {
        val mainActivity = context as MainActivity
        mainActivity.finish() // 关闭当前 Activity
        context.startActivity(mainActivity.intent) // 重新启动当前 Activity
    }



    companion object {
        const val REQUEST_CODE_FINGER_ACTIVITY: String = "1000"
        const val REQUEST_CODE_QRCODE_SCAN: String = "1002"
        const val REQUEST_CODE_BIOMETRIC_VERIFY: String = "1003"
        const val REQUEST_CODE_SET_UP_BIOMETRIC: String = "1004"
        const val REQUEST_CODE_IS_BIOMETRIC_SET_UP: String = "1005"
        const val REQUEST_CODE_SET_UP_LANGUAGE: String = "1006"
        const val REQUEST_CODE_GET_DOWNLOAD_STATUS: String = "1007"
        const val REQUEST_CODE_GOOGLE_SIGN_IN: String = "1008"
        const val REQUEST_CODE_GOOGLE_SIGN_OUT: String = "1009"
        val requestCodeMap = HashMap<String, Callback>()
    }
}
