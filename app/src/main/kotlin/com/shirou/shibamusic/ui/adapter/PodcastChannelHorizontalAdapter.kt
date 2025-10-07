package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.shirou.shibamusic.databinding.ItemHorizontalPodcastChannelBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil

class PodcastChannelHorizontalAdapter(private val click: ClickCallback) : RecyclerView.Adapter<PodcastChannelHorizontalAdapter.ViewHolder>() {

    private var podcastChannels: List<PodcastChannel> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalPodcastChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcastChannel = podcastChannels[position]

        holder.binding.podcastChannelTitleTextView.text = podcastChannel.title
        holder.binding.podcastChannelDescriptionTextView.text = MusicUtil.getReadableString(podcastChannel.description)

        CustomGlideRequest.Builder
                .from(holder.itemView.context, podcastChannel.coverArtId, CustomGlideRequest.ResourceType.Podcast)
                .build()
                .into(holder.binding.podcastChannelCoverImageView)
    }

    override fun getItemCount(): Int = podcastChannels.size

    fun setItems(podcastChannels: List<PodcastChannel>) {
        this.podcastChannels = podcastChannels
        notifyDataSetChanged()
    }

    fun getItem(id: Int): PodcastChannel = podcastChannels[id]

    inner class ViewHolder(val binding: ItemHorizontalPodcastChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.podcastChannelTitleTextView.isSelected = true
            binding.podcastChannelDescriptionTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            binding.podcastChannelMoreButton.setOnClickListener { onLongClick() }
        }

        private fun onClick() {
            val adapterPosition = bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val bundle = Bundle().apply {
                    putParcelable(Constants.PODCAST_CHANNEL_OBJECT, podcastChannels[adapterPosition])
                }
                click.onPodcastChannelClick(bundle)
            }
        }

        private fun onLongClick(): Boolean {
            val adapterPosition = bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val bundle = Bundle().apply {
                    putParcelable(Constants.PODCAST_CHANNEL_OBJECT, podcastChannels[adapterPosition])
                }
                click.onPodcastChannelLongClick(bundle)
            }
            return true
        }
    }
}
