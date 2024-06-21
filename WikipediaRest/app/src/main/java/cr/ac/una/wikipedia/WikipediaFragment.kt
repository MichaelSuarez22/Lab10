package cr.ac.una.wikipedia

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

class WikipediaFragment : Fragment() {
    private lateinit var searchButton: Button
    private lateinit var searchEditText: EditText
    private lateinit var articleListView: ListView
    private val articles = mutableListOf<WikipediaArticle>()
    private lateinit var adapter: WikipediaArticleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_wikipedia, container, false)

        searchButton = view.findViewById(R.id.searchButton)
        searchEditText = view.findViewById(R.id.searchEditText)
        articleListView = view.findViewById(R.id.articleListView)

        adapter = WikipediaArticleAdapter(requireContext(), articles)
        articleListView.adapter = adapter

        // Limpiar la caché del WebView
        articleListView.clearFocus()

        // Cargar la URL inicial si hay conexión a Internet
        if (isNetworkAvailable()) {
            loadWikipediaPage("Costa_Rica")
        } else {
            Toast.makeText(activity, "No internet connection", Toast.LENGTH_SHORT).show()
        }

        searchButton.setOnClickListener {
            val title = searchEditText.text.toString().replace(" ", "_")
            if (isNetworkAvailable()) {
                loadWikipediaPage(title)
            } else {
                Toast.makeText(activity, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadWikipediaPage(title: String) {
        val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$title"
        Executors.newSingleThreadExecutor().execute {
            try {
                val apiResponse = URL(url).readText()
                val jsonObject = JSONObject(apiResponse)
                val article = WikipediaArticle(
                    title = jsonObject.getString("title"),
                    thumbnailUrl = jsonObject.optJSONObject("thumbnail")?.optString("source"),
                    url = jsonObject.getString("content_urls").let {
                        JSONObject(it).getJSONObject("desktop").getString("page")
                    }
                )
                activity?.runOnUiThread {
                    articles.clear()
                    articles.add(article)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(activity, "Failed to load article", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
