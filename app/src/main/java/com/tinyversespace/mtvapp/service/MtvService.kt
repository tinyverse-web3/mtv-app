package com.tinyversespace.mtvapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import core.Core


class MtvService : Service() {

    override fun onCreate() {
        super.onCreate()
        // 在此处进行服务初始化操作
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 在此处执行后台任务逻辑，例如网络请求、数据处理等
        val mtvRootPath = intent?.getStringExtra("mtv_root_path")
        Thread{
            try{
                Core.startDauthService("9888", "sdk", mtvRootPath, "mtv")
                val checkIsOK =  Core.checkServerIsOK(30)
                // 发送广播通知任务完成
                val intent = Intent("$packageName.MTV_SERVER_LAUNCH")
                intent.putExtra("server_is_ok", checkIsOK)
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

    private fun createRootDir(){

    }
}
