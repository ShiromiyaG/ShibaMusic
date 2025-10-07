package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryArtistBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.util.Constants

@UnstableApi
class ArtistAdapter(
    private val click: ClickCallback,
    private val mix: Boolean,
    private val bestOf: Boolean
) : RecyclerView.Adapter<ArtistAdapter.ViewHolder>() {

    var artists: List<ArtistID3> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun setItems(artists: List<ArtistID3>) {
        this.artists = artists
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

        holder.item.artistNameLabel.text = artist.name

        CustomGlideRequest.Builder
            .from(holder.itemView.context, artist.coverArtId, CustomGlideRequest.ResourceType.Artist)
            .build()
            .into(holder.item.artistCoverImageView)
    }

    override fun getItemCount(): Int = artists.size

    fun getItem(position: Int): ArtistID3 = artists[position]

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    inner class ViewHolder(val item: ItemLibraryArtistBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.artistNameLabel.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.ARTIST_OBJECT, artists[bindingAdapterPosition])
                putBoolean(Constants.MEDIA_MIX, mix)
                putBoolean(Constants.MEDIA_BEST_OF, bestOf)
            }
            click.onArtistClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.ARTIST_OBJECT, artists[bindingAdapterPosition])
            }
            click.onArtistLongClick(bundle)
            return true
        }
    }
}
