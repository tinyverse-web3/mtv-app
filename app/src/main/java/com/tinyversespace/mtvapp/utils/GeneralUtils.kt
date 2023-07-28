package com.tinyversespace.mtvapp.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            showToast(context, "图片已经保存到相册中")
        } catch (e: Exception) {
            // showToast(context, "Failed to save image to gallery")
            showToast(context, "图片保存失败：" + e.localizedMessage)
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun makeFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "IMG_$timeStamp"
    }

}