package com.shirou.shibamusic.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHorizontalPlaylistDialogTrackBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.MusicUtil

class PlaylistDialogSongHorizontalAdapter : RecyclerView.Adapter<PlaylistDialogSongHorizontalAdapter.ViewHolder>() {

    var songs: MutableList<Child> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    val items: MutableList<Child>
        get() = songs

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalPlaylistDialogTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val song = songs[position]

        holder.item.apply {
            playlistDialogSongTitleTextView.text = song.title
            playlistDialogAlbumArtistTextView.text = song.artist
            playlistDialogSongDurationTextView.text = MusicUtil.getReadableDurationString(song.duration, false)
        }

        CustomGlideRequest.Builder
            .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(holder.item.playlistDialogSongCoverImageView)
    }

    override fun getItemCount(): Int = songs.size

    fun getItem(id: Int): Child = songs[id]

    class ViewHolder(val item: ItemHorizontalPlaylistDialogTrackBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.playlistDialogSongTitleTextView.isSelected = true
        }
    }

    fun setItems(newSongs: List<Child>) {
        songs = newSongs.toMutableList()
    }
}
