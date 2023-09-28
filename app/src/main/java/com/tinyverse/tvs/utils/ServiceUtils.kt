package com.tinyverse.tvs.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.URLUtil
import android.widget.Toast
import com.core.web.CallbackBean
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogxmaterialyou.style.MaterialYouStyle
import com.tinyverse.tvs.R
import com.tinyverse.tvs.activities.SplashScreenActivity
import com.tinyverse.tvs.biometric.CIPHERTEXT_WRAPPER
import com.tinyverse.tvs.biometric.SHARED_PREFS_FILENAME
import com.tinyverse.tvs.jsbridge.JsCallMtv
import com.tinyverse.tvs.service.MtvService
import com.tinyverse.tvs.service.SocketConnect
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


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

}