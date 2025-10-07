package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibrarySimilarArtistBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.SimilarArtistID3
import com.shirou.shibamusic.util.Constants

class ArtistSimilarAdapter(private val click: ClickCallback) : RecyclerView.Adapter<ArtistSimilarAdapter.ViewHolder>() {

    private var artists: List<SimilarArtistID3> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibrarySimilarArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

        holder.item.artistNameLabel.text = artist.name

        CustomGlideRequest.Builder
                .from(holder.itemView.context, artist.coverArtId, CustomGlideRequest.ResourceType.Artist)
                .build()
                .into(holder.item.similarArtistCoverImageView)
    }

    override fun getItemCount(): Int = artists.size

    fun getItem(position: Int): SimilarArtistID3 = artists[position]

    fun setItems(newArtists: List<SimilarArtistID3>) {
        this.artists = newArtists
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    inner class ViewHolder(val item: ItemLibrarySimilarArtistBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
            item.artistNameLabel.isSelected = true
        }

        fun onClick() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val bundle = Bundle()
                bundle.putParcelable(Constants.ARTIST_OBJECT, artists[position])
                click.onArtistClick(bundle)
            }
        }

        fun onLongClick(): Boolean {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val bundle = Bundle()
                bundle.putParcelable(Constants.ARTIST_OBJECT, artists[position])
                click.onArtistLongClick(bundle)
            }
            return true
        }
    }
}
