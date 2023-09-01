package com.tinyverse.tvs.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.emirhankolver.GlobalExceptionHandler
import com.tinyverse.tvs.R
import com.tinyverse.tvs.databinding.ActivityCrashBinding
import com.tinyverse.tvs.utils.language.MultiLanguageService
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
                    binding.bLog.text = this.getString(R.string.crash_log_file_prompt) + logFile.absolutePath
                }

            }
        }
        setOnClickListeners()
        setContentView(binding.root)
    }

    override fun attachBaseContext(newBase: Context) { //切换语言
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
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

    private fun saveErrorLogToFile(exception: Throwable): File? {
        val fileName = "tvs_error.log"
        val errorLog = collectErrorLog(exception)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, fileName)
        try {
            FileWriter(logFile, true).use { writer ->
                writer.appendLine("-------------------------------------------------") // 添加分隔符
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