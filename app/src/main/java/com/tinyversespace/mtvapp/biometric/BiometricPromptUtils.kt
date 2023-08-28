/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tinyversespace.mtvapp.biometric

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.core.web.CallbackBean
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv
import com.tinyversespace.mtvapp.utils.GeneralUtils

// Since we are using the same methods in more than one Activity, better give them their own file.
object BiometricPromptUtils {
    private const val TAG = "BiometricPromptUtils"
    fun createBiometricPrompt(
        requestCode: String,
        activity: AppCompatActivity,
        processSuccess: (BiometricPrompt.AuthenticationResult, String) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                Log.d(TAG, "errCode is $errCode and errString is: $errString")
                callbackJs(activity, requestCode, -2, errString as String)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "Biometric authentication failed for unknown reason.")
                callbackJs(activity, requestCode, -1, "Biometric authentication failed for unknown reason.")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication was successful")
                processSuccess(result, requestCode)
            }
        }
        return BiometricPrompt(activity, executor, callback)
    }

    fun createPromptInfo(activity: AppCompatActivity): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(activity.getString(R.string.prompt_info_title))
//            setSubtitle(activity.getString(R.string.prompt_info_subtitle))
            setDescription(activity.getString(R.string.prompt_info_description))
            setConfirmationRequired(false)
            setNegativeButtonText(activity.getString(R.string.prompt_info_use_app_password))
        }.build()

    fun callbackJs(activity: AppCompatActivity, requestCode: String, code: Int, result: String){
        var message = activity.getString(R.string.prompt_info_bio_verify_error)
        val data: Any = result
        val isDelete = false
        val callback = JsCallMtv.requestCodeMap[requestCode]
        if(callback != null){
            message = "$message: $data"
            callback.success(CallbackBean(code, message, data), isDelete)
        }
        GeneralUtils.showToast(activity, message, 5 * 1000)
        activity.finish()
    }
}