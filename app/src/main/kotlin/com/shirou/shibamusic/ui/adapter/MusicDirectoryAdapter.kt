package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryMusicDirectoryBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants

@UnstableApi
class MusicDirectoryAdapter(private val click: ClickCallback) : RecyclerView.Adapter<MusicDirectoryAdapter.ViewHolder>() {

    private var children: List<Child> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryMusicDirectoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]

        holder.item.musicDirectoryTitleTextView.text = child.title

        val type = if (child.isDir) {
            CustomGlideRequest.ResourceType.Directory
        } else {
            CustomGlideRequest.ResourceType.Song
        }

        CustomGlideRequest.Builder
            .from(holder.itemView.context, child.coverArtId, type)
            .build()
            .into(holder.item.musicDirectoryCoverImageView)

        holder.item.musicDirectoryMoreButton.visibility = if (child.isDir) View.VISIBLE else View.INVISIBLE
        holder.item.musicDirectoryPlayButton.visibility = if (child.isDir) View.INVISIBLE else View.VISIBLE
    }

    override fun getItemCount(): Int = children.size

    fun setItems(children: List<Child>?) {
        this.children = children ?: emptyList()
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemLibraryMusicDirectoryBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.musicDirectoryTitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.musicDirectoryMoreButton.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle()
            val child = children[bindingAdapterPosition]

            if (child.isDir) {
                bundle.putString(Constants.MUSIC_DIRECTORY_ID, child.id)
                click.onMusicDirectoryClick(bundle)
            } else {
                bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(children))
                bundle.putInt(Constants.ITEM_POSITION, bindingAdapterPosition)
                click.onMediaClick(bundle)
            }
        }

        private fun onLongClick(): Boolean {
            val child = children[bindingAdapterPosition]

            return if (!child.isDir) {
                val bundle = Bundle()
                bundle.putParcelable(Constants.TRACK_OBJECT, child)

                click.onMediaLongClick(bundle)
                true
            } else {
                false
            }
        }
    }
}
