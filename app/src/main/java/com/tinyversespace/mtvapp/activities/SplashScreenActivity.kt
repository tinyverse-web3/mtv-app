package com.tinyversespace.mtvapp.activities

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kongzue.dialogx.dialogs.MessageDialog
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.service.MtvService
import com.tinyversespace.mtvapp.utils.GeneralUtils
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess


@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private const val FOLDER_NAME = ".mtv_repo"
        private const val REQUEST_PERMISSION = 1
        private const val REQUEST_MANAGE_ALL_FILES_ACCESS_PERMISSION_CODE = 1001
        var mtvRootPath = ""
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        lateinit var requestPermissionLauncher : ActivityResultLauncher<Intent>
        private lateinit var latch: CountDownLatch
    }

    private val serverCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 接收到任务完成的广播，启动 MainActivity
            val serverIsOk = intent.getBooleanExtra("server_is_ok", false)
            if(serverIsOk){
                val mainIntent = Intent(this@SplashScreenActivity, MainActivity::class.java)
//                Toast.makeText(context, "Dauth server launch successfully", Toast.LENGTH_SHORT).show()
                startActivity(mainIntent)
                // 关闭当前活动
                finish()
            }else{
                //提示用户进行操作，重启应用还是退出应用
                Toast.makeText(context, "Dauth server launch failed!!!", Toast.LENGTH_LONG).show()
                promptUserForAction()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置布局
        setContentView(R.layout.splash_screen)

        // 创建一个动画
        var logoImageView = findViewById<ImageView>(R.id.splash_image)
        startLogoAnimator(logoImageView)

        if(savedInstanceState == null){ //防止onCreate被多次调用
            // 检查并请求存储权限
            //if (isStoragePermissionGranted()) { //数据保存在/sdcard/Android目录中，需要调用此方法来进行授权，删除App，用户数据也会被保留
            //    launchMtvServer()
            //}

            //数据只保存在/sdcard/Android/com.tinyversespace.mtvapp/目录中 注意：删除App，用户数据也会被删除
            launchMtvServer()
        }
        // 注册广播接收器
        //通过serverCompletedReceiver广播器来接收服务是否启动OK
        val filter = IntentFilter("$packageName.MTV_SERVER_LAUNCH")
        registerReceiver(serverCompletedReceiver, filter)

        //初始化全局DilogX
        GeneralUtils.dialogInit(this.applicationContext)
    }



    override fun onDestroy() {
        super.onDestroy()

        // 在活动销毁时注销广播接收器
        unregisterReceiver(serverCompletedReceiver)
    }

    private fun launchMtvServer(){
        //创建MTV服务所需要的存储目录
        mtvRootPath = createFolderIfNotExists().absolutePath
        // 启动后台服务
        val serviceIntent = Intent(this@SplashScreenActivity, MtvService::class.java).apply {
            putExtra("mtv_root_path", mtvRootPath)
        }
        startService(serviceIntent)
    }

    private fun createFolderIfNotExists() : File{
        //val folderPath = "${Environment.getExternalStoragePublicDirectory("Android")}/$FOLDER_NAME" //数据保存在/sdcard/Android目录中， 删除App，用户数据也会被保留
        val folderPath = "${this.getExternalFilesDir(null)}/$FOLDER_NAME" //数据只保存在/sdcard/Android/com.tinyversespace.mtvapp/，删除App，用户数据也会被删除
        val folder = File(folderPath)

        if (!folder.exists()) {
            if (folder.mkdirs()) {
                // 文件夹创建成功
                Toast.makeText(this, "MTV server storage folder created successfully", Toast.LENGTH_SHORT).show()
            } else {
                // 文件夹创建失败
                Toast.makeText(this, "MTV server storage folder created failed", Toast.LENGTH_SHORT).show()
            }
        }
        return folder
    }


    private fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // API < 30 (android 11)
            val grantedPermissions = mutableListOf<String>()
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    grantedPermissions.add(permission)
                }
            }
            if (grantedPermissions.size == permissions.size) {
                return true
            }
        }
        //API >= 30 (android 11)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (Environment.isExternalStorageManager()) {   // 检查是否已经设置 ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION 权限
                return true
            }
        }
        requestPermission()
        return false
    }



    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上版本，跳转到系统设置页面
            requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val receivedData = intent.getIntExtra("request_code", 0)
                if(receivedData == REQUEST_MANAGE_ALL_FILES_ACCESS_PERMISSION_CODE){
                    if ( result.resultCode == Activity.RESULT_OK || Environment.isExternalStorageManager()) {
                        // 用户已经授权 MANAGE_EXTERNAL_STORAGE 权限，可以执行后续的代码
                        // 在这里执行您的代码逻辑
                        launchMtvServer()
                    } else {
                        // 用户未授权 MANAGE_EXTERNAL_STORAGE 权限，弹出提示对话框
                        MessageDialog.build()
                            .setTitle("权限请求")
                            .setMessage("需要 MANAGE_EXTERNAL_STORAGE 权限才能正常使用应用，请前往设置授权。")
                            .setCancelable(false)
                            .setOkButton("去授权") { baseDialog, _ ->
                                baseDialog.dismiss()
                                // 跳转到应用设置页面
                                authorizeAccessSdcard()
                                false
                            }
                            .setCancelButton("退出应用") { baseDialog, _ ->
                                baseDialog.dismiss()
                                finish()
                                false
                            }
                            .show()
                    }
                }
            }
            authorizeAccessSdcard()
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this@SplashScreenActivity, permissions, REQUEST_PERMISSION)
        }
    }

    private fun authorizeAccessSdcard(){
        if( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && Environment.isExternalStorageManager()){
           return
        }
        val appIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("request_code", REQUEST_MANAGE_ALL_FILES_ACCESS_PERMISSION_CODE)
        requestPermissionLauncher.launch(appIntent)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if(allPermissionsGranted){
               launchMtvServer()
            } else {
                // 权限被拒绝，无法创建文件夹
                Toast.makeText(this, "The permission is not approved by the user, and the application may not be available!!!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun promptUserForAction(){
        MessageDialog.build()
            .setTitle("提示")
            .setMessage("服务启动失败，是否重启应用？")
            .setCancelable(true)
            .setOkButton("重启") { baseDialog, _ ->
                baseDialog.dismiss()
                // 重启应用
                // 延迟3秒后重新启动应用
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    finish()
                    val intent = Intent(this, SplashScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    exitProcess(0)
                }, 3000)
                false
            }
            .setCancelButton("退出") { baseDialog, _ ->
                // 关闭对话框
                baseDialog.dismiss()
                // 退出应用
                exitProcess(0)
                false
            }
            .show()
    }

    private fun startLogoAnimator(logoImageView: ImageView){
        // 创建 ValueAnimator 实现缩放动画
        val scaleAnimator = ValueAnimator.ofFloat(0.5f, 1.0f)
        scaleAnimator.duration = 2000L
        scaleAnimator.interpolator = AccelerateInterpolator()

        // 设置动画更新监听器
        scaleAnimator.addUpdateListener { animator ->
            val scale = animator.animatedValue as Float
            logoImageView.scaleX = scale
            logoImageView.scaleY = scale
        }

        // 设置动画重复模式为REVERSE，表示循环放大和缩小
        scaleAnimator.repeatMode = ValueAnimator.REVERSE

        // 设置动画重复次数为INFINITE，表示无限循环
        scaleAnimator.repeatCount = ValueAnimator.INFINITE

        // 启动动画
        scaleAnimator.start()
    }

}