package com.tinyverse.tvs.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.tinyverse.tvs.R
import com.tinyverse.tvs.databinding.FragmentMinutiaeBinding
import io.reactivex.disposables.CompositeDisposable
import com.tinyverse.tvs.BuildConfig

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MinutiaeFragment : CameraXFragment() {


    private var callBack: CallBack? = null
    private var disposable = CompositeDisposable()
    private var fragmentBinding: FragmentMinutiaeBinding?=null

    private var processingImage:AtomicBoolean = AtomicBoolean(false)
    var openCvLoaded:AtomicBoolean = AtomicBoolean(false)



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentBinding = FragmentMinutiaeBinding.inflate(inflater, container, false)
        lensFacing = CameraSelector.LENS_FACING_BACK
        return fragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentBinding?.buttonTakePicture?.setOnClickListener {
            takePicture()
        }

        fragmentBinding?.autoFocus?.setOnClickListener {
            enableAutoFocus(!isAutoFocusEnable)
            fragmentBinding?.autoFocus?.text = if(isAutoFocusEnable) getString(R.string.disable_autofocus) else getString(R.string.enable_autofocus)
        }

        fragmentBinding?.layoutMinutiae?.buttonBackMinutiae?.setOnClickListener {
            fragmentBinding?.layoutButtons?.visibility = View.VISIBLE
            fragmentBinding?.layoutMinutiae?.root?.visibility = View.GONE
        }

        if(savedInstanceState == null){
            //Enable Autofocus by default, to focus the center of the screen and get a better finger image
           enableAutoFocus(true)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is CallBack) {
            callBack = activity
        }
    }

    override fun onDetach() {
        callBack = null
        super.onDetach()
    }


    override fun onResume() {
        fragmentBinding?.autoFocus?.text = if(isAutoFocusEnable) "Disable Focus" else "Enable Focus"
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        if (!disposable.isDisposed) {
            disposable.dispose();
        }
        fragmentBinding = null
        super.onDestroyView()
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Events from camera2 fragment
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    override val cameraPreview: PreviewView
        get(){
            return fragmentBinding?.cameraPreview!!
        }

    override val requestedPermissions: ArrayList<String>
        get() {
            return ArrayList<String>()
        }

    override fun onPermissionResult(
        permissionsGranted: ArrayList<String>,
        permissionsDenied: ArrayList<String>,
        permissionsRevoked: ArrayList<String>
    ) {

    }

    override fun getApplicationId(): String {
        return BuildConfig.APPLICATION_ID
    }

    override fun onPictureTaken(image: ImageProxy) {
    }

    override fun onImageProxy(image: ImageProxy) {
        image.close()
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    interface CallBack {
        fun onError()
    }

    companion object {
        private val TAG = MinutiaeFragment::class.java.simpleName

        fun newInstance(): MinutiaeFragment {
            return MinutiaeFragment()
        }
    }
}
