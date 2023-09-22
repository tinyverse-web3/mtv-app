package com.tinyverse.tvs.utils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
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
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.file.Files
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


object GeneralUtils {

    fun saveBase64ImageToGallery(context: Context, fileName: String?, base64Image: String, mimeType: String) {
        try {
            // Decode the base64 string to a Bitmap
            val bytes = android.util.Base64.decode(base64Image.split(",")[1], android.util.Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            // Determine the format to use based on the MIME type
            val format = when (mimeType) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/jpeg" -> Bitmap.CompressFormat.JPEG
                else -> null
            }

            if (format == null) {
                showToast(context, "Unsupported image format")
                return
            }
            var imageName = fileName
            if (fileName == null) {
                imageName = makeFileName()
            }

            // Save the bitmap to the gallery using the content resolver
            val contentResolver: ContentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${imageName}.${format.name}")
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/MyImages")
            }
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            val outputStream = contentResolver.openOutputStream(imageUri!!)
            bmp.compress(format, 100, outputStream)
            outputStream?.close()

//            showToast(context, "Image saved to gallery")
            showToast(context, context.getString(R.string.toast_pic_have_saved))
        } catch (e: Exception) {
            // showToast(context, "Failed to save image to gallery")
            showToast(context, context.getString(R.string.toast_pic_failed_save_pic) + e.localizedMessage)
        }
    }

    fun saveFileToDownload(context: Context, url: String, userAgent: String,
                           contentDisposition: String, mimeType: String){
        val request = DownloadManager.Request(Uri.parse(url))
        request.setMimeType(mimeType)
        request.addRequestHeader("User-Agent", userAgent)
        request.setDescription("Downloading file...")
        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // 注册下载完成广播接收器
        val onComplete = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(context: Context?, intent: Intent?) {
                val receivedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (receivedId == downloadId) {
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            // 下载成功
                            JsCallMtv.requestCodeMap[JsCallMtv.REQUEST_CODE_GET_DOWNLOAD_STATUS]?.success(
                                CallbackBean(
                                    0,
                                    context!!.getString(R.string.toast_download_file_ok),
                                    "success"
                                ),
                                false
                            )
                            showToast(context!!, context.getString(R.string.toast_download_file_ok))
                        } else {
                            // 下载失败
                            JsCallMtv.requestCodeMap[JsCallMtv.REQUEST_CODE_GET_DOWNLOAD_STATUS]?.success(
                                CallbackBean(
                                    -1,
                                    context!!.getString(R.string.toast_download_file_failed),
                                    "failed"
                                ),
                                false
                            )
                            showToast(context!!, context.getString(R.string.toast_download_file_ok))
                        }
                    }
                    cursor.close()
                }
            }
        }
        context.registerReceiver(receiver, onComplete)

    }

    private fun addTimestampSuffixToFileName(fileName: String): String {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val extension = fileName.substringAfterLast(".", "")
        val baseName = fileName.substringBeforeLast(".")

        return "$baseName-$timestamp.$extension"
    }

     fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun makeFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "IMG_$timeStamp"
    }



    fun showToast(context: Context, message: String, duration: Int) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                toast.show()
            }
        }, 0, 3500)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                toast.cancel()
                timer.cancel()
            }
        }, duration.toLong())
    }

    fun dialogInit(context: Context){
        DialogX.init(context)
        DialogX.globalStyle = MaterialYouStyle.style()
    }

    var secretKeyName = "mtv_biometric_encryption_key"

    fun deleteBiometricKey(){
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        try {
            keyStore.deleteEntry(secretKeyName)
        } catch (e: Exception) {
            // 处理异常，比如密钥不存在等情况
            e.printStackTrace()
        }
    }

    fun clearBiometricConfig(context: Context){
        deleteBiometricKey()
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_FILENAME, Context.MODE_PRIVATE)
        if(sharedPrefs.contains(CIPHERTEXT_WRAPPER)){
            val editor: SharedPreferences.Editor = sharedPrefs.edit()
            editor.remove(CIPHERTEXT_WRAPPER)
            editor.apply()
        }
    }

    fun saveErrorLogToFile( context: Context, xCrashLogFile: File): File? {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentDateAndTime = sdf.format(Date())
        val fileName = "tvs_error_$currentDateAndTime.log"
        var logcatLog = collectLogcatLog(context)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logFile = File(downloadsDir, fileName)
        try {
            //向日志中添加TVS App自己的logcat
            FileWriter(xCrashLogFile, true).use { writer ->
                writer.appendLine("TVS App Logcat -------------------------------------------------") // 添加分隔符
                writer.appendLine(logcatLog)
                writer.close()
            }
            // 使用 Files.move() 方法移动文件
            Files.move(xCrashLogFile.toPath(), logFile.toPath())
            return logFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun collectLogcatLog(context: Context):String{
        val maxLines = 200 // 要获取的日志行数
        val packageName = context.packageName
        val command = "logcat -d -t $maxLines | grep $packageName"
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val logcatOutput = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            logcatOutput.appendLine(line)
        }
        return logcatOutput.toString()
    }

    fun createFolderIfNotExists(context: Context) : File{
        //val folderPath = "${Environment.getExternalStoragePublicDirectory("Android")}/$FOLDER_NAME" //数据保存在/sdcard/Android目录中， 删除App，用户数据也会被保留
        val folderPath = "${context.getExternalFilesDir(null)}/${Constants.MTV_SERVICE_ROOT_FOLDER_NAME}" //数据只保存在/sdcard/Android/com.tinyverse.tvs/，删除App，用户数据也会被删除
        val folder = File(folderPath)

        if (!folder.exists()) {
            if (folder.mkdirs()) {
                // 文件夹创建成功
                // Toast.makeText(this, getString(R.string.toast_mtv_folder_created_ok), Toast.LENGTH_SHORT).show()
            } else {
                // 文件夹创建失败
                //Toast.makeText(this, getString(R.string.toast_mtf_folder_created_failed), Toast.LENGTH_SHORT).show()
            }
        }
        return folder
    }

}