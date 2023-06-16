package com.tinyversespace.mtvapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.fragments.CameraXFragment
import com.tinyversespace.mtvapp.fragments.MinutiaeFragment
import com.tinyversespace.mtvapp.fragments.PreviewFragment
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv
import com.tinyversespace.mtvapp.utils.PhotoItem
import java.io.File


class FingerprintActivity : AppCompatActivity(), CameraXFragment.OnPhotoTakenListener {

    companion object {
        private const val TAG = "FingerprintActivity"
    }
    private var photoItems: ArrayList<PhotoItem> = arrayListOf()
    private var photoCounter: Int = 0


    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var imageView: ImageView
    private lateinit var statusImageView: ImageView

    private var imageCapture: ImageCapture? = null
    private var currentStep: Int = 1

    private val deleteRequestCode = 1001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerpint)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        imageView = findViewById(R.id.imageView)

        captureButton.setOnClickListener {
            showCameraFragment()
            captureButton.isClickable = false
        }

        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 申请权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                deleteRequestCode
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == deleteRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，执行删除操作
                Toast.makeText(this, "sdcard permission granted予", Toast.LENGTH_SHORT)
            } else {
                // 权限被拒绝
                // 处理权限被拒绝的情况
                Toast.makeText(this, "The authorization of the sdcard failed, and the write and delete operations cannot be performed", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onPhotoTaken(photoUri: Uri) {
        //TODO 调用处理方法验证图片是否合格
//        val inputStream = this.contentResolver.openInputStream(photoUri)
//        if (inputStream != null) {
//            val byteArray = inputStream.readBytes()
//            // 这里可以使用 byteArray，即为读取到的文件内容
//            inputStream.close()
//        } else {
//            // 无法打开输入流，处理错误情况
//        }
        val isPhotoOK = false
        photoItems.add(PhotoItem(photoUri, isPhotoOK))
        photoCounter++
        currentStep++
        showPreviewFragment()
    }



    private fun deletePhotoFromGallery(context: Context, savedUri: Uri) {
        val contentResolver: ContentResolver = context.contentResolver
        val delFile = savedUri.toFile()

        // 构建删除操作的查询条件
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(delFile.name)
        val queryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        // 执行删除操作
        val deletedCount = contentResolver.delete(queryUri, selection, selectionArgs)


        if (deletedCount > 0) {
            Toast.makeText(context, "Successes to delete photo", Toast.LENGTH_SHORT).show()
            return
        }
        if (delFile.exists()) {
            val deleted = delFile.delete()
            if(deleted){
                // 扫描文件以更新相册
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(delFile.path),
                    null,
                    null
                )
                Toast.makeText(context, "Successes to delete photo", Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(context, "Failed to delete photo", Toast.LENGTH_SHORT).show()
    }


    private fun showCameraFragment() {
        val fragment = MinutiaeFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun onPhotoPreviewContinue() {
        if (photoCounter < 3) {
            // 继续拍摄下一张照片
            captureButton.text = "拍摄第 $currentStep 张指纹照片"
            showCameraFragment()
        } else {
            // 显示所有照片的预览页面
            showPreviewFragment()
            //TODO 调用go方法生成保险箱特征数据，然后返回给前端
            if(checkAllPicIsOK(photoItems)){
                showConfirmationDialog()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun onPhotoPreviewRetry() {
        // 处理继续按钮的点击事件
        // 可以执行一些逻辑，如继续拍照或完成拍照流程等
        deleteCurrentPhoto()
        captureButton.text = "拍摄第 $currentStep 张指纹照片"
        showCameraFragment()
    }

    private fun showPreviewFragment() {
        val fragment = PreviewFragment.newInstance(photoItems)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteCurrentPhoto() {
        if( photoCounter <= 0){
            return
        }
        var delIndex = photoCounter -1
        deletePhotoFromGallery(this, photoItems[delIndex].photoUri)
        photoItems.removeAt(delIndex)
        photoCounter--
        currentStep--
    }


//    //TODO 增加调用JS的回调函数返回结果给前端
//    private fun handleResultAndReturn() {
//        // 处理操作逻辑
//        val result = "完成指纹收集"
//
//        // 处理结果并返回到普通类
//        val jsBridge = JsCallMtv(this)
//        jsBridge.handleActivityResult(result)
//
//        // 关闭当前Activity
//        finish()
//    }

    // 在返回键按下时处理返回结果并关闭当前 Activity
    override fun onBackPressed() {
//        handleResultAndReturn()
        super.onBackPressed()
    }


    private fun showConfirmationDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Confirmation")
        alertDialogBuilder.setMessage("The fingerprint photo has been collected, do you want to proceed?")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, which ->
            //handleResultAndReturn()
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, which ->
            // Do nothing, stay on the current activity
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun checkAllPicIsOK( photoItems: ArrayList<PhotoItem>) : Boolean{
        var isOK = true
        for(item in photoItems){
            if(!item.status){
                isOK = false
                break
            }
        }
        return isOK
    }


}


