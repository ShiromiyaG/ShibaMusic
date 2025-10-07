package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryMusicFolderBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.MusicFolder
import com.shirou.shibamusic.util.Constants

@UnstableApi
class MusicFolderAdapter(private val click: ClickCallback) : RecyclerView.Adapter<MusicFolderAdapter.ViewHolder>() {

    private var musicFolders: List<MusicFolder> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryMusicFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val musicFolder = musicFolders[position]

        holder.item.musicFolderTitleTextView.text = musicFolder.name

        CustomGlideRequest.Builder
            .from(holder.itemView.context, musicFolder.name, CustomGlideRequest.ResourceType.Folder)
            .build()
            .into(holder.item.musicFolderCoverImageView)
    }

    override fun getItemCount(): Int {
        return musicFolders.size
    }

    fun setItems(musicFolders: List<MusicFolder>) {
        this.musicFolders = musicFolders
        notifyDataSetChanged()
    }

    fun getItem(position: Int): MusicFolder {
        return musicFolders[position]
    }

    inner class ViewHolder(val item: ItemLibraryMusicFolderBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.musicFolderTitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }

            item.musicFolderMoreButton.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.MUSIC_FOLDER_OBJECT, musicFolders[bindingAdapterPosition])
            click.onMusicFolderClick(bundle)
        }
    }
}
