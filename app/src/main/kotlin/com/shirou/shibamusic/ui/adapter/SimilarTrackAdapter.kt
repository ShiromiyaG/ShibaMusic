package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHomeSimilarTrackBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants

class SimilarTrackAdapter(private val click: ClickCallback) : RecyclerView.Adapter<SimilarTrackAdapter.ViewHolder>() {

    private var songs: List<Child> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeSimilarTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        holder.item.titleTrackLabel.text = song.title

        CustomGlideRequest.Builder
                .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
                .build()
                .into(holder.item.trackCoverImageView)
    }

    override fun getItemCount(): Int = songs.size

    fun getItem(position: Int): Child = songs[position]

    fun setItems(songs: List<Child>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemHomeSimilarTrackBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.TRACK_OBJECT, songs[bindingAdapterPosition])
                putBoolean(Constants.MEDIA_MIX, true)
            }
            click.onMediaClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.TRACK_OBJECT, songs[bindingAdapterPosition])
            }
            click.onMediaLongClick(bundle)
            return true
        }
    }
}
