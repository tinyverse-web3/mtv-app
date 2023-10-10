package com.tinyverse.tvs.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import com.tinyverse.tvs.activities.SplashScreenActivity
import com.tinyverse.tvs.service.MtvService
import com.tinyverse.tvs.service.SocketConnect
import com.tinyverse.tvs.utils.Constants.MTV_SERVICE_TEST_API
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


object ServiceUtils {

    fun launchMtvServer(context: Context){
        //创建MTV服务所需要的存储目录
        val mtvFolder = GeneralUtils.createFolderIfNotExists(context)
        SplashScreenActivity.mtvRootPath = mtvFolder.absolutePath
        // 启动后台服务
        val serviceIntent = Intent(context, MtvService::class.java).apply {
            putExtra("mtv_root_path", mtvFolder.absolutePath)
        }
        context.startService(serviceIntent)
    }

    fun stopMtvServer(context: Context){
        // 停止后台服务
        val serviceIntent = Intent(context, MtvService::class.java)
        context.stopService(serviceIntent)
    }


    fun launchSocketServer(context: Context){
        // 启动后台服务
        val socketServiceIntent = Intent(context, SocketConnect::class.java)
        context.startService(socketServiceIntent)
    }

    fun stopSocketServer(context: Context){
        // 停止后台服务
        val serviceIntent = Intent(context, SocketConnect::class.java)
        context.stopService(serviceIntent)
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        // 检查服务是否在后台运行
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun isPortListening(host: String, port: Int): Boolean {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 500) // 连接超时设置为1秒
            socket.close()
            return true // 连接成功，端口在监听中
        } catch (e: IOException) {
            // 连接失败，端口未监听
            return false
        }
    }

    fun checkServiceAPIAvailability(timeoutSeconds: Long): Boolean{
        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBody: RequestBody =
            RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "")

        val request = Request.Builder()
            .url(MTV_SERVICE_TEST_API)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            // 检查 HTTP 状态码是否在 200...299 范围内
            if (response.isSuccessful) {
                return true // 服务可用
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false // 服务不可用或超时
    }
}