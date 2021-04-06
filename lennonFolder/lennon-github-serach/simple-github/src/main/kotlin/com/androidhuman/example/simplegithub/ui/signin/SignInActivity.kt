package com.androidhuman.example.simplegithub.ui.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.androidhuman.example.simplegithub.BuildConfig
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubAccessToken
import com.androidhuman.example.simplegithub.api.provideAuthApi
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import com.androidhuman.example.simplegithub.ui.main.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * p.240~244 코틀린 익스텐션은 구글이 버리는것 같아서 적용하지 않았음. 대신 뷰 바인딩을 씁시다.
 */
class SignInActivity : AppCompatActivity() {
    internal lateinit var btnStart: Button
    internal lateinit var progress: ProgressBar
    internal var accessTokenCall: Call<GithubAccessToken>? = null

    // 사용자 인증 토큰이 있는지 여부 확인
    internal val api by lazy { provideAuthApi() }
    internal val authTokenProvider by lazy { AuthTokenProvider(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        btnStart = findViewById(R.id.btnActivitySignInStart)
        progress = findViewById(R.id.pbActivitySignIn)
        btnStart.setOnClickListener(View.OnClickListener {
            val authUri = Uri.Builder().scheme("https").authority("github.com")
                    .appendPath("login")
                    .appendPath("oauth")
                    .appendPath("authorize")
                    .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
                    .build()
            //authUri : https://github.com/login/oauth/authorize?client_id=cd40ab31a8ad344cd4e7

            //웹 페이지 구현이 복잡한 커스텀 웹뷰 대신 크롬 커스텀 탭을 사용
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@SignInActivity, authUri)
        })

        // 있으면 메인 액티비티로 이동
        if (null != authTokenProvider.token) {
            launchMainActivity()
        }
    }

    override fun onStop() {
        super.onStop()
        // 액티비티가 화면에서 사라지는 시점에 api 호출 객체가 생성되어 있다면 api 요청 취소
        // (?.는 null이 아니면 아래의 run 실행, null이면 null 반환)
        accessTokenCall?.run{ cancel() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showProgress()

        // uri : lennonsimplegithub://authorize?code=fa7fca5be85c7aa167f3
        val uri = intent.data ?: throw IllegalArgumentException("No data exists")
        val code = uri.getQueryParameter("code")
                ?: throw IllegalStateException("No code exists")
        getAccessToken(code)
    }

    private fun getAccessToken(code: String) {
        showProgress()

        // 액세스 토큰을 요청하는 REST API
        accessTokenCall = api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET, code)

        // 비동기 방식으로 엑세스 토큰 요청
        // 앞에서 api 호출에 필요한 객체를 받았으므로 null일리 없다!
        accessTokenCall!!.enqueue(object : Callback<GithubAccessToken?> {
            override fun onResponse(call: Call<GithubAccessToken?>,
                                    response: Response<GithubAccessToken?>) {
                hideProgress()
                val token = response.body()
                if (response.isSuccessful && null != token) {
                    authTokenProvider.updateToken(token.accessToken)
                    launchMainActivity()
                } else {
                    showError(IllegalStateException(
                            "Not successful: " + response.message()))
                }
            }

            override fun onFailure(call: Call<GithubAccessToken?>, t: Throwable) {
                hideProgress()
                showError(t)
            }
        })
    }

    private fun showProgress() {
        btnStart.visibility = View.GONE
        progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        btnStart.visibility = View.VISIBLE
        progress.visibility = View.GONE
    }

    private fun showError(throwable: Throwable) {
        Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
    }

    private fun launchMainActivity() {
        startActivity(Intent(
                this@SignInActivity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}