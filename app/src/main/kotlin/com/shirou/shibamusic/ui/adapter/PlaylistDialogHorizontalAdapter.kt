package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemHorizontalPlaylistDialogBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil

class PlaylistDialogHorizontalAdapter(private val click: ClickCallback) : RecyclerView.Adapter<PlaylistDialogHorizontalAdapter.ViewHolder>() {

    private var playlists: List<Playlist> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalPlaylistDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.item.playlistDialogTitleTextView.text = playlist.name
        holder.item.playlistDialogCountTextView.text = holder.itemView.context.getString(R.string.playlist_counted_tracks, playlist.songCount, MusicUtil.getReadableDurationString(playlist.duration, false))
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    fun setItems(playlists: List<Playlist>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Playlist {
        return playlists[id]
    }

    inner class ViewHolder(val item: ItemHorizontalPlaylistDialogBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.playlistDialogTitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.PLAYLIST_OBJECT, playlists[bindingAdapterPosition])
            click.onPlaylistClick(bundle)
        }
    }
}
