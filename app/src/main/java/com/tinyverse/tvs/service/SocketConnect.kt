package com.tinyverse.tvs.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import com.tinyverse.tvs.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class Message(
val masterKey:  String,
val messageKey: String,
val gunName:    String,
val alias:      String,
val timeStamp:  Long,
val content:    String)

class SocketConnect: Service(){

    private lateinit var mainHandler : Handler

    override fun onCreate() {
        super.onCreate()
        // 在此处进行服务初始化操作
        mainHandler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 在此处执行后台任务逻辑，例如网络请求、数据处理等
        Thread{
            try{
                startConnectServer(this)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }.start()
        // START_STICKY 表示服务被杀死后会自动重启
        return START_STICKY
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Throws(IOException::class)
    fun startConnectServer(context:Context) {
        val port = 9666
        val serverSocket = ServerSocket(port)
        println("Server listening on port $port")
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val clientSocket = serverSocket.accept()
                println("Connect:New client connected: ${clientSocket.inetAddress.hostAddress}")

                launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val writer = PrintWriter(clientSocket.getOutputStream(), true)

                    // 处理客户端请求
                    //while (true) {
                    val request = reader.readLine()// ?: break
                    println("Connect:Received a message: $request")

                    val gson = Gson()
                    val message = gson.fromJson(request, Message::class.java)


                    if (message == null) {
                        println("Connect:json unmashal failed.")
                        return@launch
                    }
                    println("Connect:json unmashal complete")

                    var username = message.messageKey

                    // 将 Unix 时间戳转换为 LocalDateTime 对象
                    val dateTime = Instant.ofEpochSecond(message.timeStamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

                    // 定义要使用的日期时间格式
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")

                    // 格式化 LocalDateTime 对象为字符串
                    val formattedDateTime = dateTime.format(formatter)

                    if (message.alias != "") {
                        username = message.alias
                    } else if (message.gunName != "") {
                        username = message.gunName
                    }else if (message.masterKey != "") {
                        username = message.masterKey
                    }
                    //var msg = formattedDateTime + " Receive a message form " + username + ": " + message.content
                    var msg = context.getString(R.string.toast_message) + "(" + formattedDateTime + ")-> " +  username + ": " + message.content

                    // 处理客户端请求
                    val response = "ok"

                    // 发送响应给客户端
                    writer.println(response)
                    println("Connect:Response ok completed")

                    mainHandler.post {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    //}

                    //println("Connect:No data, close")
                    writer.close()
                    reader.close()
                    clientSocket.close()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 如果服务不提供绑定功能，返回 null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 在此处进行服务销毁操作
    }

}

