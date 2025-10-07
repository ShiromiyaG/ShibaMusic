package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemHorizontalPlaylistBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import java.util.Locale

class PlaylistHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PlaylistHorizontalAdapter.ViewHolder>(), Filterable {

    private var playlists: MutableList<Playlist> = mutableListOf()
    private var playlistsFull: MutableList<Playlist> = mutableListOf()

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<Playlist>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(playlistsFull)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                for (item in playlistsFull) {
                    val playlistName = item.name?.lowercase(Locale.getDefault()) ?: ""
                    if (playlistName.contains(filterPattern)) {
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
            playlists.clear()
            if (results?.count ?: 0 > 0) {
                (results?.values as? List<Playlist>)?.let {
                    playlists.addAll(it)
                }
            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]

    holder.item.playlistTitleTextView.text = playlist.name.orEmpty()
        holder.item.playlistSubtitleTextView.text = holder.itemView.context.getString(
            R.string.playlist_counted_tracks,
            playlist.songCount,
            MusicUtil.getReadableDurationString(playlist.duration, false)
        )

        CustomGlideRequest.Builder
            .from(holder.itemView.context, playlist.coverArtId, CustomGlideRequest.ResourceType.Playlist)
            .build()
            .into(holder.item.playlistCoverImageView)
    }

    override fun getItemCount(): Int = playlists.size

    fun getItem(id: Int): Playlist = playlists[id]

    fun setItems(playlists: List<Playlist>) {
        this.playlists = playlists.toMutableList()
        this.playlistsFull = playlists.toMutableList()
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = filtering

    inner class ViewHolder(val item: ItemHorizontalPlaylistBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.playlistTitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.playlistMoreButton.setOnClickListener { onLongClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.PLAYLIST_OBJECT, playlists[bindingAdapterPosition])
            }
            click.onPlaylistClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.PLAYLIST_OBJECT, playlists[bindingAdapterPosition])
            }
            click.onPlaylistLongClick(bundle)
            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.PLAYLIST_ORDER_BY_NAME -> {
                playlists.sortWith(compareBy { it.name.orEmpty().lowercase(Locale.getDefault()) })
            }
            Constants.PLAYLIST_ORDER_BY_RANDOM -> {
                playlists.shuffle()
            }
        }
        notifyDataSetChanged()
    }
}
