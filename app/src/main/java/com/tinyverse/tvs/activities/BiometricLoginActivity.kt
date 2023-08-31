package com.tinyverse.tvs.activities

import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.core.web.CallbackBean
import com.kongzue.dialogx.dialogs.MessageDialog
import com.tinyverse.tvs.R
import com.tinyverse.tvs.biometric.AppUser
import com.tinyverse.tvs.biometric.BiometricPromptUtils
import com.tinyverse.tvs.biometric.CIPHERTEXT_WRAPPER
import com.tinyverse.tvs.biometric.CryptographyManager
import com.tinyverse.tvs.biometric.SHARED_PREFS_FILENAME
import com.tinyverse.tvs.jsbridge.JsCallMtv
import com.tinyverse.tvs.utils.GeneralUtils
import com.tinyverse.tvs.utils.language.MultiLanguageService
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MultiLanguageService.changeContextLocale(newBase))
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = GeneralUtils.secretKeyName
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
            GeneralUtils.showToast(this, getString(R.string.toast_no_bio_registered), 15 * 1000)//15秒
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
                    val message = getString(R.string.prompt_info_bio_set_ok)
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
                val secretKeyName = GeneralUtils.secretKeyName
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
                GeneralUtils.showToast(this, getString(R.string.toast_no_bio_registered), 15 * 1000)//15秒
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
                    val message = getString(R.string.prompt_info_bio_verify_ok)
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
            .setTitle(getString(R.string.prompt_info_bio_not_configured))
            .setMessage(getString(R.string.prompt_info_bio_unlock_to_configured))
            .setCancelable(true)
            .setOkButton(getString(R.string.dialog_button_confirm)) { baseDialog, _ ->
                baseDialog.dismiss()
                val callback = JsCallMtv.requestCodeMap[activityRequestCode]
                if(callback != null){
                    val message = getString(R.string.prompt_info_bio_not_configured)
                    val data: Any = getString(R.string.prompt_info_bio_not_configured)
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
            .setTitle(getString(R.string.credentials_have_changed))
            .setMessage(getString(R.string.prompt_info_bio_unlock_to_reconfigured))
            .setCancelable(true)
            .setOkButton(getString(R.string.dialog_button_confirm)) { baseDialog, _ ->
                baseDialog.dismiss()
                val callback = JsCallMtv.requestCodeMap[activityRequestCode]
                if(callback != null){
                    val message = getString(R.string.prompt_info_app_bio_reconfigure)
                    val data: Any = getString(R.string.prompt_info_app_bio_reconfigure)
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
            var message = getString(R.string.prompt_info_bio_have_configured)
            var data: Any = getString(R.string.prompt_info_bio_have_configured)
            val isDelete = false
            if( ciphertextWrapper != null){
                callback.success(CallbackBean(0, message, data), isDelete)
            }else{
                message = getString(R.string.prompt_info_bio_not_configured)
                data = getString(R.string.prompt_info_bio_not_configured)
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