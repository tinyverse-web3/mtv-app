package com.tinyverse.tvs.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.core.web.CallbackBean
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.tinyverse.tvs.R
import com.tinyverse.tvs.jsbridge.JsCallMtv


class GoogleLoginActivity : AppCompatActivity() {

  /* Google Play services requires us to provide the SHA-1 of our signing certificate so Google can
   create an OAuth2 client and API key for our app.click on Configure a project(link in ring).
   Enter project name, select Andorid, enter package name and SHA-1 certificate.*/

  private val TAG = "GoogleLoginActivity"

  private var mGoogleSignInClient: GoogleSignInClient? = null

  private var activityRequestCode: String = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityRequestCode = intent.getStringExtra("request_code")!!
    val gso =
      GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)//To request users' email
        // addresses create the GoogleSignInOptions object with the requestEmail option.
        .requestIdToken(getString(R.string.server_web_client_id))
        .requestEmail()
        .build()

    mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
  }

  override fun onStart() {
    super.onStart()
    val account =
      GoogleSignIn.getLastSignedInAccount(this)//check if a user has already signed in to your app with Google
    if(activityRequestCode == JsCallMtv.REQUEST_CODE_GOOGLE_SIGN_IN){
      login(account)
    }
    if(activityRequestCode == JsCallMtv.REQUEST_CODE_GOOGLE_SIGN_OUT){
      logout(this, account)
    }
  }


  private fun login(account: GoogleSignInAccount?){
    if (account != null) {
      try{
        mGoogleSignInClient?.signOut()
          ?.addOnCompleteListener(this, object : OnCompleteListener<Void> {
            override fun onComplete(p0: Task<Void>) {
              signIn()
            }
          })
      }catch (e: Exception){
        Log.e("TAG", "login:signOut failed and call signIn" + e.message)
        signIn()
      }
    } else {
      //If account == null, the user has not yet signed in to your app with Google.
      signIn()
    }
  }

  private fun logout(context: Context, account: GoogleSignInAccount?){
    signOut(context, account)
  }


  private fun signIn() {
    //By calling signIn()we launch the login screen using the Intent we get by calling
    // getSignInIntent method on the GoogleSignInClient object, and start the login by calling
    // startActivityForResult
    val intent =
      mGoogleSignInClient!!.signInIntent//The intent prompts the user to select a Google
    // account to sign in with.
    signInLauncher.launch(intent)
  }

  private fun signOut(context: Context, account: GoogleSignInAccount?){
    if(account != null){
      mGoogleSignInClient?.signOut()
        ?.addOnCompleteListener(this, object : OnCompleteListener<Void> {
          override fun onComplete(p0: Task<Void>) {
            val toast = Toast.makeText(context, "Login account is null.", Toast.LENGTH_LONG)
            toast.show()
            finish()
          }
        })
    }
  }


  //The GoogleSignInAccount object (account) contains information about the signed-in user,
  // such as the user's name ,email etc
  private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    // 处理登录结果，可以在这里处理返回的Intent
    if (result.resultCode == Activity.RESULT_OK) {
      val task =
        GoogleSignIn.getSignedInAccountFromIntent(result.data)
      try {
        val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
        returnLoginRes(activityRequestCode, account, "")
      } catch (e: ApiException) {
        // The ApiException status code indicates the detailed failure reason.
        // Please refer to the GoogleSignInStatusCodes class reference for more information.
        Log.e("TAG", "signInResult:failed code=" + e.statusCode)
        returnLoginRes(activityRequestCode, null, "google login, signInResult:failed code=" + e.statusCode)
      }
    } else {
      Log.e("TAG", "login activity result code: " + result.resultCode)
      returnLoginRes(activityRequestCode, null, "login activity result code: " + result.resultCode)
    }
  }


  private fun returnLoginRes(requestCode: String, account: GoogleSignInAccount?, errorMsg: String){
    val callback = JsCallMtv.requestCodeMap[requestCode]
    if(callback == null){
      val toast = Toast.makeText(this, "Login account is null.", Toast.LENGTH_LONG)
      toast.show()
      finish()
      return
    }
    var data: Any = ""
    val isDelete = false
    var message = ""
    var code = 0
    if (account != null) {
      message = getString(R.string.prompt_google_login_ok)
      val signInRes = HashMap<String, Any>()
      signInRes["IdToken"] = account.idToken.toString()
      signInRes["UserName"] = account.displayName.toString()
      signInRes["Email"] = account.email.toString()

      data = signInRes
      code = 0
    }
    if (!errorMsg.isNullOrEmpty()) {
      message = getString(R.string.prompt_google_login_failed)
      data = errorMsg
      code = -1
    }
    callback.success(CallbackBean(code, message, data), isDelete)
    finish()
  }

}
