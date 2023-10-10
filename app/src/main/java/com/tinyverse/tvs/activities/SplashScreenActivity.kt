package com.tinyverse.tvs.activities

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
import com.tinyverse.tvs.R
import com.tinyverse.tvs.service.SocketConnect
import com.tinyverse.tvs.service.VersionInfoService
import com.tinyverse.tvs.utils.Constants
import com.tinyverse.tvs.utils.GeneralUtils
import com.tinyverse.tvs.utils.ServiceUtils
import com.tinyverse.tvs.utils.language.MultiLanguageService
import kotlin.system.exitProcess


@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private const val FOLDER_NAME = Constants.MTV_SERVICE_ROOT_FOLDER_NAME
        private const val REQUEST_PERMISSION = 1
        private const val REQUEST_MANAGE_ALL_FILES_ACCESS_PERMISSION_CODE = 1001
        var mtvRootPath = ""
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        lateinit var requestPermissionLauncher : ActivityResultLauncher<Intent>
        private const val TAG = "SplashScreenActivity"
    }

    private val broadcastData = HashMap<String, Any>()

    private val mtvServerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 接收到任务完成的广播，启动 MainActivity
            val serverIsOk = intent.getBooleanExtra("server_is_ok", false)
            var serverMsg =  intent.getStringExtra("server_msg")
            // 处理第一个服务的广播
            broadcastData["server_is_ok"] = serverIsOk
            if(serverMsg.isNullOrEmpty()){
                serverMsg = Constants.ServerMsg.LAUNCH_FAILED.name
            }
            broadcastData["server_msg"] = serverMsg
            checkBroadcastResult(broadcastData)
        }
    }
    private val versionServerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 接收到任务完成的广播，启动 MainActivity
            val isNeedClearCache = intent.getBooleanExtra("is_need_clear_cache", true)
            // 处理第一个服务的广播
            broadcastData["is_need_clear_cache"] = isNeedClearCache
            checkBroadcastResult(broadcastData)
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

            //数据只保存在/sdcard/Android/com.tinyverse.tvs/目录中 注意：删除App，用户数据也会被删除
            launchMtvServer()
            ServiceUtils.launchSocketServer(this@SplashScreenActivity)
            launchVersionServer(Constants.TVS_WEB_VERSION_URL)
        }
        // 注册广播接收器
        //通过serverCompletedReceiver广播器来接收服务是否启动OK
        val mtvFilter = IntentFilter("$packageName.MTV_SERVER_LAUNCH")
        registerReceiver(mtvServerReceiver, mtvFilter)
        val versionFilter = IntentFilter("$packageName.CLEAR_CACHE")
        registerReceiver(versionServerReceiver, versionFilter)

        //初始化全局DilogX
        GeneralUtils.dialogInit(this.applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 在活动销毁时注销广播接收器
        unregisterReceiver(mtvServerReceiver)
        unregisterReceiver(versionServerReceiver)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
    }


    private fun launchMtvServer(){
        ServiceUtils.launchMtvServer(this@SplashScreenActivity)
    }

    private fun checkBroadcastResult(dataMap: HashMap<String, Any>) {
        val keysToCheck = listOf("server_is_ok", "is_need_clear_cache")
        if (!checkKeysExist(keysToCheck)) {
            return
        }
        //两个服务都完成了，执行下一步任务
        // 接收到任务完成的广播，启动 MainActivity
        val serverIsOk = dataMap["server_is_ok"] as? Boolean
        val serverMsg =  dataMap["server_msg"] as? String
        val isNeedClearCache = dataMap["is_need_clear_cache"] as? Boolean
        if(serverIsOk!!){
            val mainIntent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            mainIntent.putExtra("is_need_clear_cache",  isNeedClearCache)
            //Toast.makeText(context, getString(R.string.toast_dauth_launch_success), Toast.LENGTH_SHORT).show()
            startActivity(mainIntent)
            // 关闭当前活动
            finish()
        }else{
            //提示用户进行操作，重启应用还是退出应用
            Toast.makeText(this, getString(R.string.toast_dauth_launch_failed), Toast.LENGTH_LONG).show()
            val hasExitBtn = true
            var hasRebootBtn = true
            var promptMsg :String
            when(serverMsg){
                Constants.ServerMsg.LAUNCH_FAILED.name, Constants.ServerMsg.PORT_LISTENING_FAIL.name ->{
                    promptMsg =  getString(R.string.mtv_service_launch_failed_message)
                }
                Constants.ServerMsg.API_ACCESS_FAIL.name -> {
                    promptMsg = getString(R.string.mtv_service_api_access_failed_message)
                    hasRebootBtn = false
                }
                else -> {
                    promptMsg = getString(R.string.mtv_service_launch_failed_message)
                }
            }
            promptUserForAction(promptMsg, hasRebootBtn, hasExitBtn)
        }
    }

    private fun checkKeysExist(keys: List<String>): Boolean {
        for (key in keys) {
            if (!broadcastData.containsKey(key)) {
                return false
            }
        }
        return true
    }

    private fun launchSocketServer(){
        // 启动后台服务
        val socketServiceIntent = Intent(this@SplashScreenActivity, SocketConnect::class.java)
        startService(socketServiceIntent)
    }

    private fun launchVersionServer(versionUrl: String){
        // 启动后台服务
        val versionServiceIntent = Intent(this@SplashScreenActivity, VersionInfoService::class.java).apply {
            putExtra("tvs_version_url", versionUrl)
        }
        startService(versionServiceIntent)
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
                            .setTitle(getString(R.string.per_request_title))
                            .setMessage(getString(R.string.per_external_storage_request_message))
                            .setCancelable(false)
                            .setOkButton(getString(R.string.per_dialog_button_go_grant)) { baseDialog, _ ->
                                baseDialog.dismiss()
                                // 跳转到应用设置页面
                                authorizeAccessSdcard()
                                false
                            }
                            .setCancelButton(getString(R.string.per_dialog_button_exit_app)) { baseDialog, _ ->
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
                Toast.makeText(this, getString(R.string.toast_folder_permission_approved_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun promptUserForAction(errMsg: String, hasRebootBtn: Boolean, hasExitBtn: Boolean){
        var messageDialog = MessageDialog.build()
            .setTitle(getString(R.string.dialog_title_tip))
            .setMessage(errMsg)
            .setCancelable(false)
        if(hasRebootBtn) {
            messageDialog.setOkButton(getString(R.string.mtv_service_dialog_button_restart)) { baseDialog, _ ->
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
        }
        if(hasExitBtn) {
            messageDialog.setCancelButton(getString(R.string.mtv_service_dialog_button_exit)) { baseDialog, _ ->
                // 关闭对话框
                baseDialog.dismiss()
                // 退出应用
                exitProcess(0)
                false
            }
        }
        messageDialog.show()
    }



    private fun startLogoAnimator(logoImageView: ImageView){
        // 创建 ValueAnimator 实现缩放动画
        val scaleAnimator = ValueAnimator.ofFloat(0.4f, 0.6f)
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