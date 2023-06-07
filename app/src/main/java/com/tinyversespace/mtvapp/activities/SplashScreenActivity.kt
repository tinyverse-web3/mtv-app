package com.tinyversespace.mtvapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.service.MtvService
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import java.io.File

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private const val FOLDER_NAME = ".mtv_repo"
        private const val REQUEST_PERMISSION = 1
        var mtvRootPath = ""
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置布局
        setContentView(R.layout.splash_screen)


        //创建MTV服务所需要的存储目录
        // 检查并请求存储权限
        if (isStoragePermissionGranted()) {
            mtvRootPath = createFolderIfNotExists().absolutePath
        }

        // 创建 Handler 对象
        val handler = Handler(Looper.getMainLooper())
        // 延迟 3 秒后跳转到主页面
        handler.postDelayed({
            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)

        // 启动后台服务
        val serviceIntent = Intent(this@SplashScreenActivity, MtvService::class.java).apply {
            putExtra("mtv_root_path", mtvRootPath)
        }
        startService(serviceIntent)
    }


    private fun createFolderIfNotExists() : File{
        val folderPath = "${Environment.getExternalStoragePublicDirectory("Android")}/$FOLDER_NAME"
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
        val grantedPermissions = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission)
            }
        }
        if(grantedPermissions.size == permissions.size) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // API < 30 (android 11)
                return true
            }
            //API >= 30 (android 11)
            if(Environment.isExternalStorageManager()){
                return true
            }
        }
        requestPermission()
        return true
    }


    // 检查是否已经设置 ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION 权限
    private fun hasActionManageAllFilesAccessPermission(context: Context): Boolean {
        return Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION == Settings.Secure.getString(
            context.contentResolver,
            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
        )
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上版本，跳转到系统设置页面
            try{
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }catch (ex: ActivityNotFoundException){
                ex.printStackTrace()
                val appIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(appIntent)
            }

        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
        }
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
                mtvRootPath = createFolderIfNotExists().absolutePath
            } else {
                // 权限被拒绝，无法创建文件夹
                Toast.makeText(this, "The permission is not approved by the user, and the application may not be available!!!", Toast.LENGTH_LONG)
            }
        }
    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_PERMISSION) {
//            // 返回从应用设置页面
//            // 检查权限是否已授予
//            // 请求权限
//            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
//        }
//    }

}