package com.androidhuman.example.simplegithub.ui.search

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.model.RepoSearchResponse
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.databinding.ActivitySearchBinding
import com.androidhuman.example.simplegithub.ui.repo.RepositoryActivity
import com.androidhuman.example.simplegithub.ui.search.SearchAdapter.ItemClickListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity(), ItemClickListener {
    internal lateinit var rvList: RecyclerView
    internal lateinit var progress: ProgressBar
    internal lateinit var tvMessage: TextView
    internal lateinit var menuSearch: MenuItem
    internal lateinit var searchView: SearchView
    internal val adapter: SearchAdapter by lazy {
        SearchAdapter().apply {
            setItemClickListener(this@SearchActivity)
        }
    }
    internal val api: GithubApi by lazy { provideGithubApi(this) }
    internal var searchCall: Call<RepoSearchResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        rvList = findViewById(R.id.rvActivitySearchList)
        progress = findViewById(R.id.pbActivitySearch)
        tvMessage = findViewById(R.id.tvActivitySearchMessage)

        with(rvList) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }
    }

    override fun onStop() {
        super.onStop()
        searchCall?.run { cancel() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu.findItem(R.id.menu_activity_search_query)
        searchView = (menuSearch.getActionView() as SearchView).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                // 검색어 입력 후 검색을 눌렀을때 콜백
                override fun onQueryTextSubmit(query: String): Boolean {
                    updateTitle(query)
                    hideSoftKeyboard()
                    collapseSearchView()
                    searchRepository(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
        }

        with(menuSearch) {
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    if ("" == searchView.query) {
                        finish()
                    }
                    return true
                }
            })
            expandActionView()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_activity_search_query == item.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        val intent = Intent(this, RepositoryActivity::class.java).apply {
            putExtra(RepositoryActivity.KEY_USER_LOGIN, repository.owner.login)
            putExtra(RepositoryActivity.KEY_REPO_NAME, repository.name)
        }
        startActivity(intent)
    }

    private fun searchRepository(query: String) {
        clearResults()
        hideError()
        showProgress()
        searchCall = api.searchRepository(query)
        searchCall!!.enqueue(object : Callback<RepoSearchResponse> {
            override fun onResponse(call: Call<RepoSearchResponse>,
                                    response: Response<RepoSearchResponse>) {
                hideProgress()
                val searchResult = response.body()
                if (response.isSuccessful && null != searchResult) {
                    // 검색 결과를 어댑터의 데이터에 갱신
                    with(adapter) {
                        setItems(searchResult.items)
                        notifyDataSetChanged()
                    }
                    if (0 == searchResult.totalCount) {
                        showError(getString(R.string.no_search_result))
                    }
                } else {
                    showError("Not successful: " + response.message())
                }
            }

            override fun onFailure(call: Call<RepoSearchResponse?>, t: Throwable) {
                hideProgress()
                showError(t.message)
            }
        })
    }

    private fun updateTitle(query: String) {
        supportActionBar?.run {
            subtitle = query
        }
    }

    private fun hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).run {
            hideSoftInputFromWindow(searchView.windowToken, 0)
        }
    }

    private fun collapseSearchView() {
        menuSearch.collapseActionView()
    }

    private fun clearResults() {
        with(adapter) {
            clearItems()
            notifyDataSetChanged()
        }
    }

    private fun showProgress() {
        progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progress.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }

    private fun hideError() {
        with(tvMessage) {
            text = ""
            visibility = View.GONE
        }
    }
}