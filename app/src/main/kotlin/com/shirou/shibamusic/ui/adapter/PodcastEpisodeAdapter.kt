package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemHomePodcastEpisodeBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PodcastEpisodeAdapter(private val click: ClickCallback) : RecyclerView.Adapter<PodcastEpisodeAdapter.ViewHolder>() {

    private var podcastEpisodes: List<PodcastEpisode> = emptyList()
    private var podcastEpisodesFull: List<PodcastEpisode> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomePodcastEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcastEpisode = podcastEpisodes[position]
        val simpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        holder.item.apply {
            podcastTitleLabel.text = podcastEpisode.title.orEmpty()
            podcastSubtitleLabel.text = podcastEpisode.artist.orEmpty()
            podcastReleasesAndDurationLabel.text = holder.itemView.context.getString(
                R.string.podcast_release_date_duration_formatter,
                simpleDateFormat.format(podcastEpisode.publishDate ?: Date(0)),
                MusicUtil.getReadablePodcastDurationString((podcastEpisode.duration ?: 0).toLong())
            )
            podcastDescriptionText.text = MusicUtil.getReadableString(podcastEpisode.description)

            CustomGlideRequest.Builder
                .from(holder.itemView.context, podcastEpisode.coverArtId, CustomGlideRequest.ResourceType.Podcast)
                .build()
                .into(podcastCoverImageView)

            podcastPlayButton.isEnabled = podcastEpisode.status == "completed"
            podcastMoreButton.visibility = if (podcastEpisode.status == "completed") View.VISIBLE else View.GONE
            podcastDownloadRequestButton.visibility = if (podcastEpisode.status == "completed") View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount(): Int = podcastEpisodes.size

    fun setItems(podcastEpisodes: List<PodcastEpisode>) {
        this.podcastEpisodesFull = podcastEpisodes
        this.podcastEpisodes = podcastEpisodesFull.filter { it.status == "completed" }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    inner class ViewHolder(val item: ItemHomePodcastEpisodeBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { openMore() }

            item.podcastPlayButton.setOnClickListener { onClick() }
            item.podcastMoreButton.setOnClickListener { openMore() }
            item.podcastDownloadRequestButton.setOnClickListener { requestDownload() }
        }

        fun onClick() {
            val podcastEpisode = podcastEpisodes[bindingAdapterPosition]

            if (podcastEpisode.status == "completed") {
                val bundle = Bundle().apply {
                    putParcelable(Constants.PODCAST_OBJECT, podcastEpisodes[bindingAdapterPosition])
                }
                click.onPodcastEpisodeClick(bundle)
            }
        }

        private fun openMore(): Boolean {
            val podcastEpisode = podcastEpisodes[bindingAdapterPosition]

            return if (podcastEpisode.status == "completed") {
                val bundle = Bundle().apply {
                    putParcelable(Constants.PODCAST_OBJECT, podcastEpisodes[bindingAdapterPosition])
                }
                click.onPodcastEpisodeLongClick(bundle)
                true
            } else {
                false
            }
        }

        fun requestDownload() {
            val podcastEpisode = podcastEpisodes[bindingAdapterPosition]

            if (podcastEpisode.status != "completed") {
                val bundle = Bundle().apply {
                    putParcelable(Constants.PODCAST_OBJECT, podcastEpisodes[bindingAdapterPosition])
                }
                click.onPodcastEpisodeAltClick(bundle)
            }
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.PODCAST_FILTER_BY_DOWNLOAD -> {
                podcastEpisodes = podcastEpisodesFull.filter { it.status == "completed" }
            }
            Constants.PODCAST_FILTER_BY_ALL -> {
                podcastEpisodes = podcastEpisodesFull
            }
        }
        notifyDataSetChanged()
    }
}
