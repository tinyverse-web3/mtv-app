package com.tinyverse.tvs.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.emirhankolver.GlobalExceptionHandler
import com.tinyverse.tvs.databinding.ActivityCrashBinding
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess


@SuppressLint("SetTextI18n")

class CrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashBinding.inflate(layoutInflater)
        GlobalExceptionHandler.getThrowableFromIntent(intent).let {
            Log.e(TAG, "Error Data: ", it)
            if (it != null) {
                val logFile = saveErrorLogToFile(it)
                //显示日志文件路径：
                if(logFile != null){
                    binding.bLog.text = "Report us log: " + logFile.absolutePath
                }

            }
        }
        setOnClickListeners()
        setContentView(binding.root)
    }

    private fun setOnClickListeners() {
        binding.bRestartApp.setOnClickListener {
            binding.bRestartApp.isEnabled = false
            binding.bRestartApp.text = "Starting..."
            finish()
            val intent = Intent(this, SplashScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            exitProcess(0)
        }
    }

    private fun uncaughtException(exception: Throwable) {
        val logPath: String
        if (Environment.getExternalStorageState() ==
            Environment.MEDIA_MOUNTED
        ) {
            logPath = (Environment.getExternalStorageDirectory()
                .absolutePath
                    + File.separator
                    + File.separator
                    + "log")
            val file = File(logPath)
            if (!file.exists()) {
                file.mkdirs()
            }
            try {
                val fw = FileWriter(
                    logPath + File.separator
                            + "tvs_errorlog.log", true
                )
                fw.write(Date().toString() + "\n")
                // 错误信息
                // 这里还可以加上当前的系统版本，机型型号 等等信息
                val stackTrace = exception.stackTrace
                fw.write(exception.message + "\n")
                for (i in stackTrace.indices) {
                    fw.write(
                        ("file:" + stackTrace[i].fileName + " class:"
                                + stackTrace[i].className + " method:"
                                + stackTrace[i].methodName + " line:"
                                + stackTrace[i].lineNumber + "\n")
                    )
                }
                fw.write("\n")
                fw.close()
                //显示日志文件路径：
                binding.bLog.text = "Error Log: $logPath"
                // 上传错误信息到服务器
                // uploadToServer();
            } catch (e: IOException) {
                Log.e("crash handler", "load file failed...", e.cause)
            }
        }
        exception.printStackTrace()
    }

    private fun saveErrorLogToFile(exception: Throwable): File? {
        val fileName = "tvs_error.log"
        val errorLog = collectErrorLog(exception)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, fileName)
        try {
            FileWriter(logFile, true).use { writer ->
                writer.appendLine("-------------------------------") // 添加分隔符
                writer.appendLine(errorLog)
            }
            return logFile
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun collectErrorLog(exception: Throwable): String {
        val stackTrace = StringBuilder()
        stackTrace.append("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        stackTrace.append("Exception:\n")
        stackTrace.append("${exception.javaClass.name}: ${exception.message}\n")
        for (element in exception.stackTrace) {
            stackTrace.append("    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n")
        }
        return stackTrace.toString()
    }


    companion object {
        private const val TAG = "CrashActivity"
    }



}