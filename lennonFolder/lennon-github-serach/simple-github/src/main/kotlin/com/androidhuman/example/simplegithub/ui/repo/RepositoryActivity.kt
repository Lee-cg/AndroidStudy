package com.androidhuman.example.simplegithub.ui.repo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.databinding.ActivityRepositoryBinding
import com.androidhuman.example.simplegithub.rx.AutoClearedDisposable
import com.androidhuman.example.simplegithub.ui.GlideApp
import io.reactivex.android.schedulers.AndroidSchedulers
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RepositoryActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRepositoryBinding

    internal val api: GithubApi by lazy { provideGithubApi((this)) }
    //internal var repoCall: Call<GithubRepo>? = null
    //private val disposable = CompositeDisposable()
    private val disposables = AutoClearedDisposable(this)
    private val dateFormatInResponse = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
    private val dateFormatToShow = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepositoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycle.addObserver(disposables)

        try {
            val login = intent.getStringExtra(KEY_USER_LOGIN)
                    ?: throw IllegalArgumentException("No login info exists in extras")
            val repo = intent.getStringExtra(KEY_REPO_NAME)
                    ?: throw IllegalArgumentException("No repo info exists in extras")
            showRepositoryInfo(login, repo)
        } catch (e: IllegalArgumentException) {
            finish()
        }

    }

    private fun showRepositoryInfo(login: String, repoName: String) {

        disposables.add(api.getRepository(login, repoName)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe{showProgress()}
                .doOnError{hideProgress(false)}
                .doOnComplete{hideProgress(true)}
                .subscribe({ repo ->
                    GlideApp.with(this@RepositoryActivity)
                            .load(repo.owner.avatarUrl)
                            .into(binding.ivActivityRepositoryProfile)
                    binding.tvActivityRepositoryName.text = repo.fullName
                    binding.tvActivityRepositoryStars.text = resources
                            .getQuantityString(R.plurals.star, repo.stars, repo.stars)
                    if (repo.description.isNullOrBlank()) {
                        binding.tvActivityRepositoryDescription.setText(R.string.no_description_provided)
                    } else {
                        binding.tvActivityRepositoryDescription.text = repo.description
                    }
                    if (repo.language.isNullOrBlank()) {
                        binding.tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
                    } else {
                        binding.tvActivityRepositoryLanguage.text = repo.language
                    }
                    try {
                        val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
                        binding.tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
                    } catch (e: ParseException) {
                        binding.tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
                    }
                }) {
                    showError(it.message)
                })
//Rx 자바를 적용하기전 코드
//        showProgress()
//        repoCall = api.getRepository(login, repoName)
//        repoCall!!.enqueue(object : Callback<GithubRepo?> {
//            override fun onResponse(call: Call<GithubRepo?>, response: Response<GithubRepo?>) {
//                hideProgress(true)
//                val repo = response.body()
//                if (response.isSuccessful && null != repo) {
//                    GlideApp.with(this@RepositoryActivity)
//                            .load(repo.owner.avatarUrl)
//                            .into(binding.ivActivityRepositoryProfile)
//                    binding.tvActivityRepositoryName.text = repo.fullName
//                    binding.tvActivityRepositoryStars.text = resources
//                            .getQuantityString(R.plurals.star, repo.stars, repo.stars)
//                    if (null == repo.description) {
//                        binding.tvActivityRepositoryDescription.setText(R.string.no_description_provided)
//                    } else {
//                        binding.tvActivityRepositoryDescription.text = repo.description
//                    }
//                    if (null == repo.language) {
//                        binding.tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
//                    } else {
//                        binding.tvActivityRepositoryLanguage.text = repo.language
//                    }
//                    try {
//                        val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
//                        binding.tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
//                    } catch (e: ParseException) {
//                        binding.tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
//                    }
//                } else {
//                    showError("Not successful: " + response.message())
//                }
//            }
//
//            override fun onFailure(call: Call<GithubRepo?>, t: Throwable) {
//                hideProgress(false)
//                showError(t.message)
//            }
//        })
    }

    private fun showProgress() {
        binding.llActivityRepositoryContent.visibility = View.GONE
        binding.pbActivityRepository.visibility = View.VISIBLE
    }

    private fun hideProgress(isSucceed: Boolean) {
        binding.llActivityRepositoryContent.visibility = if (isSucceed) View.VISIBLE else View.GONE
        binding.pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(binding.tvActivityRepositoryMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }

    companion object {
        const val KEY_USER_LOGIN = "user_login"
        const val KEY_REPO_NAME = "repo_name"
    }
}