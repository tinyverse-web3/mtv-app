package com.tinyverse.tvs.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.core.web.JsBridgeWebView
import com.core.web.JsInject
import com.core.web.base.BaseWebView
import com.core.web.base.BaseWebViewClient
import com.kongzue.dialogx.dialogs.MessageDialog
import com.tinyverse.tvs.BuildConfig
import com.tinyverse.tvs.R
import com.tinyverse.tvs.jsbridge.JsCallMtv
import com.tinyverse.tvs.utils.Constants
import com.tinyverse.tvs.utils.GeneralUtils
import com.tinyverse.tvs.utils.ServiceUtils
import com.tinyverse.tvs.utils.language.MultiLanguageService
import com.tinyverse.tvs.views.progress.LoadView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var jsCallMtv: JsCallMtv
    private lateinit var homeFeatureString: Array<String>
    private var jsInject: JsInject? = null
    private var isBackButtonClicked = false
    private val resetDelayMillis = 2000L // 设置延迟时间，单位为毫秒
    private var isBackPressedOnce = false
    private var isNeedClearCache = false
    private var isFirstTimeResume = true



    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE: Int = 10001
    }

    private val exitAppReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "$packageName.EXIT_APP") {
                finishAffinity()
                exitProcess(0)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 权限已授予，执行相机或相册操作
                showFileChooserDialog()
            } else {
                // 权限被拒绝
                Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_SHORT).show()
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

        //注册一个退出应用的广播接收器
        val filter = IntentFilter("$packageName.EXIT_APP")
        registerReceiver(exitAppReceiver, filter)

        //是否要清除Cache
        //isNeedClearCache = intent.getBooleanExtra("is_need_clear_cache", true)
        isNeedClearCache = true

        // 请求存储权限
        requestStoragePermission()

        //将屏幕设置为全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        //去掉标题栏
        setContentView(R.layout.activity_main_webview)
        webView = findViewById<View>(R.id.webView) as JsBridgeWebView
        //supportRequestWindowFeature(Window.FEATURE_NO_TITLE)


        //初始化webView及设置webView
        webViewInit()

        //初始化jsInject
        jsInject = JsInject(webView!!)

        //加载主页面
        loadUrl(Constants.TVS_WEB_URL)

        //主页面url特征字符串：表示回到主页面
        homeFeatureString = arrayOf("/home/space", "/unlock", "/index")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity-->onResume()")
        if (isFirstTimeResume) {
            // 这是第一次调用 onResume
            Log.i(TAG, "MainActivity-->onResume(): there is no need to do anything when calling onResume for the first time." )
            isFirstTimeResume = false
           return
        }
        // 使用协程在后台线程中执行检查端口的操作
        val job = GlobalScope.launch(Dispatchers.IO) {
            val isPortAvailable = ServiceUtils.isPortListening("127.0.0.1", Constants.MTV_SERVICE_PORT.toInt()) // 传入你要检查的端口号
            var checkApiIsOK = isPortAvailable
            if(isPortAvailable){//进一步检查api是否可用
                checkApiIsOK = ServiceUtils.checkServiceAPIAvailability(2)
            }
            withContext(Dispatchers.Main) {
                // 在主线程中根据结果执行操作
                if (!isPortAvailable) {
                    Log.w(TAG, "MainActivity-->onResume(): the port(" +  Constants.MTV_SERVICE_PORT + ") is not listening, the service has exited and the application needs to be restarted.")
                    // 端口可用，服务已经退出了需要重启应用
                    finish()
                    val intent = Intent(this@MainActivity, SplashScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    exitProcess(0)
                }else if(!checkApiIsOK){
                    promptUserForExitAction(getString(R.string.mtv_service_api_access_failed_message))
                }
            }
        }
    }

    private fun promptUserForExitAction(errMsg: String){
        var messageDialog = MessageDialog.build()
            .setTitle(getString(R.string.dialog_title_tip))
            .setMessage(errMsg)
            .setCancelable(false)
            .setCancelButton(getString(R.string.mtv_service_dialog_button_exit)) { baseDialog, _ ->
                // 关闭对话框
                baseDialog.dismiss()
                // 退出应用
                exitProcess(0)
                false
            }
        messageDialog.show()
    }


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授权成功，执行拍照操作
                takePic()
            } else {
                Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show()
            }
        }
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
        webView!!.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
