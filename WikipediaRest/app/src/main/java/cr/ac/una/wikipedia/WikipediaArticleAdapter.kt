package cr.ac.una.wikipedia

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide

class WikipediaArticleAdapter(
    private val context: Context,
    private val articles: List<WikipediaArticle>
) : BaseAdapter() {

    override fun getCount(): Int = articles.size

    override fun getItem(position: Int): Any = articles[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_article, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val article = articles[position]
        holder.title.text = article.title
        if (article.thumbnailUrl != null) {
            Glide.with(context).load(article.thumbnailUrl).into(holder.image)
        } else {
            holder.image.setImageResource(android.R.color.transparent)
        }

        view.setOnClickListener {
            val fragment = ArticleFragment()
            val bundle = Bundle()
            bundle.putString("article_url", article.url)
            fragment.arguments = bundle

            val activity = context as FragmentActivity
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private class ViewHolder(view: View) {
        val image: ImageView = view.findViewById(R.id.articleImage)
        val title: TextView = view.findViewById(R.id.articleTitle)
    }
}
