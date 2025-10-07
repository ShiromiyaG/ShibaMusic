package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryCatalogueAlbumBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.util.Constants
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayList
import kotlin.comparisons.naturalOrder

class AlbumCatalogueAdapter(
    private val click: ClickCallback,
    private val showArtist: Boolean
) : RecyclerView.Adapter<AlbumCatalogueAdapter.ViewHolder>(), Filterable {

    private var currentFilter: String = ""

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = ArrayList<AlbumID3>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(albumsFull)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()
                currentFilter = filterPattern

                for (item in albumsFull) {
                    val albumName = item.name?.lowercase(Locale.getDefault()) ?: ""
                    if (albumName.contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            albums = results?.values as? List<AlbumID3> ?: emptyList()
            notifyDataSetChanged()
        }
    }

    private var albums: List<AlbumID3> = emptyList()
    private var albumsFull: List<AlbumID3> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryCatalogueAlbumBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]

    holder.item.albumNameLabel.text = album.name.orEmpty()
    holder.item.artistNameLabel.text = album.artist ?: album.displayArtist.orEmpty()
        holder.item.artistNameLabel.visibility = if (showArtist) View.VISIBLE else View.GONE

        CustomGlideRequest.Builder
            .from(holder.itemView.context, album.coverArtId, CustomGlideRequest.ResourceType.Album)
            .build()
            .into(holder.item.albumCatalogueCoverImageView)
    }

    override fun getItemCount(): Int = albums.size

    fun getItem(position: Int): AlbumID3 = albums[position]

    fun setItems(albums: List<AlbumID3>) {
        this.albumsFull = ArrayList(albums)
        filtering.filter(currentFilter)
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = filtering

    inner class ViewHolder(val item: ItemLibraryCatalogueAlbumBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.albumNameLabel.isSelected = true
            item.artistNameLabel.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        private fun onClick() {
            Bundle().apply {
                putParcelable(Constants.ALBUM_OBJECT, albums[bindingAdapterPosition])
            }.also {
                click.onAlbumClick(it)
            }
        }

        private fun onLongClick(): Boolean {
            Bundle().apply {
                putParcelable(Constants.ALBUM_OBJECT, albums[bindingAdapterPosition])
            }.also {
                click.onAlbumLongClick(it)
            }
            return true
        }
    }

    fun sort(order: String) {
        albums = when (order) {
            Constants.ALBUM_ORDER_BY_NAME -> albums.sortedBy { it.name.orEmpty() }
            Constants.ALBUM_ORDER_BY_ARTIST -> albums.sortedWith(
                compareBy(Comparator.nullsLast(naturalOrder<String>())) {
                    it.artist?.lowercase(Locale.getDefault()) ?: ""
                }
            )
            Constants.ALBUM_ORDER_BY_YEAR -> albums.sortedBy { it.year }
            Constants.ALBUM_ORDER_BY_RANDOM -> albums.shuffled()
            Constants.ALBUM_ORDER_BY_RECENTLY_ADDED -> albums.sortedByDescending { it.created ?: Date(0) }
            Constants.ALBUM_ORDER_BY_RECENTLY_PLAYED -> albums.sortedByDescending { it.played ?: Date(0) }
            Constants.ALBUM_ORDER_BY_MOST_PLAYED -> albums.sortedByDescending { it.playCount }
            else -> albums // No specific order, keep as is
        }
        notifyDataSetChanged()
    }
}
