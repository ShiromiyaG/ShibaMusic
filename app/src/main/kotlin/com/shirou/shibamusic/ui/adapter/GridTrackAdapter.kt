package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import com.shirou.shibamusic.databinding.ItemHomeGridTrackBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.model.Chronology
import com.shirou.shibamusic.util.Constants

import java.util.ArrayList

class GridTrackAdapter(private val click: ClickCallback) : RecyclerView.Adapter<GridTrackAdapter.ViewHolder>() {

    private var items: List<Chronology> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeGridTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        CustomGlideRequest.Builder
            .from(holder.itemView.context, item.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(holder.binding.trackCoverImageView)
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): Chronology = items[position]

    fun setItems(items: List<Chronology>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemHomeGridTrackBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(items))
                putBoolean(Constants.MEDIA_CHRONOLOGY, true)
                putInt(Constants.ITEM_POSITION, bindingAdapterPosition)
            }
            click.onMediaClick(bundle)
        }
    }
}
