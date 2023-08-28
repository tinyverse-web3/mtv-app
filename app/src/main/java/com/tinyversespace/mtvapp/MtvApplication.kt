package com.tinyversespace.mtvapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.tinyversespace.mtvapp.utils.language.LanguageType
import com.tinyversespace.mtvapp.utils.language.MultiLanguageService


class MtvApplication  : Application() {

    override fun onCreate() {
        super.onCreate()
        // 在这里添加您的应用级别初始化和配置逻辑
        // 例如：初始化某些库、设置默认语言、配置全局变量等
        MultiLanguageService.init(this)
        applyLanguageSetting()
    }

    private fun applyLanguageSetting() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var selectedLanguage = sharedPrefs.getString("language", null)
        if (selectedLanguage == null) {
            selectedLanguage = "en" //default language
        }
        when(selectedLanguage){
            "en" -> {
                MultiLanguageService.changeLanguage(this, LanguageType.LANGUAGE_EN)
            }
            "zh-CN" -> {
                MultiLanguageService.changeLanguage(this, LanguageType.LANGUAGE_ZH_CN)
            }
        }
    }
}