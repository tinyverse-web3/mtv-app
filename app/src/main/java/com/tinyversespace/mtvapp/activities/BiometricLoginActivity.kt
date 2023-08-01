package com.tinyversespace.mtvapp.activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.core.web.CallbackBean
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.biometric.AppUser
import com.tinyversespace.mtvapp.biometric.BiometricPromptUtils
import com.tinyversespace.mtvapp.biometric.CIPHERTEXT_WRAPPER
import com.tinyversespace.mtvapp.biometric.CryptographyManager
import com.tinyversespace.mtvapp.biometric.SHARED_PREFS_FILENAME
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv
import com.tinyversespace.mtvapp.utils.GeneralUtils


class BiometricLoginActivity : AppCompatActivity() {
    private val TAG = "BiometricLogin"
    private lateinit var cryptographyManager: CryptographyManager
    private var activityRequestCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRequestCode = intent.getStringExtra("request_code")!!
        showBiometricPromptForEncryption()
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = getString(R.string.secret_key_name)
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(activityRequestCode,this, ::encryptAndStoreServerToken)
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }else{
            GeneralUtils.showToast(this, "用户无法进行身份验证，因为没有注册生物识别或设备凭据。", 15 * 1000)//15秒
            finish()
        }
    }

    private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult, requestCode: String) {
        authResult.cryptoObject?.cipher?.apply {
            AppUser.fakeToken?.let { token ->
                Log.d(TAG, "The token from server is $token")
                val encryptedServerTokenWrapper = cryptographyManager.encryptData(token, this)
                cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                    encryptedServerTokenWrapper,
                    applicationContext,
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                )
                val callback = JsCallMtv.requestCodeMap[requestCode]
                if(callback != null){
                    val message = "生物识别设置成功"
                    val data: Any = "success"
                    val isDelete = false
                    callback.success(CallbackBean(0, message, data), isDelete)
                }
            }
        }
        finish()
    }


}