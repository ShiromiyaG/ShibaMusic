package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.shirou.shibamusic.databinding.ItemLibraryAlbumBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.util.Constants

class AlbumAdapter(private val click: ClickCallback) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    private var albums: List<AlbumID3> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]

        holder.item.albumNameLabel.text = album.name
        holder.item.artistNameLabel.text = album.artist

        CustomGlideRequest.Builder
            .from(holder.itemView.context, album.coverArtId, CustomGlideRequest.ResourceType.Album)
            .build()
            .into(holder.item.albumCoverImageView)
    }

    override fun getItemCount(): Int = albums.size

    fun getItem(position: Int): AlbumID3 = albums[position]

    fun setItems(albums: List<AlbumID3>) {
        this.albums = albums
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemLibraryAlbumBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.albumNameLabel.isSelected = true
            item.artistNameLabel.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
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
}
