package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHorizontalAlbumBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.util.Constants
import java.util.Comparator // Explicitly import Java's Comparator for clarity
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.emptyList

class AlbumHorizontalAdapter(
    private val click: ClickCallback,
    private val isOffline: Boolean
) : RecyclerView.Adapter<AlbumHorizontalAdapter.ViewHolder>(), Filterable {

    private var albumsFull: List<AlbumID3> = emptyList()
    private var albums: List<AlbumID3> = emptyList()
    private var currentFilter: String = ""

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = ArrayList<AlbumID3>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(albumsFull)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                currentFilter = filterPattern

                for (item in albumsFull) {
                    val albumName = item.name?.lowercase(Locale.getDefault()) ?: ""
                    if (albumName.contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            return FilterResults().apply {
                values = filteredList
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            // results.values is Object?, so a cast is necessary.
            // Based on performFiltering, it will always be List<AlbumID3>.
            @Suppress("UNCHECKED_CAST")
            albums = results?.values as? List<AlbumID3> ?: emptyList()
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]

        holder.item.albumTitleTextView.text = album.name.orEmpty()
        holder.item.albumArtistTextView.text = album.artist ?: album.displayArtist.orEmpty()

        CustomGlideRequest.Builder
            .from(holder.itemView.context, album.coverArtId, CustomGlideRequest.ResourceType.Album)
            .build()
            .into(holder.item.albumCoverImageView)
    }

    override fun getItemCount(): Int = albums.size

    fun setItems(albums: List<AlbumID3>?) {
        this.albumsFull = albums ?: emptyList()
        filtering.filter(currentFilter)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = filtering

    fun getItem(id: Int): AlbumID3 = albums[id]

    inner class ViewHolder(val item: ItemHorizontalAlbumBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.albumTitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.albumMoreButton.setOnClickListener { onLongClick() }
        }

        private fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.ALBUM_OBJECT, albums[bindingAdapterPosition])
            }
            click.onAlbumClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.ALBUM_OBJECT, albums[bindingAdapterPosition])
            }
            click.onAlbumLongClick(bundle)
            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.ALBUM_ORDER_BY_NAME -> {
                albums = albums.sortedBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
            }
            Constants.ALBUM_ORDER_BY_MOST_RECENTLY_STARRED -> {
                // Assuming AlbumID3.starred is Comparable? (e.g., String?)
                albums = albums.sortedWith(Comparator.comparing(AlbumID3::starred, Comparator.nullsLast(Comparator.reverseOrder())))
            }
            Constants.ALBUM_ORDER_BY_LEAST_RECENTLY_STARRED -> {
                // Assuming AlbumID3.starred is Comparable? (e.g., String?)
                albums = albums.sortedWith(Comparator.comparing(AlbumID3::starred, Comparator.nullsLast(Comparator.naturalOrder())))
            }
        }
        notifyDataSetChanged()
    }
}
