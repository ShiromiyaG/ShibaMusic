package com.shirou.shibamusic.ui.adapter

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.session.MediaBrowser
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemPlayerQueueSongBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.MediaIndexCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.google.common.util.concurrent.ListenableFuture
import java.util.ArrayList

class PlayerSongQueueAdapter(private val click: ClickCallback) : RecyclerView.Adapter<PlayerSongQueueAdapter.ViewHolder>() {

    var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private val songs: MutableList<Child> = mutableListOf()

    val items: MutableList<Child>
        get() = songs

    fun submitList(newSongs: List<Child>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlayerQueueSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[holder.layoutPosition]

        with(holder.binding) {
            queueSongTitleTextView.text = song.title.orEmpty()
            queueSongSubtitleTextView.text =
                holder.itemView.context.getString(
                    R.string.song_subtitle_formatter,
                    song.artist.orEmpty(),
                    MusicUtil.getReadableDurationString(song.duration, false),
                    MusicUtil.getReadableAudioQualityString(song)
                )

            val thumbnail: RequestBuilder<Drawable> = CustomGlideRequest.Builder
                .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
                .build()
                .sizeMultiplier(0.1f)

            CustomGlideRequest.Builder
                .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
                .build()
                .thumbnail(thumbnail)
                .into(queueSongCoverImageView)

            MediaManager.getCurrentIndex(mediaBrowserListenableFuture, object : MediaIndexCallback {
                override fun onRecovery(index: Int) {
                    if (holder.layoutPosition < index) {
                        queueSongTitleTextView.alpha = 0.2f
                        queueSongSubtitleTextView.alpha = 0.2f
                        ratingIndicatorImageView.alpha = 0.2f
                    } else {
                        queueSongTitleTextView.alpha = 1.0f
                        queueSongSubtitleTextView.alpha = 1.0f
                        ratingIndicatorImageView.alpha = 1.0f
                    }
                }
            })

            if (Preferences.showItemRating()) {
                if (song.starred == null && song.userRating == null) {
                    ratingIndicatorImageView.visibility = View.GONE
                }

                preferredIcon.visibility = if (song.starred != null) View.VISIBLE else View.GONE
                ratingBarLayout.visibility = if (song.userRating != null) View.VISIBLE else View.GONE

                song.userRating?.let { userRating ->
                    oneStarIcon.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, if (userRating >= 1) R.drawable.ic_star else R.drawable.ic_star_outlined))
                    twoStarIcon.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, if (userRating >= 2) R.drawable.ic_star else R.drawable.ic_star_outlined))
                    threeStarIcon.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, if (userRating >= 3) R.drawable.ic_star else R.drawable.ic_star_outlined))
                    fourStarIcon.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, if (userRating >= 4) R.drawable.ic_star else R.drawable.ic_star_outlined))
                    fiveStarIcon.setImageDrawable(AppCompatResources.getDrawable(holder.itemView.context, if (userRating >= 5) R.drawable.ic_star else R.drawable.ic_star_outlined))
                }
            } else {
                ratingIndicatorImageView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = songs.size

    override fun getItemId(position: Int): Long = position.toLong()

    fun getItem(id: Int): Child = songs[id]

    inner class ViewHolder(val binding: ItemPlayerQueueSongBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.queueSongTitleTextView.isSelected = true
            binding.queueSongSubtitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(songs))
                putInt(Constants.ITEM_POSITION, bindingAdapterPosition)
            }
            click.onMediaClick(bundle)
        }
    }
}
