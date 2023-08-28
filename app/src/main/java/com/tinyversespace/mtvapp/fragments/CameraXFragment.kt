package com.tinyversespace.mtvapp.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.Camera2CameraInfoImpl
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.utils.PermissionUtils
import com.tinyversespace.mtvapp.utils.permission.PermissionStatus
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
abstract class CameraXFragment : Fragment() {

    private var onPhotoTakenListener: OnPhotoTakenListener? = null


    private var isAutoCameraPermissionRevokedHandled:Boolean = true

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            onCallbackRequestPermissions(results)
        }

    private val registerActivitySettingsForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onCallbackRequestActivitySettings(result)
        }
    
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId: Int = -1
    private var cameraId: Int = -1
    protected var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    protected var isAutoFocusEnable = false
    protected var isTorchEnable = false


    //abstract val callbackFrameProcessor: FrameProcessor
    abstract val cameraPreview: PreviewView
    abstract val requestedPermissions:ArrayList<String>

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraXFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.

        checkPermissions(requestedPermissions)

        Handler().postDelayed(Runnable {
           enableAutoFocus(isAutoFocusEnable)
            enableTorch(isTorchEnable)
        }, 300)

    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        displayManager.unregisterDisplayListener(displayListener)
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(savedInstanceState!=null){
            if(savedInstanceState.containsKey(KEY_AUTO_FOCUS)){
                isAutoFocusEnable = savedInstanceState.getBoolean(KEY_AUTO_FOCUS)
            }
            if(savedInstanceState.containsKey(KEY_IS_TORCH_ENABLE)){
                isTorchEnable = savedInstanceState.getBoolean(KEY_IS_TORCH_ENABLE)
            }

            if (savedInstanceState.containsKey(KEY_AUTO_HANDLE_CAMERA_PERMISSION_REVOKED)) {
                isAutoCameraPermissionRevokedHandled = savedInstanceState.getBoolean(
                    KEY_AUTO_HANDLE_CAMERA_PERMISSION_REVOKED, true)
            }
        }

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // Determine the output directory


        /*// Wait for the views to be properly laid out
        cameraPreview.post {

            // Keep track of the display in which this view is attached
            displayId = cameraPreview.display.displayId

            // Set up the camera and its use cases
            setUpCamera()
        }*/

        val scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val maxZoomRatio = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio
                val minZoomRatio = camera?.cameraInfo?.zoomState?.value?.minZoomRatio

                val zoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio
                Log.d(TAG, "Max zoom ratio: $maxZoomRatio")
                Log.d(TAG, "Min zoom ratio: $minZoomRatio")
                Log.d(TAG, "Zoom ratio: $zoomRatio")

                zoomRatio?.let {
                    val scale = it * detector?.scaleFactor!!
                    camera?.cameraControl?.setZoomRatio(scale)
                    //camera!!.cameraControl.setLinearZoom(1F)
                }
                return true
            }

        })

        cameraPreview.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_AUTO_FOCUS, isAutoFocusEnable)
        outState.putBoolean(KEY_IS_TORCH_ENABLE, isTorchEnable)
        outState.putBoolean(KEY_AUTO_HANDLE_CAMERA_PERMISSION_REVOKED, isAutoCameraPermissionRevokedHandled)
        super.onSaveInstanceState(outState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPhotoTakenListener) {
            onPhotoTakenListener = context
        } else {
            throw RuntimeException("$context must implement OnPhotoTakenListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        onPhotoTakenListener = null
    }

    interface OnPhotoTakenListener {
        fun onPhotoTaken(photoPath: Uri)
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    /** Declare and bind preview, capture and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi", "UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { cameraPreview.display.getRealMetrics(it) }

        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = cameraPreview.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraFilter = CameraFilter { cameraInfos ->
            val filteredCameraInfos = java.util.ArrayList<CameraInfo>()

            for (cameraInfo in cameraInfos) {
                if(cameraInfo is Camera2CameraInfoImpl){
                    cameraInfo.cameraId
                    if (cameraInfo.cameraId.equals(cameraId.toString())) {
                        filteredCameraInfos.add(cameraInfo)
                    }
                }
            }
            filteredCameraInfos
        }

        val cameraSelector:CameraSelector
        if(cameraId>-1){
            cameraSelector = CameraSelector.Builder().addCameraFilter (cameraFilter).build()
        } else {
            cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        }



        // Preview
        val previewBuilder = Preview.Builder()
        previewBuilder
                // We request aspect ratio but no resolution
               .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .setHighResolutionDisabled(false)

        preview = previewBuilder.build()

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
            .setJpegQuality(100)
            .setHighResolutionDisabled(false)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()



        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            val useCases = ArrayList<UseCase>()
            imageCapture?.let { useCases.add(it) }

            if(useCases.isNotEmpty()) {
                val useCasesArray = useCases.toTypedArray()
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, *useCasesArray)
            } else {
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview)
            }
            // val supportedQualities = QualitySelector.getSupportedQualities(camera!!.cameraInfo)
            camera!!.cameraControl.setZoomRatio(2.0f)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(cameraPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }


    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }



    private fun enableTorch(enable: Boolean){
        isTorchEnable = enable
        camera?.cameraControl?.enableTorch(enable)
    }

    fun torchState(): LiveData<Int>? {
        return camera?.cameraInfo?.torchState
    }

    fun takePicture(){
        val photoFile = createOutputFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 图像保存成功
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    savePhotoToGallery(savedUri)
                    // 在此处处理保存成功后的操作，例如显示预览图或通知用户保存成功
                    onPhotoTakenListener?.onPhotoTaken(savedUri)
                }
                override fun onError(exception: ImageCaptureException) {
                    // 图像保存失败
                    // 在此处处理保存失败后的操作，例如显示错误提示或通知用户保存失败
                    Log.e(TAG, "Photo capture failed: ${exception.message}")
                    Toast.makeText(requireContext(), getString(R.string.toast_capture_photo_failed), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    abstract fun onPictureTaken(image: ImageProxy)
    abstract fun onImageProxy(image: ImageProxy)


    private fun savePhotoToGallery(photoUri: Uri) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photoUri.toFile().name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
        }

        val contentResolver: ContentResolver = requireContext().contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            try {
                val outputStream = contentResolver.openOutputStream(imageUri)
                outputStream?.use {
                    val bitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                    } else {
                        val source = ImageDecoder.createSource(contentResolver, photoUri)
                        ImageDecoder.decodeBitmap(source)
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                //Toast.makeText(requireContext(), "Photo saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                //Toast.makeText(requireContext(), "Failed to save photo to gallery", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            //Toast.makeText(requireContext(), "Failed to create image file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)
        return File(storageDir, fileName)
    }



    fun enableAutoFocus(enable: Boolean){
        if(enable) {
            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    cameraPreview.width.toFloat(), cameraPreview.height.toFloat())
            val centerWidth = cameraPreview.width.toFloat() / 2
            val centerHeight = cameraPreview.height.toFloat() / 2
            //create a point on the center of the view
            val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
            try {
                camera?.cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                                autoFocusPoint,
                                FocusMeteringAction.FLAG_AF
                        ).apply {
                            //auto-focus every 1 seconds
                            setAutoCancelDuration(2, TimeUnit.SECONDS)
                        }.build()
                )
            } catch (e: CameraInfoUnavailableException) {
                Log.d("ERROR", "cannot access camera", e)
            }

        } else {
            camera?.cameraControl?.cancelFocusAndMetering()
        }
        isAutoFocusEnable = enable
    }



    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Permissions
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    protected fun hasCameraPermission():Boolean{
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Permissions
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onCallbackRequestPermissions(results :Map<String, Boolean>) {
        if(results.isNotEmpty()) {
            if(isAdded && isResumed) {
                checkPermissions(requestedPermissions)
            }
        }
    }

    private fun onCallbackRequestActivitySettings(result :androidx.activity.result.ActivityResult) {
       // checkPermissions(requestedPermissions)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermissions(permissions:ArrayList<String> = ArrayList()) {
        //request permission
        val hasPermissionCamera = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasPermissionCamera && !permissions.contains(Manifest.permission.CAMERA)) {
            permissions.add(Manifest.permission.CAMERA)
        }

        val checkPermissions = PermissionUtils.checkPermissions(requireActivity(), permissions)
        val permissionsGranted =
            PermissionUtils.getPermissions(checkPermissions, PermissionStatus.GRANTED)
        val permissionsRevoked =
            PermissionUtils.getPermissions(checkPermissions, PermissionStatus.REVOKED)
        val permissionsDenied =
            PermissionUtils.getPermissions(checkPermissions, PermissionStatus.DENIED)

        onPermissionResult(permissionsGranted, permissionsDenied, permissionsRevoked)

        if (PermissionUtils.allPermissionsGranted(checkPermissions)) {
            //Dont do anything
            cameraPreview.post {

                // Keep track of the display in which this view is attached
                displayId = cameraPreview.display.displayId

                // Set up the camera and its use cases
                setUpCamera()
            }
        } else {

            if (permissionsRevoked.isNotEmpty()) {
                for(permission in permissionsRevoked) {
                    when (permission) {
                        Manifest.permission.CAMERA->{
                            if(isAutoCameraPermissionRevokedHandled) {
                                val builder = AlertDialog.Builder(context)
                                builder.setTitle(getString(R.string.permission_camera_title))
                                    .setMessage(R.string.permission_camera_rationale)
                                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                                        if (isAdded && isResumed) {
                                            checkPermissions(requestedPermissions)
                                        }
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton(R.string.button_go_back) { dialog, which ->
                                        if (isAdded && isResumed) {
                                            requireActivity().onBackPressed()
                                        }
                                        dialog.dismiss()
                                    }
                                    .setNeutralButton(R.string.button_go_settings) { dialog, which ->
                                        if (isAdded && isResumed) {
                                            //Go to settings
                                            registerActivitySettingsForResult?.launch(
                                                Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:" + getApplicationId())
                                                )
                                            )
                                        }
                                        dialog.dismiss()
                                    }
                                    .show()
                            }
                        }
                    }
                }
            } else {
                PermissionUtils.requestPermissionsDeniedToUser(requireContext(), permissionsDenied, permissionLauncher)
            }
        }
    }

    abstract fun onPermissionResult(permissionsGranted: ArrayList<String>, permissionsDenied: ArrayList<String>, permissionsRevoked: ArrayList<String>)
    abstract fun getApplicationId():String

    val rotation:Int
    get() {
        return getRotation(lensFacing)
    }

    private fun getRotation(lensPosition: Int = CameraSelector.LENS_FACING_BACK):Int{
        //return cameraPreview.display.rotation
        try {
            if (camera != null) {
                val sensorRotationDegrees = camera?.cameraInfo?.sensorRotationDegrees!!
                val rotation = cameraPreview.display.rotation

                var degrees = 0
                when (rotation) {
                    Surface.ROTATION_0 -> degrees = 0
                    Surface.ROTATION_90 -> degrees = 90
                    Surface.ROTATION_180 -> degrees = 180
                    Surface.ROTATION_270 -> degrees = 270
                }
                var result: Int
                if (lensPosition == CameraSelector.LENS_FACING_FRONT) {
                    result = (sensorRotationDegrees + degrees - 360) % 360
                    result = (360 + result) % 360  // compensate the mirror
                } else {  // back-facing
                    result = (sensorRotationDegrees - degrees + 360) % 360
                }
                return result
            } else {
                return cameraPreview.display.rotation
            }
        }catch (e:Exception){
            return 0
        }
    }

    companion object {

        private val TAG = CameraXFragment::class.java.simpleName
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        const val KEY_AUTO_FOCUS = "KEY_AUTO_FOCUS"
        const val KEY_IS_TORCH_ENABLE = "KEY_IS_TORCH_ENABLE"
        private const val KEY_AUTO_HANDLE_CAMERA_PERMISSION_REVOKED = "KEY_AUTO_HANDLE_CAMERA_PERMISSION"

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
                File(baseFolder, SimpleDateFormat(format, Locale.US)
                        .format(System.currentTimeMillis()) + extension)
    }
}
