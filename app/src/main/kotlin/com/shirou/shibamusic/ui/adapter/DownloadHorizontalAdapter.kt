package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemHorizontalDownloadBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import kotlin.collections.ArrayList

@OptIn(UnstableApi::class)
class DownloadHorizontalAdapter(private val click: ClickCallback) : RecyclerView.Adapter<DownloadHorizontalAdapter.ViewHolder>() {

    private var view: String = Constants.DOWNLOAD_TYPE_TRACK
    private var filterKey: String? = null
    private var filterValue: String? = null

    private var songs: List<Child> = emptyList()
    private var shuffling: List<Child> = emptyList()
    private var grouped: List<Child> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (view) {
            Constants.DOWNLOAD_TYPE_TRACK -> initTrackLayout(holder, position)
            Constants.DOWNLOAD_TYPE_ALBUM -> initAlbumLayout(holder, position)
            Constants.DOWNLOAD_TYPE_ARTIST -> initArtistLayout(holder, position)
            Constants.DOWNLOAD_TYPE_GENRE -> initGenreLayout(holder, position)
            Constants.DOWNLOAD_TYPE_YEAR -> initYearLayout(holder, position)
        }
    }

    override fun getItemCount(): Int = grouped.size

    fun setItems(view: String, filterKey: String, filterValue: String?, songs: List<Child>) {
        this.view = if (filterValue != null) view else filterKey
        this.filterKey = filterKey
        this.filterValue = filterValue

        this.songs = songs
        this.grouped = groupSong(songs)
        this.shuffling = shufflingSong(songs)

        notifyDataSetChanged()
    }

    fun getItem(id: Int): Child = grouped[id]

    fun getShuffling(): List<Child> = shuffling

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    private fun groupSong(songs: List<Child>): List<Child> = when (view) {
        Constants.DOWNLOAD_TYPE_TRACK -> filterSong(filterKey, filterValue, songs.filter { it.id != null }.distinctBy { it.id })
        Constants.DOWNLOAD_TYPE_ALBUM -> filterSong(filterKey, filterValue, songs.filter { it.albumId != null }.distinctBy { it.albumId })
        Constants.DOWNLOAD_TYPE_ARTIST -> filterSong(filterKey, filterValue, songs.filter { it.artistId != null }.distinctBy { it.artistId })
        Constants.DOWNLOAD_TYPE_GENRE -> filterSong(filterKey, filterValue, songs.filter { it.genre != null }.distinctBy { it.genre })
        Constants.DOWNLOAD_TYPE_YEAR -> filterSong(filterKey, filterValue, songs.filter { it.year != null }.distinctBy { it.year })
        else -> emptyList()
    }

    private fun filterSong(filterKey: String?, filterValue: String?, songs: List<Child>): List<Child> {
        if (filterValue == null) {
            return songs
        }

        return songs.filter { child ->
            when (filterKey) {
                Constants.DOWNLOAD_TYPE_TRACK -> child.id == filterValue
                Constants.DOWNLOAD_TYPE_ALBUM -> child.albumId == filterValue
                Constants.DOWNLOAD_TYPE_GENRE -> child.genre == filterValue
                Constants.DOWNLOAD_TYPE_YEAR -> child.year == filterValue.toIntOrNull()
                Constants.DOWNLOAD_TYPE_ARTIST -> child.artistId == filterValue
                else -> false
            }
        }
    }

    private fun shufflingSong(songs: List<Child>): List<Child> {
        return if (filterValue == null) {
            songs
        } else {
            filterSong(filterKey, filterValue, songs)
        }
    }

    private fun countSong(filterKey: String?, filterValue: String?, songs: List<Child>): String {
        if (filterValue == null) {
            return "0"
        }

        val count = songs.count { child ->
            when (filterKey) {
                Constants.DOWNLOAD_TYPE_TRACK -> child.id == filterValue
                Constants.DOWNLOAD_TYPE_ALBUM -> child.albumId == filterValue
                Constants.DOWNLOAD_TYPE_GENRE -> child.genre == filterValue
                Constants.DOWNLOAD_TYPE_YEAR -> child.year == filterValue.toIntOrNull()
                Constants.DOWNLOAD_TYPE_ARTIST -> child.artistId == filterValue
                else -> false
            }
        }
        return count.toString()
    }

    private fun initTrackLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]

        holder.item.downloadedItemTitleTextView.text = song.title
        holder.item.downloadedItemSubtitleTextView.text = holder.itemView.context.getString(
            R.string.song_subtitle_formatter,
            song.artist,
            MusicUtil.getReadableDurationString(song.duration, false),
            ""
        )

        holder.item.downloadedItemPreTextView.text = song.album

        CustomGlideRequest.Builder
            .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(holder.item.itemCoverImageView)

        holder.item.itemCoverImageView.visibility = View.VISIBLE
        holder.item.downloadedItemMoreButton.visibility = View.VISIBLE
        holder.item.divider.visibility = View.VISIBLE

        if (position > 0) {
            if (grouped.getOrNull(position - 1)?.album != song.album) {
                holder.item.divider.setPadding(0, holder.itemView.context.resources.getDimensionPixelSize(R.dimen.downloaded_item_padding), 0, 0)
            } else {
                holder.item.divider.visibility = View.GONE
            }
        }
    }

    private fun initAlbumLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]

        holder.item.downloadedItemTitleTextView.text = song.album
        holder.item.downloadedItemSubtitleTextView.text = holder.itemView.context.getString(R.string.download_item_single_subtitle_formatter, countSong(Constants.DOWNLOAD_TYPE_ALBUM, song.albumId, songs))
        holder.item.downloadedItemPreTextView.text = song.artist

        CustomGlideRequest.Builder
            .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(holder.item.itemCoverImageView)

        holder.item.itemCoverImageView.visibility = View.VISIBLE
        holder.item.downloadedItemMoreButton.visibility = View.VISIBLE
        holder.item.divider.visibility = View.VISIBLE

        if (position > 0) {
            if (grouped.getOrNull(position - 1)?.artist != song.artist) {
                holder.item.divider.setPadding(0, holder.itemView.context.resources.getDimensionPixelSize(R.dimen.downloaded_item_padding), 0, 0)
            } else {
                holder.item.divider.visibility = View.GONE
            }
        }
    }

    private fun initArtistLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]

        holder.item.downloadedItemTitleTextView.text = song.artist
        holder.item.downloadedItemSubtitleTextView.text = holder.itemView.context.getString(R.string.download_item_single_subtitle_formatter, countSong(Constants.DOWNLOAD_TYPE_ARTIST, song.artistId, songs))

        CustomGlideRequest.Builder
            .from(holder.itemView.context, song.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(holder.item.itemCoverImageView)

        holder.item.itemCoverImageView.visibility = View.VISIBLE
        holder.item.downloadedItemMoreButton.visibility = View.VISIBLE
        holder.item.divider.visibility = View.GONE
    }

    private fun initGenreLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]

        holder.item.downloadedItemTitleTextView.text = song.genre
        holder.item.downloadedItemSubtitleTextView.text = holder.itemView.context.getString(R.string.download_item_single_subtitle_formatter, countSong(Constants.DOWNLOAD_TYPE_GENRE, song.genre, songs))

        holder.item.itemCoverImageView.visibility = View.GONE
        holder.item.downloadedItemMoreButton.visibility = View.VISIBLE
        holder.item.divider.visibility = View.GONE
    }

    private fun initYearLayout(holder: ViewHolder, position: Int) {
        val song = grouped[position]

        holder.item.downloadedItemTitleTextView.text = song.year?.toString()
        holder.item.downloadedItemSubtitleTextView.text = holder.itemView.context.getString(R.string.download_item_single_subtitle_formatter, countSong(Constants.DOWNLOAD_TYPE_YEAR, song.year?.toString(), songs))

        holder.item.itemCoverImageView.visibility = View.GONE
        holder.item.downloadedItemMoreButton.visibility = View.VISIBLE
        holder.item.divider.visibility = View.GONE
    }

    inner class ViewHolder(val item: ItemHorizontalDownloadBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.downloadedItemTitleTextView.isSelected = true
            item.downloadedItemSubtitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.downloadedItemMoreButton.setOnClickListener { onLongClick() }
        }

        fun onClick() {
            val bundle = Bundle()
            val currentPosition = bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return

            val currentChild = grouped.getOrNull(currentPosition) ?: return

            when (view) {
                Constants.DOWNLOAD_TYPE_TRACK -> {
                    bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(grouped))
                    bundle.putInt(Constants.ITEM_POSITION, currentPosition)
                    click.onMediaClick(bundle)
                }
                Constants.DOWNLOAD_TYPE_ALBUM -> {
                    currentChild.albumId?.let { albumId ->
                        bundle.putString(Constants.DOWNLOAD_TYPE_ALBUM, albumId)
                        click.onAlbumClick(bundle)
                    }
                }
                Constants.DOWNLOAD_TYPE_ARTIST -> {
                    currentChild.artistId?.let { artistId ->
                        bundle.putString(Constants.DOWNLOAD_TYPE_ARTIST, artistId)
                        click.onArtistClick(bundle)
                    }
                }
                Constants.DOWNLOAD_TYPE_GENRE -> {
                    currentChild.genre?.let { genre ->
                        bundle.putString(Constants.DOWNLOAD_TYPE_GENRE, genre)
                        click.onGenreClick(bundle)
                    }
                }
                Constants.DOWNLOAD_TYPE_YEAR -> {
                    currentChild.year?.toString()?.let { yearString ->
                        bundle.putString(Constants.DOWNLOAD_TYPE_YEAR, yearString)
                        click.onYearClick(bundle)
                    }
                }
            }
        }

        private fun onLongClick(): Boolean {
            val filteredSongs = ArrayList<Child>()
            val bundle = Bundle()
            val currentPosition = bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return false
            val currentChild = grouped.getOrNull(currentPosition) ?: return false

            when (view) {
                Constants.DOWNLOAD_TYPE_TRACK -> filteredSongs.add(currentChild)
                Constants.DOWNLOAD_TYPE_ALBUM -> currentChild.albumId?.let { albumId ->
                    filteredSongs.addAll(filterSong(Constants.DOWNLOAD_TYPE_ALBUM, albumId, songs))
                }
                Constants.DOWNLOAD_TYPE_ARTIST -> currentChild.artistId?.let { artistId ->
                    filteredSongs.addAll(filterSong(Constants.DOWNLOAD_TYPE_ARTIST, artistId, songs))
                }
                Constants.DOWNLOAD_TYPE_GENRE -> currentChild.genre?.let { genre ->
                    filteredSongs.addAll(filterSong(Constants.DOWNLOAD_TYPE_GENRE, genre, songs))
                }
                Constants.DOWNLOAD_TYPE_YEAR -> currentChild.year?.toString()?.let { yearString ->
                    filteredSongs.addAll(filterSong(Constants.DOWNLOAD_TYPE_YEAR, yearString, songs))
                }
            }

            if (filteredSongs.isEmpty()) return false

            bundle.putParcelableArrayList(Constants.DOWNLOAD_GROUP, filteredSongs)
            bundle.putString(Constants.DOWNLOAD_GROUP_TITLE, item.downloadedItemTitleTextView.text.toString())
            bundle.putString(Constants.DOWNLOAD_GROUP_SUBTITLE, item.downloadedItemSubtitleTextView.text.toString())
            click.onDownloadGroupLongClick(bundle)

            return true
        }
    }
}
