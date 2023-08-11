package com.tinyversespace.mtvapp.activities

import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.core.web.CallbackBean
import com.kongzue.dialogx.dialogs.MessageDialog
import com.tinyversespace.mtvapp.R
import com.tinyversespace.mtvapp.biometric.AppUser
import com.tinyversespace.mtvapp.biometric.BiometricPromptUtils
import com.tinyversespace.mtvapp.biometric.CIPHERTEXT_WRAPPER
import com.tinyversespace.mtvapp.biometric.CryptographyManager
import com.tinyversespace.mtvapp.biometric.SHARED_PREFS_FILENAME
import com.tinyversespace.mtvapp.jsbridge.JsCallMtv
import com.tinyversespace.mtvapp.utils.GeneralUtils
import java.security.KeyStore
import javax.crypto.Cipher


class BiometricLoginActivity : AppCompatActivity() {
    private val TAG = "BiometricLogin"
    private var cryptographyManager: CryptographyManager = CryptographyManager()
    private var activityRequestCode: String = ""
    private lateinit var biometricPrompt: BiometricPrompt
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRequestCode = intent.getStringExtra("request_code")!!
        if(activityRequestCode == JsCallMtv.REQUEST_CODE_SET_UP_BIOMETRIC){ //设置生物识别
            showBiometricPromptForEncryption()
        }
        if(activityRequestCode == JsCallMtv.REQUEST_CODE_IS_BIOMETRIC_SET_UP) {//判断是否开启了生物识别
            isBiometricsSetUp()
        }
        if(activityRequestCode == JsCallMtv.REQUEST_CODE_BIOMETRIC_VERIFY) {//生物识别验证
            if (ciphertextWrapper != null) {
                showBiometricPromptForDecryption()
            } else {
                showEnableBiometricDialog()
            }
        }
    }

    /**
     * The logic is kept inside onResume instead of onCreate so that authorizing biometrics takes
     * immediate effect.
     */
    override fun onResume() {
        super.onResume()

        if (ciphertextWrapper != null) {
            if (AppUser.fakeToken == null) {
                showBiometricPromptForDecryption()
            } else {
                // The user has already logged in, so proceed to the rest of the app
                // this is a todo for you, the developer
            }
        }
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = getString(R.string.secret_key_name)
            var cipher: Cipher = try {
                cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            }catch (e: KeyPermanentlyInvalidatedException){
                e.printStackTrace()
                deleteKey(secretKeyName)
                cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            }
            val biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(
                    activityRequestCode,
                    this,
                    ::encryptAndStoreServerToken)
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

    // for biometric
    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK)
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val secretKeyName = getString(R.string.secret_key_name)
                var cipher: Cipher
                try {
                    cipher = cryptographyManager.getInitializedCipherForDecryption(
                        secretKeyName, textWrapper.initializationVector
                    )
                }catch (e: KeyPermanentlyInvalidatedException){
                    e.printStackTrace()
                    showReEnableBiometricDialog()//需要重新设置生物识别
                    return
                }
                biometricPrompt =
                    BiometricPromptUtils.createBiometricPrompt(
                        activityRequestCode,
                        this,
                        ::decryptServerTokenFromStorage
                    )
                val promptInfo = BiometricPromptUtils.createPromptInfo(this)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }else{
                GeneralUtils.showToast(this, "用户无法进行身份验证，因为没有注册生物识别或设备凭据。", 15 * 1000)//15秒
                finish()
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
        finish()
    }

    private fun showEnableBiometricDialog() {
        MessageDialog.build()
            .setTitle("生物识别未配置")
            .setMessage("请解锁后在设置页面进行配置")
            .setCancelable(true)
            .setOkButton("确定") { baseDialog, _ ->
                baseDialog.dismiss()
                val callback = JsCallMtv.requestCodeMap[activityRequestCode]
                if(callback != null){
                    val message = "应用生物识别未配置"
                    val data: Any = "应用生物识别未配置"
                    val isDelete = false
                    callback.success(CallbackBean(-3, message, data), isDelete)
                }
                finish()
                false
            }
            .show()
    }

    //for biometric
    private fun showReEnableBiometricDialog() {

        MessageDialog.build()
            .setTitle("设备生物识别凭据已经改变")
            .setMessage("请解锁应用后在设置页面重新进行配置")
            .setCancelable(true)
            .setOkButton("确定") { baseDialog, _ ->
                baseDialog.dismiss()
                val callback = JsCallMtv.requestCodeMap[activityRequestCode]
                if(callback != null){
                    val message = "应用生物识别需要重新配置"
                    val data: Any = "应用生物识别需要重新配置"
                    val isDelete = false
                    callback.success(CallbackBean(-3, message, data), isDelete)
                }
                finish()
                false
            }
            .show()
    }

    private fun isBiometricsSetUp(){
        val callback = JsCallMtv.requestCodeMap[activityRequestCode]
        if(callback != null){
            var message = "应用生物识别已配置"
            var data: Any = "应用生物识别已配置"
            val isDelete = false
            if( ciphertextWrapper != null){
                callback.success(CallbackBean(0, message, data), isDelete)
            }else{
                message = "应用生物识别未配置"
                data = "应用生物识别未配置"
                callback.success(CallbackBean(-3, message, data), isDelete)
            }
        }
        finish()
    }



    private fun deleteKey(alias: String){
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null) // Keystore must be loaded before it can be accessed
        try {
            keyStore.deleteEntry(alias)
        } catch (e: Exception) {
            // 处理异常，比如密钥不存在等情况
            e.printStackTrace()
        }
    }

}