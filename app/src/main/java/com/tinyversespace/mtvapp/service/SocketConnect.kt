package com.tinyversespace.mtvapp.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
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

class SocketConnect {
    private val mainHandler = Handler(Looper.getMainLooper())

    @OptIn(DelicateCoroutinesApi::class)
    @Throws(IOException::class)
    fun startConnectServer(context:Context) {
        val port = 8080
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
                    var msg = formattedDateTime + " Receive a message form " + username + ": " + message.content

                    // 处理客户端请求
                    val response = "ok"

                    // 发送响应给客户端
                    writer.println(response)
                    println("Connect:Response ok completed")

                    mainHandler.post {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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


   }

