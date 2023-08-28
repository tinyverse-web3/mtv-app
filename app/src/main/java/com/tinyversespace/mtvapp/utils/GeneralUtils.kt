package com.tinyversespace.mtvapp.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.URLUtil
import android.widget.Toast
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogxmaterialyou.style.MaterialYouStyle
import com.tinyversespace.mtvapp.R
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
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, addTimestampSuffixToFileName(URLUtil.guessFileName(url, contentDisposition, mimeType)))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // 注册下载完成广播接收器
        val onComplete = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val receiver = object : BroadcastReceiver() {
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
                            showToast(context!!,context.getString(R.string.toast_download_file_ok))
                        } else {
                            // 下载失败
                            showToast(context!!,context.getString(R.string.toast_download_file_failed))
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

}