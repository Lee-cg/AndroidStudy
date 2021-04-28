package com.androidhuman.example.simplegithub.ui.signin

import android.arch.lifecycle.Lifecycle
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.androidhuman.example.simplegithub.BuildConfig
import com.androidhuman.example.simplegithub.api.model.GithubAccessToken
import com.androidhuman.example.simplegithub.api.provideAuthApi
import com.androidhuman.example.simplegithub.data.AuthTokenProvider
import com.androidhuman.example.simplegithub.databinding.ActivitySignInBinding
import com.androidhuman.example.simplegithub.rx.AutoClearedDisposable
import com.androidhuman.example.simplegithub.ui.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * p.240~244 코틀린 익스텐션은 구글이 버리는것 같아서 적용하지 않았음. 대신 뷰 바인딩을 씁시다.
 */
class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding

    //internal var accessTokenCall: Call<GithubAccessToken>? = null
   // private val disposable = CompositeDisposable()

    private val disposable = AutoClearedDisposable(this)

    // 사용자 인증 토큰이 있는지 여부 확인
    private val api by lazy { provideAuthApi() }
    private val authTokenProvider by lazy { AuthTokenProvider(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycle.addObserver(disposable)

        binding.btnActivitySignInStart.setOnClickListener(View.OnClickListener {
            val authUri = Uri.parse("https://github.com/login/oauth/authorize?client_id=${BuildConfig.GITHUB_CLIENT_ID}")

            //authUri : https://github.com/login/oauth/authorize?client_id=cd40ab31a8ad344cd4e7

            //웹 페이지 구현이 복잡한 커스텀 웹뷰 대신 크롬 커스텀 탭을 사용
           CustomTabsIntent.Builder().build().run{
                this.launchUrl(this@SignInActivity, authUri)
            }
        })

        // 있으면 메인 액티비티로 이동
        if (null != authTokenProvider.token) {
            launchMainActivity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showProgress()

        // uri : lennonsimplegithub://authorize?code=fa7fca5be85c7aa167f3
        try {
            val uri = intent.data ?: throw IllegalArgumentException("No data exists")
            val code = uri.getQueryParameter("code")
                    ?: throw IllegalStateException("No code exists")
            getAccessToken(code)
        } catch (e: IllegalArgumentException) {
            //데이터가 없을 경우 SignInActivity를 finish()해서 다시 로그인 유도
            finish()
        }
    }

    private fun getAccessToken(code: String) {
        disposable.add(api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET,
                code)

                .map { it.accessToken } //api를 통해 받은 응답에서 액세스 토큰만 추출
                .observeOn(AndroidSchedulers.mainThread()) //이 이후에 실행되는 코드는 안드로이드 메인스레드에서 실행
                .doOnSubscribe{showProgress()} // 구독할 때 수행할 작업
                .doOnTerminate{hideProgress()} // 스트림이 종료될 때 수행할 작업
                .subscribe({token -> // 옵저버블 구독
                    authTokenProvider.updateToken(token)
                    launchMainActivity()
                }, {
                    showError(it)
                })
        )
//Rx 자바를 쓰기전 코드
//        showProgress()
//
//        // 액세스 토큰을 요청하는 REST API
//        accessTokenCall = api.getAccessToken(
//                BuildConfig.GITHUB_CLIENT_ID,
//                BuildConfig.GITHUB_CLIENT_SECRET, code)
//
//        // 비동기 방식으로 엑세스 토큰 요청
//        // 앞에서 api 호출에 필요한 객체를 받았으므로 null일리 없다!
//        accessTokenCall?.enqueue(object : Callback<GithubAccessToken?> {
//            override fun onResponse(call: Call<GithubAccessToken?>,
//                                    response: Response<GithubAccessToken?>) {
//                hideProgress()
//                val token = response.body()
//                if (response.isSuccessful && null != token) {
//                    authTokenProvider.updateToken(token.accessToken)
//                    launchMainActivity()
//                } else {
//                    showError(IllegalStateException(
//                            "Not successful: " + response.message()))
//                }
//            }
//
//            override fun onFailure(call: Call<GithubAccessToken?>, t: Throwable) {
//                hideProgress()
//                showError(t)
//            }
//        })
    }

    private fun showProgress() {
        binding.btnActivitySignInStart.visibility = View.GONE
        binding.pbActivitySignIn.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.btnActivitySignInStart.visibility = View.VISIBLE
        binding.pbActivitySignIn.visibility = View.GONE
    }

    private fun showError(throwable: Throwable) {
        Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
    }

    private fun launchMainActivity() {
        startActivity(Intent(
                this@SignInActivity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}