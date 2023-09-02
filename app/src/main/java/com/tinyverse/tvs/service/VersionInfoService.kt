package com.tinyverse.tvs.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tinyverse.tvs.utils.language.MultiLanguageService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody


class VersionInfoService : Service() {

    override fun onCreate() {
        super.onCreate()
        // 在此处进行服务初始化操作
    }

    override fun attachBaseContext(newBase: Context) {//当语言设置后，自动更新语言
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 在此处执行后台任务逻辑，例如网络请求、数据处理等
        val tvsWebUrl = intent?.getStringExtra("tvs_version_url")
        Thread{
            try{
                val clearCache = isNeedClearCache(tvsWebUrl!!)
                // 发送广播通知任务完成
                val intent = Intent("$packageName.CLEAR_CACHE")
                intent.putExtra("is_need_clear_cache", clearCache)
                sendBroadcast(intent)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }.start()
        // START_STICKY 表示服务被杀死后会自动重启
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 如果服务不提供绑定功能，返回 null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 在此处进行服务销毁操作
    }


    private fun getVersionFileContent(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            val responseBody: ResponseBody? = response.body()

            if (responseBody != null) {
                val versionContent = responseBody.string()
                responseBody.close() // 关闭资源
                return versionContent
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
        return ""
    }

    private fun isNeedClearCache(versionUrl: String): Boolean{
        val tvsVersionOnLineStr = getVersionFileContent(versionUrl)
        // 等待网络请求返回
        var tvsVersionOnLine = 0;
        if(tvsVersionOnLineStr.isNullOrEmpty()){
            tvsVersionOnLine = 0;
        }else{
            try {
                tvsVersionOnLine = tvsVersionOnLineStr.toInt()
                Log.d(TAG, "current web page version: $tvsVersionOnLine")
            }catch (e: Exception){
                e.printStackTrace()
                tvsVersionOnLine = 0
            }

        }
        Log.i(TAG, "current web page version: $tvsVersionOnLine")
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var tvsVersionSP = sharedPrefs.getInt("version", 0)

        if(tvsVersionOnLine > tvsVersionSP){
            val editor = sharedPrefs.edit()
            editor.putInt("version", tvsVersionOnLine)
            editor.apply()
            return true
        }else {
            return false
        }
    }

    companion object {
        private const val TAG = "VersionInfoService"
    }

}
