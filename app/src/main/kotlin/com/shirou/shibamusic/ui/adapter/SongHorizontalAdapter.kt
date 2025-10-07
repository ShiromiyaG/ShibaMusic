package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemHorizontalTrackBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import java.util.ArrayList
import java.util.Date
import java.util.Locale

@UnstableApi
class SongHorizontalAdapter(
    private val click: ClickCallback,
    private val showCoverArt: Boolean,
    private val showAlbum: Boolean,
    private val album: AlbumID3?
) : RecyclerView.Adapter<SongHorizontalAdapter.ViewHolder>(), Filterable {

    private var songsFull: MutableList<Child> = mutableListOf()
    private var songs: MutableList<Child> = mutableListOf()
    private var currentFilter: String = ""

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = ArrayList<Child>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(songsFull)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                currentFilter = filterPattern

                for (item in songsFull) {
                    val title = item.title?.lowercase(Locale.getDefault()) ?: ""
                    if (title.contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            return FilterResults().apply {
                values = filteredList
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            songs = (results?.values as? List<Child>)?.toMutableList() ?: mutableListOf()
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        with(holder.item) {
            searchResultSongTitleTextView.text = song.title.orEmpty()

            searchResultSongSubtitleTextView.text =
                holder.itemView.context.getString(
                    R.string.song_subtitle_formatter,
                    (if (showAlbum) song.album else song.artist).orEmpty(),
                    MusicUtil.getReadableDurationString(song.duration, false),
                    MusicUtil.getReadableAudioQualityString(song)
                )

            trackNumberTextView.text = MusicUtil.getReadableTrackNumber(holder.itemView.context, song.track)

            searchResultDownloadIndicatorImageView.visibility =
                if (DownloadUtil.getDownloadTracker(holder.itemView.context).isDownloaded(song.id)) View.VISIBLE else View.GONE

            if (showCoverArt) {
                CustomGlideRequest.Builder
                    .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
                    .build()
                    .into(songCoverImageView)
            }

            trackNumberTextView.visibility = if (showCoverArt) View.INVISIBLE else View.VISIBLE
            songCoverImageView.visibility = if (showCoverArt) View.VISIBLE else View.INVISIBLE

            differentDiskDividerSector.visibility = View.GONE // Default to GONE to prevent recycling issues
            discTitleTextView.text = "" // Clear text to prevent recycling issues

            if (!showCoverArt &&
                (position == 0 ||
                        (position > 0 &&
                                songs.getOrNull(position - 1)?.discNumber != null &&
                                song.discNumber != null &&
                                songs[position - 1].discNumber!! < song.discNumber!!
                        )
                )
            ) {
                differentDiskDividerSector.visibility = View.VISIBLE

                if (song.discNumber?.toString()?.isNotBlank() == true) {
                    discTitleTextView.text = holder.itemView.context.getString(R.string.disc_titleless, song.discNumber.toString())
                }

                album?.discTitles?.let { discTitles ->
                    val discTitle = discTitles.firstOrNull { it.disc == song.discNumber }

                    if (discTitle?.disc != null && discTitle.title?.isNotBlank() == true) {
                        discTitleTextView.text = holder.itemView.context.getString(R.string.disc_titlefull, discTitle.disc.toString(), discTitle.title)
                    }
                }
            }

            if (Preferences.showItemRating()) {
                val hasRating = song.starred != null || song.userRating != null
                ratingIndicatorImageView.visibility = if (hasRating) View.VISIBLE else View.GONE

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
                preferredIcon.visibility = View.GONE
                ratingBarLayout.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = songs.size

    fun setItems(songs: List<Child>?) {
        this.songsFull = songs?.toMutableList() ?: mutableListOf()
        filtering.filter(currentFilter)
        notifyDataSetChanged() // To update UI with `songsFull` if filter is empty, or pending filtered `songs`.
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = filtering

    fun getItem(id: Int): Child = songs[id]

    inner class ViewHolder(val item: ItemHorizontalTrackBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.searchResultSongTitleTextView.isSelected = true
            item.searchResultSongSubtitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.searchResultSongMoreButton.setOnClickListener { onLongClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(MusicUtil.limitPlayableMedia(songs, bindingAdapterPosition)))
                putInt(Constants.ITEM_POSITION, MusicUtil.getPlayableMediaPosition(songs, bindingAdapterPosition))
            }
            click.onMediaClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.TRACK_OBJECT, songs[bindingAdapterPosition])
            }
            click.onMediaLongClick(bundle)
            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.MEDIA_BY_TITLE -> songs.sortBy { it.title.orEmpty().lowercase(Locale.getDefault()) }
            Constants.MEDIA_MOST_RECENTLY_STARRED -> songs.sortWith(compareByDescending<Child> { it.starred ?: Date(0) })
            Constants.MEDIA_LEAST_RECENTLY_STARRED -> songs.sortWith(compareBy<Child> { it.starred ?: Date(Long.MAX_VALUE) })
        }
        notifyDataSetChanged()
    }
}
