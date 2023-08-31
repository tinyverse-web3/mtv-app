package com.tinyverse.tvs.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import com.ailiwean.core.Result
import com.ailiwean.core.view.style2.NBZxingView
import com.ailiwean.core.zxing.ScanTypeConfig
import com.google.android.cameraview.AspectRatio
import com.tinyverse.tvs.R

class QrcodeScanView @JvmOverloads constructor(

    context: Context,
    attributeSet: AttributeSet? = null,
    def: Int = 0
    ) : NBZxingView(context, attributeSet, def) {

        private lateinit var onCallBackStringMac: OnCallBackStringMac

        interface OnCallBackStringMac {
            fun stringMac(text: String)
        }

        fun setOnCallBackStringMac(onCallBackStringMac: OnCallBackStringMac) {
            this.onCallBackStringMac = onCallBackStringMac
        }


        override fun resultBack(content: Result) {
            if(content.text == null){
                content.text = content.toString()
            }
            onCallBackStringMac.stringMac(content.text)
            Toast.makeText(context, content.text, Toast.LENGTH_LONG).show()
        }

        /***
         * 返回扫码类型
         * 1 ScanTypeConfig.HIGH_FREQUENCY 高频率格式(默认)
         * 2 ScanTypeConfig.ALL  所有格式
         * 3 ScanTypeConfig.ONLY_QR_CODE 仅QR_CODE格式
         * 4 ScanTypeConfig.TWO_DIMENSION 所有二维码格式
         * 5 ScanTypeConfig.ONE_DIMENSION 所有一维码格式
         */
        override fun configScanType(): ScanTypeConfig {
            return ScanTypeConfig.ONLY_QR_CODE
        }

        fun toParse(string: String) {
            parseFile(string)
        }

        override fun provideAspectRatio(): AspectRatio {
            return AspectRatio.of(16, 9)
        }

        override fun resultBackFile(content: com.ailiwean.core.zxing.core.Result?) {
            if (content == null)
                Toast.makeText(context,  context.getString(R.string.qrcode_scan_content_not_scanned), Toast.LENGTH_SHORT).show()
            else {
                onCallBackStringMac.stringMac(content.toString())
                Toast.makeText(context, content.text, Toast.LENGTH_SHORT).show()
            }

        }

        override fun isSupportAutoZoom(): Boolean {
            return false
        }

}