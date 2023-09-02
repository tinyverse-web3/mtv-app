package com.tinyverse.tvs

import android.app.Application
import android.content.Context
import com.emirhankolver.GlobalExceptionHandler
import com.tinyverse.tvs.activities.CrashActivity
import com.tinyverse.tvs.utils.language.LanguageType
import com.tinyverse.tvs.utils.language.MultiLanguageService


class TVSApplication  : Application() {

    override fun onCreate() {
        super.onCreate()
        // 在这里添加您的应用级别初始化和配置逻辑
        // 例如：初始化某些库、设置默认语言、配置全局变量等
        MultiLanguageService.init(this)
        applyLanguageSetting()
        GlobalExceptionHandler.initialize(this, CrashActivity::class.java)
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
}