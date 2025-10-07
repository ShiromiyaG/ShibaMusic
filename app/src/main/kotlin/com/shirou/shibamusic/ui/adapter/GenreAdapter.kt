package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryGenreBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Genre
import com.shirou.shibamusic.util.Constants

class GenreAdapter(private val click: ClickCallback) : RecyclerView.Adapter<GenreAdapter.ViewHolder>() {

    private var genres: List<Genre> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = genres[position]
        holder.item.genreLabel.text = genre.genre
    }

    override fun getItemCount(): Int = genres.size

    fun getItem(position: Int): Genre = genres[position]

    fun setItems(genres: List<Genre>) {
        this.genres = genres
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemLibraryGenreBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            itemView.setOnClickListener { onClick() }
        }

        private fun onClick() {
            val bundle = Bundle().apply {
                putString(Constants.MEDIA_BY_GENRE, Constants.MEDIA_BY_GENRE)
                // getBindingAdapterPosition() can return RecyclerView.NO_POSITION (-1) if the item is no longer bound.
                // However, in a click listener, it's generally expected to be a valid position.
                // Assuming it returns a valid position for an active click.
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    putParcelable(Constants.GENRE_OBJECT, genres[position])
                }
            }
            click.onGenreClick(bundle)
        }
    }
}
