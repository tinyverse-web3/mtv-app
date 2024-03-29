package com.tinyverse.tvs.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tinyverse.tvs.utils.Constants
import com.tinyverse.tvs.utils.ServiceUtils
import com.tinyverse.tvs.utils.language.MultiLanguageService
import core.Core


class MtvService : Service() {
    val TAG = "MtvService"
    override fun onCreate() {
        super.onCreate()
        // 在此处进行服务初始化操作
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 在此处执行后台任务逻辑，例如网络请求、数据处理等
        val mtvRootPath = intent?.getStringExtra("mtv_root_path")
        Thread{
            try{
                if (!ServiceUtils.isPortListening("127.0.0.1", Constants.MTV_SERVICE_PORT.toInt())) {
                    Log.d(TAG, "MtvService-->onStartCommand()-->start mtv server start")
                    Core.startDauthService(
                        Constants.MTV_SERVICE_PORT,
                        Constants.MTV_SERVICE_TYPE,
                        mtvRootPath,
                        Constants.MTV_SERVICE_APP_NAME
                    )
                    Log.d(TAG, "MtvService-->onStartCommand()-->start mtv server end")
                    Log.d(TAG, "MtvService-->onStartCommand()-->check mtv server start")
                    val checkIsOK =  Core.checkServerIsOK(30)
                    Log.d(TAG, "MtvService-->onStartCommand()-->check mtv server end")
                    if(checkIsOK){
                        val checkApiIsOK = ServiceUtils.checkServiceAPIAvailability(5)
                        if (checkApiIsOK) {
                        // 发送广播通知任务完成
                        sendNotification(
                            true,
                            Constants.ServerMsg.LAUNCH_SUCCESS.name
                            )
                        }else{
                            sendNotification(false, Constants.ServerMsg.API_ACCESS_FAIL.name)
                        }
                    }else{
                        sendNotification(false, Constants.ServerMsg.PORT_LISTENING_FAIL.name)
                    }
                }else{
                    Log.d(TAG, "MtvService-->onStartCommand()-->Service already exists and does not need to be started.")
                }
            } catch (e: Exception){
                e.printStackTrace()
                sendNotification(false, Constants.ServerMsg.LAUNCH_FAILED.name)
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
        Log.d(TAG, "MtvService-->onDestroy()")
        //发送广播通知MainActivity完成退出
//        val intent = Intent("$packageName.EXIT_APP")
//        sendBroadcast(intent)
    }

    private fun sendNotification(result: Boolean, message: String){
        val intent = Intent("$packageName.MTV_SERVER_LAUNCH")
        intent.putExtra("server_is_ok", result)
        intent.putExtra("server_msg", message)
        this.sendBroadcast(intent)
    }
}
