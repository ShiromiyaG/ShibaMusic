package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryMusicIndexBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.helper.recyclerview.FastScrollbar
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Artist
import com.shirou.shibamusic.util.Constants
import java.util.Locale

@UnstableApi
class MusicIndexAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<MusicIndexAdapter.ViewHolder>(),
    FastScrollbar.BubbleTextGetter {

    private var artists: List<Artist> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryMusicIndexBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

        holder.item.musicIndexTitleTextView.text = artist.name.orEmpty()

        CustomGlideRequest.Builder
            .from(holder.itemView.context, artist.name, CustomGlideRequest.ResourceType.Directory)
            .build()
            .into(holder.item.musicIndexCoverImageView)
    }

    override fun getItemCount(): Int = artists.size

    fun setItems(artists: List<Artist>) {
        this.artists = artists
        notifyDataSetChanged()
    }

    override fun getTextToShowInBubble(pos: Int): String {
        if (artists.isEmpty() || pos !in artists.indices) return ""
        val name = artists[pos].name.orEmpty().trim()
        if (name.isEmpty()) return "#"
        val firstChar = name.first()
        return firstChar.toString().uppercase(Locale.getDefault())
    }

    inner class ViewHolder(val item: ItemLibraryMusicIndexBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.musicIndexTitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            item.musicIndexMoreButton.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle()
            // The original Java code would throw IndexOutOfBoundsException if getBindingAdapterPosition() is invalid (-1 or out of bounds).
            // This Kotlin code will behave equivalently by throwing an exception in such cases.
            bundle.putString(Constants.MUSIC_DIRECTORY_ID, artists[bindingAdapterPosition].id)
            click.onMusicIndexClick(bundle)
        }
    }
}
