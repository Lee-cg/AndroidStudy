package com.androidhuman.example.simplegithub.ui.search

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.ui.GlideApp
import java.util.*

/**
 * p.245 코틀린 익스텐션은 구글이 버리는것 같아서 적용하지 않았음. 대신 뷰 바인딩을 씁시다.
 */
class SearchAdapter : RecyclerView.Adapter<SearchAdapter.RepositoryHolder>() {
    private var items: MutableList<GithubRepo> = mutableListOf()
    private val placeholder = ColorDrawable(Color.GRAY)
    private var listener: ItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryHolder =
            RepositoryHolder(parent)


    override fun onBindViewHolder(holder: RepositoryHolder, position: Int) {
        items[position].let { repo ->
            with(holder.itemView) {
                GlideApp.with(context)
                        .load(repo.owner.avatarUrl)
                        .placeholder(placeholder)
                        .into(holder.ivProfile)

                holder.tvName.text = repo.fullName
                holder.tvLanguage.text = if (TextUtils.isEmpty(repo.language))
                    context.getText(R.string.no_language_specified)
                else
                    repo.language

                setOnClickListener {
                    if (null != listener) {
                        listener!!.onItemClick(repo)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int =
            items.size

    fun setItems(items: List<GithubRepo>) {
        // 인자로 받은 리스트의 형태를 어댑터 내부에서 사용하는 리스트 형태(내부자료 변경이 가능한 형태)로 변환해주어야 한단당
        this.items = items.toMutableList()
    }

    fun setItemClickListener(listener: ItemClickListener?) {
        this.listener = listener
    }

    fun clearItems() {
        items.clear()
    }

    class RepositoryHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repository, parent, false)) {
        var ivProfile: ImageView
        var tvName: TextView
        var tvLanguage: TextView

        init {
            ivProfile = itemView.findViewById(R.id.ivItemRepositoryProfile)
            tvName = itemView.findViewById(R.id.tvItemRepositoryName)
            tvLanguage = itemView.findViewById(R.id.tvItemRepositoryLanguage)
        }
    }

    interface ItemClickListener {
        fun onItemClick(repository: GithubRepo)
    }
}