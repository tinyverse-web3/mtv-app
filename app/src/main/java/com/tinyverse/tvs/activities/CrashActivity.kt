package com.tinyverse.tvs.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tinyverse.tvs.R
import com.tinyverse.tvs.databinding.ActivityCrashBinding
import com.tinyverse.tvs.utils.GeneralUtils
import com.tinyverse.tvs.utils.language.MultiLanguageService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess


class CrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashBinding.inflate(layoutInflater)
        setOnClickListeners()
        setContentView(binding.root)
        val xCrashLogPath = intent.getStringExtra("crash_log_path")
        if (xCrashLogPath != null) {
            Log.d("CrashActivity", xCrashLogPath)
        }
        if(!xCrashLogPath.isNullOrEmpty()){
            val xCrashLogFile = File(xCrashLogPath)
            if(xCrashLogFile.exists()){
                val logFile = GeneralUtils.saveErrorLogToFile(this, xCrashLogFile)
                if(logFile != null){
                    binding.bLog.text = this.getString(R.string.crash_log_file_prompt) + logFile.absolutePath
                }
            }
        }
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

    private fun backupLog(logFileName: String){
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, logFileName)
        if(!logFile.exists()){
            return
        }
        val fileSizeInBytes: Long = logFile.length()
        val fileSizeInMB: Long = (fileSizeInBytes / (1024 * 1024)).toLong() // 将字节转换为MB
        // 如果日志文件大小超过10MB，执行备份和删除操作
        if (fileSizeInBytes > 10) {
            // 获取当前日期作为备份文件名
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val currentDateAndTime = sdf.format(Date())

            // 构建备份文件名
            val backupFileName = "tvs_error_$currentDateAndTime.log"

            // 备份日志文件
            val backupFile = File(logFile.parent, backupFileName)
            logFile.renameTo(backupFile)

            // 删除原日志文件
            logFile.delete()
        }

    }

    companion object {
        private const val TAG = "CrashActivity"
    }



}