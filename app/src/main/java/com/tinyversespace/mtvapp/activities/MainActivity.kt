package com.tinyversespace.mtvapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.core.web.CallbackBean
import com.core.web.JsBridgeWebView
import com.core.web.base.BaseWebViewClient
import com.tinyversespace.mtvapp.BuildConfig
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.biometric.AppUser
import com.tinyversespace.mtvapp.biometric.BiometricPromptUtils
import com.tinyversespace.mtvapp.biometric.CIPHERTEXT_WRAPPER
import com.tinyversespace.mtvapp.biometric.CryptographyManager
import com.tinyversespace.mtvapp.biometric.SHARED_PREFS_FILENAME
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv
import com.tinyversespace.mtvapp.utils.GeneralUtils
import com.tinyversespace.mtvapp.views.progress.LoadView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    var webView: JsBridgeWebView? = null
    val encoding = "utf-8"
    private var fileChooser: ValueCallback<Array<Uri>>? = null
    private lateinit var popupWindow: PopupWindow
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private var filetype : String = "*.*"
    private lateinit var jsCallMtv: JsCallMtv

        //for biometric
    private var biometricJsRequestCode: String = ""
    private lateinit var biometricPrompt: BiometricPrompt
    private val cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )


    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE: Int = 10001
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 权限已授予，执行相机或相册操作
                showFileChooserDialog()
            } else {
                // 权限被拒绝
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 拍照成功，处理照片文件
                val selectedFiles = arrayOf(photoUri)
                fileChooser?.onReceiveValue(selectedFiles)
                fileChooser = null
            } else {
                // 用户取消了操作
                fileChooser?.onReceiveValue(null)
                fileChooser = null
            }
        }

    private val chooseFromGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 从相册选择成功，处理选择的文件
                val data: Intent? = result.data
                val selectedFiles = arrayOf(data?.data!!)
                fileChooser?.onReceiveValue(selectedFiles)
                fileChooser = null
            } else {
                // 用户取消了操作
                fileChooser?.onReceiveValue(null)
                fileChooser = null
            }
        }


    private fun getRealPathFromURI(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return null
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    // 显示权限解释给用户
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求相机权限
        requestCameraPermission()

        // 请求存储权限
        requestStoragePermission()

        //将屏幕设置为全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        //去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_webview)
        webView = findViewById<View>(R.id.webView) as JsBridgeWebView


        //初始化webView及设置webView
        webViewInit()

        //加载主页面
        //val url = "https://service.tinyverse.space/test.html"
        var url = "https://dev.tinyverse.space/"
        //var url = "http://192.168.1.104:5173"
        //var url = "https://webcam-test.com/"
        //val url = "https://dragonir.github.io/h5-scan-qrcode/#/"
        loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun webViewInit() {
        //在此处添加提供给JS调用的接口，可以添加多个接口类每增加一个接口类：webView.addJavascriptInterface(new ToJSAPIClass(this))；
        //可以添加多行；
        jsCallMtv = JsCallMtv(this)
        webView!!.addJavascriptInterface(jsCallMtv, "mtv-client")

        val ws = webView!!.settings
        ws.javaScriptEnabled = true //开启JavaScript支持
        ws.domStorageEnabled = true //增加访问web storage支持
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW //允许访问http与https混合模式
        ws.javaScriptCanOpenWindowsAutomatically = true //让JavaScript自动打开窗口，默认false。适用于JavaScript方法window.open()
        ws.allowFileAccess = true //是否允许访问文件，默认允许
        ws.allowContentAccess = true //允许在WebView中访问内容URL（Content Url），默认允许。内容Url访问允许WebView从安装在系统中的内容提 供者载入内容。

        //出现net::ERR_CACHE_MISS错误提示
        //使用缓存的方式是基于导航类型。正常页面加载的情况下将缓存内容。当导航返回,
        //内容不会恢复（重新加载生成）,而只是从缓存中取回内容
        //webView!!.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        ws.cacheMode = WebSettings.LOAD_NO_CACHE

        //下载文件
        webView!!.setDownloadListener(imageDownloadListener)

    }

    //重写onKeyDown(keyCode, event)方法 改写物理按键 返回的逻辑
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView!!.canGoBack()) {
                webView!!.goBack() //返回上一页面
                return true
            } else {
                exitProcess(0) //退出程序
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * The logic is kept inside onResume instead of onCreate so that authorizing biometrics takes
     * immediate effect.
     */
    override fun onResume() {
        super.onResume()

        if (ciphertextWrapper != null) {
            if (AppUser.fakeToken == null) {
                showBiometricPromptForDecryption(biometricJsRequestCode)
            } else {
                // The user has already logged in, so proceed to the rest of the app
                // this is a todo for you, the developer
            }
        }
    }


    private fun loadUrl(url: String?) {
        webView!!.webViewClient = object : BaseWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                var newUrl = updateUrlTimestamp(request.url.toString())
                webView!!.loadUrl(newUrl)
                return true
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler,
                error: SslError?
            ) {
                Log.d(TAG, "onReceivedSslError!")
                handler.proceed() // Ignore SSL certificate errors
            }
        }

        //添加进度条
        LoadView.initDialog(this)

        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {
                    LoadView.stopLoading()
                } else {
                    LoadView.showLoading(view.context, newProgress.toString() + "")
                    if (newProgress == 100) {
                        LoadView.stopLoading()
                    }
                }
                super.onProgressChanged(view, newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                var selectFile = 0
                var filetypes = fileChooserParams?.acceptTypes
                if (filetypes != null){
                    if (filetypes[0].isNotEmpty() && filetypes[0] == "image/*") {
                        selectFile = 1
                    }
                }
                fileChooser = filePathCallback

                if (selectFile == 1) {
                    // need select a image , first request camera permission
                    // 请求权限
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    // 请求存储权限
                    chooseFile()
                }

                return true
            }
        }

        // 加载主页面
        webView!!.loadUrl(url!!)
    }




    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun showFileChooserDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_file_chooser, null)
        val btnCamera = view.findViewById<Button>(R.id.btn_camera)
        val btnGallery = view.findViewById<Button>(R.id.btn_gallery)

        btnCamera.setOnClickListener {
            // 拍照
            takePhoto()
            popupWindow.dismiss()
        }

        btnGallery.setOnClickListener {
            // 从相册选择
            chooseFromGallery()
            popupWindow.dismiss()
        }

        popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(webView, Gravity.BOTTOM, 0, 0)
    }

    private fun takePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            try {
                photoFile = createImageFile()
                photoUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    photoFile
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePictureLauncher.launch(cameraIntent)
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    private fun chooseFromGallery() {
       // val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        chooseFromGalleryLauncher.launch(galleryIntent)
    }

    private fun chooseFile() {
        //val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "*/*"
        chooseFromGalleryLauncher.launch(galleryIntent)
    }



    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun requestCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 相机权限未授予，需要进行请求
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // 如果之前用户拒绝过权限请求，可以在这里给出一些解释说明
                // 可以显示一个对话框或者弹出一个提示，说明需要相机权限的原因
            }
            // 请求相机权限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            // 相机权限已经授予，可以进行相机操作
        }
    }

    private var imageDownloadListener =
        DownloadListener { url, _, _, mimetype, _ ->
            GeneralUtils.saveBase64ImageToGallery(this, null, url, mimetype)
        }

    fun startBiometricVerify(jsRequestCode: String) {
        this.biometricJsRequestCode = jsRequestCode
        if (ciphertextWrapper != null) {
            showBiometricPromptForDecryption(jsRequestCode)
        } else {
            showEnableBiometricDialog()
        }
    }

    // for biometric
    private fun showBiometricPromptForDecryption(jsRequestCode: String) {
        ciphertextWrapper?.let { textWrapper ->
            val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val secretKeyName = getString(R.string.secret_key_name)
                val cipher = cryptographyManager.getInitializedCipherForDecryption(
                    secretKeyName, textWrapper.initializationVector
                )
                biometricPrompt =
                    BiometricPromptUtils.createBiometricPrompt(
                        jsRequestCode,
                        this,
                        ::decryptServerTokenFromStorage
                    )
                val promptInfo = BiometricPromptUtils.createPromptInfo(this)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }else{
                GeneralUtils.showToast(this, "用户无法进行身份验证，因为没有注册生物识别或设备凭据。", 15 * 1000)//15秒
            }
        }
    }

    // for biometric
    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult, requestCode: String) {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                val plaintext =
                    cryptographyManager.decryptData(textWrapper.ciphertext, it)
                AppUser.fakeToken = plaintext
                // Now that you have the token, you can query server for everything else
                // the only reason we call this fakeToken is because we didn't really get it from
                // the server. In your case, you will have gotten it from the server the first time
                // and therefore, it's a real token.
                val callback = JsCallMtv.requestCodeMap[requestCode]
                if(callback != null){
                    val message = "生物识别认证成功"
                    val data: Any = plaintext
                    val isDelete = false
                    callback.success(CallbackBean(0, message, data), isDelete)
                }
            }
        }
    }

    //for biometric
    private fun showEnableBiometricDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("应用生物识别未配置")
            .setMessage("请解锁后在设置页面进行设置")
            .setPositiveButton("确定") { _, _ ->
            }
            .create()

        alertDialog.show()
    }

    //解决当页面刷新jsbridge失效的问题，通过解决刷新后添加一个时间戳
    private fun updateUrlTimestamp(originalUrl: String): String {
        // 创建一个格式化日期的对象
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        // 获取当前时间并格式化为时间戳
        val timestamp = dateFormat.format(Date())
        // 使用 Uri.Builder 构建 URL，并在参数中添加时间戳
        val uri = Uri.parse(originalUrl)
            .buildUpon()
            .appendQueryParameter("timestamp", timestamp)
            .build()
        // 获取带有时间戳的最终 URL
        return uri.toString()
    }
}