//        ws.cacheMode = WebSettings.LOAD_NO_CACHE

        //下载文件
        webView!!.setDownloadListener(fileDownloadListener)

        //清除webview cache
        if(isNeedClearCache){
            webView!!.clearCache(true)
            webView!!.clearHistory()
        }
    }

    //重写onKeyDown(keyCode, event)方法 改写物理按键 返回的逻辑
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isBackPressedOnce) {//连续点击两次退出应用
                // 第二次点击返回按钮，弹出 Toast 并退出应用
                Toast.makeText(this, getString(R.string.toast_back_twice_exit_mtv), Toast.LENGTH_LONG).show()
                finish()
                exitProcess(0)
            }else{
                // 第一次点击返回按钮，设置标志位为 true，然后启动延迟重置
                isBackPressedOnce = true
                resetBackButtonClicksNumberFlagAfterDelay()
            }
            if(!isBackButtonClicked){//防止重复点击
                // 第一次点击返回按钮，设置标志位为 true，然后启动延迟重置
                isBackButtonClicked = true
                resetBackButtonClickFlagAfterDelay()
            }else{
                return true
            }
            if(!webView!!.canGoBack()){ //history中已经是最后一个页面了
                promptUserForAction()
                return true
            }
            val webViewUrl = webView!!.url
            if(containsSpecificValue(webViewUrl!!)){//已经是最后一个页面了，不能go back
                promptUserForAction()
            }else{//history中还有页面能go back
                webView!!.goBack()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun loadUrl(url: String?) {
        webView!!.webViewClient = object : BaseWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                webView!!.loadUrl(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    (view as? BaseWebView)?.let { inject(it, url) } //注入JsBridge
                }
            }

            override fun onPageFinished(view: WebView?, url: String) {
                super.onPageFinished(view, url)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {//注入JsBridge
                    (view as? BaseWebView)?.let { inject(it, url) }
                }
            }
        }

        //添加进度条
        LoadView.initDialog(this)
        var isFirstLoad = true //第一次加载时显示进度条

        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if(isFirstLoad){ //第一次加载时显示进度条
                    if (newProgress == 100) {
                        LoadView.stopLoading()
                        isFirstLoad = false //第一次加载完成
                    } else {
                        LoadView.showLoading(view.context, newProgress.toString() + "")
                        if (newProgress == 100) {
                            LoadView.stopLoading()
                        }
                    }
                }
                super.onProgressChanged(view, newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooser = filePathCallback

                if(fileChooserParams?.isCaptureEnabled == true){//直接调用相机拍照
                    takePhoto()
                    return true
                }
1
                //弹出拍照或者进行文件选择对话框，让用户来决定是拍照还是选择相片
                var selectFile = 0
                var filetypes = fileChooserParams?.acceptTypes
                if (filetypes != null){
                    if (filetypes[0].isNotEmpty() && filetypes[0] == "image/*") {
                        selectFile = 1
                    }
                }

                if((fileChooserParams?.isCaptureEnabled == true)){
                    takePhoto()
                }
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
        // 设置 PopupWindow 点击事件监听
        popupWindow.setTouchInterceptor(fun(_: View, event: MotionEvent): Boolean {
            return if (event.action == MotionEvent.ACTION_OUTSIDE) {
                // 当点击空白处时，关闭 PopupWindow 删除fileChooser
                popupWindow.dismiss()
                fileChooser?.onReceiveValue(null)
                fileChooser = null
                true
            } else {
                false
            }
        })
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.showAtLocation(webView, Gravity.BOTTOM, 0, 0)
    }

    fun takePhoto() {
        if (checkCameraPermission()) {
            // 已经有相机权限，执行拍照操作
            takePic()
        } else {
            // 没有相机权限，请求权限
            requestCameraPermission()
        }
    }

    private fun takePic(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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
            fileChooser?.onReceiveValue(null)
            fileChooser = null
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

    private var fileDownloadListener =
        DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            when{
                mimeType.lowercase(Locale.getDefault()).contains("image") -> {
                    GeneralUtils.saveBase64ImageToGallery(this, null, url, mimeType)
                }
                mimeType.lowercase(Locale.getDefault()).contains("application") -> {
                    GeneralUtils.saveFileToDownload(this, url, userAgent, contentDisposition, mimeType)
                }
            }
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

    private fun promptUserForAction() {
        MessageDialog.build()
            .setTitle(getString(R.string.dialog_title_tip))
            .setMessage(getString(R.string.dialog_info_exit_app))
            .setCancelable(false)
            .setOkButton(getString(R.string.dialog_button_cancel)) { baseDialog, _ ->
                baseDialog.dismiss()
                resetBackButtonClickFlagAfterDelay()
                false
            }
            .setCancelButton(getString(R.string.dialog_button_ok)) { baseDialog, _ ->
                baseDialog.dismiss()
                exitProcess(0)
                false
            }
            .show()
    }


    private fun containsSpecificValue(input: String): Boolean { //判断Url是否包含特征字符串（即最后一页）
        return homeFeatureString.any { input.contains(it) }
    }

    private fun inject(webView: BaseWebView, url: String) { //注入JS方法
        if (jsInject == null) {
            jsInject = JsInject(webView)
        }
        webView.loadUrl("javascript:" + jsInject!!.injectJs())
    }

    private fun resetBackButtonClickFlagAfterDelay() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            isBackButtonClicked = false
        }, resetDelayMillis)
    }
    private fun resetBackButtonClicksNumberFlagAfterDelay() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            isBackPressedOnce = false
        }, 1000)
    }

    private fun checkCameraPermission(): Boolean {
        val permission = Manifest.permission.CAMERA
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        val permission = arrayOf(Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(this, permission, CAMERA_PERMISSION_REQUEST_CODE)
    }
}

