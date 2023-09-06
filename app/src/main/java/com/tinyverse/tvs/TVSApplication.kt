package com.tinyverse.tvs

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tinyverse.tvs.activities.CrashActivity
import com.tinyverse.tvs.utils.GeneralUtils
import com.tinyverse.tvs.utils.language.LanguageType
import com.tinyverse.tvs.utils.language.MultiLanguageService
import xcrash.ICrashCallback
import xcrash.XCrash
import java.io.File


class TVSApplication  : Application() {
    private val TAG = "TVSApplication"

    override fun onCreate() {
        super.onCreate()
        // 在这里添加您的应用级别初始化和配置逻辑
        // 例如：初始化某些库、设置默认语言、配置全局变量等
        MultiLanguageService.init(this)
        applyLanguageSetting()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        // callback for java crash, native crash and ANR
        val callback = ICrashCallback { logPath, emergency ->
            if(!logPath.isNullOrEmpty()){
                Log.d(
                    TAG, "log path: " + (logPath ?: "(null)") + ", emergency: " + (emergency
                        ?: "(null)")
                )
                launchCrashActivity(this,  CrashActivity::class.java, logPath)
            }
        }

        val nativeCallback = ICrashCallback { logPath, emergency ->
            if(!logPath.isNullOrEmpty()){
                Log.d(
                    TAG, "log path: " + (logPath ?: "(null)") + ", emergency: " + (emergency
                        ?: "(null)")
                )
                if(!logPath.isNullOrEmpty()) {
                    val xCrashLogFile = File(logPath)
                    if (xCrashLogFile.exists()) {
                        GeneralUtils.saveErrorLogToFile(this, xCrashLogFile)
                    }
                }
            }
        }
        XCrash.init(this, XCrash.InitParameters()
            .setJavaCallback(callback)
            .setNativeCallback(nativeCallback)
        )
    }

    private fun applyLanguageSetting() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var selectedLanguage = sharedPrefs.getString("language", null)
        if (selectedLanguage == null) {
            val systemDefaultLanguage = MultiLanguageService.getCurrentLanguage()
            selectedLanguage =  systemDefaultLanguage//default uses the system language
        }
        when(selectedLanguage){
            "en" -> {
                MultiLanguageService.changeLanguage(this, LanguageType.LANGUAGE_EN)
            }
            "zh-CN", "zh", "cn" -> {
                MultiLanguageService.changeLanguage(this, LanguageType.LANGUAGE_ZH_CN)
            }
            else -> {
                MultiLanguageService.changeLanguage(this, LanguageType.LANGUAGE_EN)
            }
        }
    }

    private fun launchCrashActivity(
        applicationContext: Context,
        activity: Class<*>,
        logPath: String
    ) {
        val crashedIntent = Intent(applicationContext, activity).also {
            it.putExtra("crash_log_path", logPath)
        }
        crashedIntent.addFlags( // Clears all previous activities. So backstack will be gone
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        )
        crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        applicationContext.startActivity(crashedIntent)
    }

}