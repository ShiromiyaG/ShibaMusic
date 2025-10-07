package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryCatalogueGenreBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Genre
import com.shirou.shibamusic.util.Constants
import java.util.Locale

class GenreCatalogueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<GenreCatalogueAdapter.ViewHolder>(), Filterable {

    private val filtering: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<Genre>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(genresFull)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                for (item in genresFull) {
                    val genreName = item.genre?.lowercase(Locale.getDefault()) ?: ""
                    if (genreName.contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            return FilterResults().apply {
                values = filteredList
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            genres.clear()
            (results.values as? List<Genre>)?.let {
                genres.addAll(it)
            }
            notifyDataSetChanged()
        }
    }

    private var genres: MutableList<Genre> = mutableListOf()
    private var genresFull: MutableList<Genre> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryCatalogueGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = genres[position]
    holder.item.genreLabel.text = genre.genre.orEmpty()
    }

    override fun getItemCount(): Int = genres.size

    fun getItem(position: Int): Genre = genres[position]

    fun setItems(genres: List<Genre>) {
        this.genres.clear()
        this.genres.addAll(genres)
        this.genresFull.clear()
        this.genresFull.addAll(genres)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = filtering

    inner class ViewHolder(val item: ItemLibraryCatalogueGenreBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            itemView.setOnClickListener {
                val bundle = Bundle().apply {
                    putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
                    // bindingAdapterPosition provides the adapter position, handling potential NO_POSITION better
                    // but the original Java code does not check for it either.
                    putParcelable(Constants.GENRE_OBJECT, genres[bindingAdapterPosition])
                }
                click.onGenreClick(bundle)
            }
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.GENRE_ORDER_BY_NAME -> genres.sortWith(compareBy { it.genre.orEmpty().lowercase(Locale.getDefault()) })
            Constants.GENRE_ORDER_BY_RANDOM -> genres.shuffle()
        }
        notifyDataSetChanged()
    }
}
