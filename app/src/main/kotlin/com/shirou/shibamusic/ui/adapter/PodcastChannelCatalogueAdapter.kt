package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHomeCataloguePodcastChannelBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.util.Constants
import java.util.Locale

class PodcastChannelCatalogueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<PodcastChannelCatalogueAdapter.ViewHolder>(), Filterable {

    private var podcastChannels: List<PodcastChannel> = emptyList()
    private var podcastChannelsFull: List<PodcastChannel> = emptyList()

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = if (constraint.isNullOrEmpty()) {
                podcastChannelsFull
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                podcastChannelsFull.filter { item ->
                    val title = item.title?.lowercase(Locale.getDefault()) ?: ""
                    title.contains(filterPattern)
                }
            }

            return FilterResults().apply {
                values = filteredList
                count = filteredList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            // results.values can be null or not of expected type, use safe cast and default to emptyList
            podcastChannels = (results?.values as? List<PodcastChannel>).orEmpty()
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeCataloguePodcastChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcastChannel = podcastChannels[position]

    holder.item.podcastChannelTitleLabel.text = podcastChannel.title.orEmpty()

        CustomGlideRequest.Builder
            .from(holder.itemView.context, podcastChannel.coverArtId, CustomGlideRequest.ResourceType.Podcast)
            .build()
            .into(holder.item.podcastChannelCatalogueCoverImageView)
    }

    override fun getItemCount(): Int = podcastChannels.size

    fun getItem(position: Int): PodcastChannel = podcastChannels[position]

    fun setItems(podcastChannels: List<PodcastChannel>) {
        this.podcastChannels = podcastChannels
        this.podcastChannelsFull = podcastChannels.toList() // Create an immutable copy
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = filtering

    inner class ViewHolder(val item: ItemHomeCataloguePodcastChannelBinding) :
        RecyclerView.ViewHolder(item.root) {

        init {
            item.podcastChannelTitleLabel.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        private fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.PODCAST_CHANNEL_OBJECT, podcastChannels[bindingAdapterPosition])
            }
            click.onPodcastChannelClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.PODCAST_CHANNEL_OBJECT, podcastChannels[bindingAdapterPosition])
            }
            click.onPodcastChannelLongClick(bundle)
            return true
        }
    }
}
