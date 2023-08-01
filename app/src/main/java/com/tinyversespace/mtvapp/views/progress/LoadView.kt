package com.tinyversespace.mtvapp.views.progress

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.tinyversespace.mtvapp.R

object LoadView {
    private val LOADERS = ArrayList<Dialog?>()
    private val VIEWS = ArrayList<WebViewCircleProgress>()
    private var dialog: Dialog? = null
    fun showLoading(context: Context, msg: String) {
        val avLoadingIndicatorView =   LayoutInflater.from(context).inflate(R.layout.view_loadview, null)
        val tvShow: TextView = avLoadingIndicatorView.findViewById<TextView>(R.id.tvShow)
        val circleProgress =
            avLoadingIndicatorView!!.findViewById<WebViewCircleProgress>(R.id.progress_bar)
        tvShow.text = "加载$msg%"
        if (dialog != null) {
            dialog!!.setContentView(avLoadingIndicatorView)
            dialog!!.setCanceledOnTouchOutside(false)
            LOADERS.add(dialog)
            VIEWS.add(circleProgress)
            if (!(context as Activity).isFinishing) {
                if (!dialog!!.isShowing) {
                    dialog!!.show()
                }
            }
        } else {
            Toast.makeText(context, "动画dialog初始化失败", Toast.LENGTH_LONG).show()
        }
    }

    fun initDialog(context: Context?) {
        dialog = Dialog(context!!, R.style.LoadingDialog)
    }

    @JvmOverloads
    fun stopLoading(type: Int = -1, dismissListener: DismissListener? = null) {
        for (i in LOADERS.indices) {
            val dialog = LOADERS[i]
            val circleProgress = VIEWS[i]
            if (type <= 0) {
                dismiss(dialog)
            } else if (type == WebViewCircleProgress.Companion.STATE_ERROR) {
                setCircleProgressState(
                    circleProgress,
                    dialog,
                    WebViewCircleProgress.Companion.STATE_ERROR,
                    dismissListener
                )
            } else {
                setCircleProgressState(
                    circleProgress,
                    dialog,
                    WebViewCircleProgress.Companion.STATE_FINISH,
                    dismissListener
                )
            }
        }
    }

    private fun setCircleProgressState(
        circleProgress: WebViewCircleProgress?,
        dialog: Dialog?,
        type: Int,
        dismissListener: DismissListener?
    ) {
        if (circleProgress != null) {
            circleProgress.finish(type)
            circleProgress.postDelayed(Runnable {
                if (dismissListener != null) {
                    dismiss(dialog)
                    dismissListener.dismiss()
                } else {
                    dismiss(dialog)
                }
            }, 600)
        } else {
            dismiss(dialog)
        }
    }

    private fun dismiss(dialog: Dialog?) {
        if (dialog != null && dialog.isShowing) {
            dialog.cancel()
        }
    }

    interface DismissListener {
        fun dismiss()
    }
}