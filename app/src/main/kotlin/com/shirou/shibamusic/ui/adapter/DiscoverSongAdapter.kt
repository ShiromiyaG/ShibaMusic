package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHomeDiscoverSongBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants

class DiscoverSongAdapter(private val click: ClickCallback) : RecyclerView.Adapter<DiscoverSongAdapter.ViewHolder>() {

    private var songs: List<Child> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeDiscoverSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        holder.item.titleDiscoverSongLabel.text = song.title
        holder.item.albumDiscoverSongLabel.text = song.album

        CustomGlideRequest.Builder
            .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(holder.item.discoverSongCoverImageView)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        startAnimation(holder)
    }

    override fun getItemCount(): Int = songs.size

    fun setItems(songs: List<Child>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemHomeDiscoverSongBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            itemView.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.TRACK_OBJECT, songs[bindingAdapterPosition])
                putBoolean(Constants.MEDIA_MIX, true)
            }
            click.onMediaClick(bundle)
        }
    }

    private fun startAnimation(holder: ViewHolder) {
        holder.item.discoverSongCoverImageView.animate()
            .setDuration(20000)
            .setStartDelay(10)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .scaleX(1.4f)
            .scaleY(1.4f)
            .start()
    }
}
