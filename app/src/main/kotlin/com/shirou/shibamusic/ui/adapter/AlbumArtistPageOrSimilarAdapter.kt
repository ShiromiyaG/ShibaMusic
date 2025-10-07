package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryArtistPageOrSimilarAlbumBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.util.Constants

class AlbumArtistPageOrSimilarAdapter(private val click: ClickCallback) : RecyclerView.Adapter<AlbumArtistPageOrSimilarAdapter.ViewHolder>() {

    private var albums: List<AlbumID3> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLibraryArtistPageOrSimilarAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]

        holder.item.albumNameLabel.text = album.name
        holder.item.artistNameLabel.text = album.artist

        CustomGlideRequest.Builder
            .from(holder.itemView.context, album.coverArtId, CustomGlideRequest.ResourceType.Album)
            .build()
            .into(holder.item.artistPageAlbumCoverImageView)
    }

    override fun getItemCount(): Int = albums.size

    fun getItem(position: Int): AlbumID3 = albums[position]

    fun setItems(albums: List<AlbumID3>) {
        this.albums = albums
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemLibraryArtistPageOrSimilarAlbumBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.albumNameLabel.isSelected = true
            item.artistNameLabel.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        private fun onClick() {
            // getBindingAdapterPosition() returns NO_POSITION if the item is not bound or removed.
            // If it returns NO_POSITION (-1), albums[-1] will cause IndexOutOfBoundsException,
            // which mimics the behavior of the original Java code if it were to receive -1.
            val position = bindingAdapterPosition
            val bundle = Bundle().apply {
                putParcelable(Constants.ALBUM_OBJECT, albums[position])
            }
            click.onAlbumClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val position = bindingAdapterPosition
            val bundle = Bundle().apply {
                putParcelable(Constants.ALBUM_OBJECT, albums[position])
            }
            click.onAlbumLongClick(bundle)
            return true
        }
    }
}
