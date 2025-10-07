package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHorizontalArtistBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.util.Constants

import java.util.Comparator
import java.util.Locale

class ArtistHorizontalAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ArtistHorizontalAdapter.ViewHolder>(), Filterable {

    private var artistsFull: MutableList<ArtistID3> = mutableListOf()
    private var artists: MutableList<ArtistID3> = mutableListOf()
    private var currentFilter: String = ""

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<ArtistID3>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(artistsFull)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                currentFilter = filterPattern

                for (item in artistsFull) {
                    val artistName = item.name?.lowercase(Locale.getDefault()) ?: ""
                    if (artistName.contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            val results = FilterResults()
            results.values = filteredList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            artists = (results?.values as? List<ArtistID3>)?.toMutableList() ?: mutableListOf()
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

        holder.item.artistNameTextView.text = artist.name.orEmpty()

        if (artist.albumCount > 0) {
            holder.item.artistInfoTextView.visibility = View.VISIBLE
            holder.item.artistInfoTextView.text = "Album count: ${artist.albumCount}"
        } else {
            holder.item.artistInfoTextView.visibility = View.GONE
        }

        CustomGlideRequest.Builder
            .from(holder.itemView.context, artist.coverArtId, CustomGlideRequest.ResourceType.Artist)
            .build()
            .into(holder.item.artistCoverImageView)
    }

    override fun getItemCount(): Int = artists.size

    fun setItems(artists: List<ArtistID3>?) {
        this.artistsFull = artists?.toMutableList() ?: mutableListOf()
        filtering.filter(currentFilter)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = filtering

    fun getItem(id: Int): ArtistID3 = artists[id]

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    inner class ViewHolder(val item: ItemHorizontalArtistBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.artistNameTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.artistMoreButton.setOnClickListener { onLongClick() }
        }

        private fun onClick() {
            val bundle = Bundle().apply {
                putParcelable(Constants.ARTIST_OBJECT, artists[bindingAdapterPosition])
            }
            click.onArtistClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable(Constants.ARTIST_OBJECT, artists[bindingAdapterPosition])
            }
            click.onArtistLongClick(bundle)
            return true
        }
    }

    fun sort(order: String) {
        when (order) {
            Constants.ARTIST_ORDER_BY_NAME -> artists.sortBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
            Constants.ARTIST_ORDER_BY_MOST_RECENTLY_STARRED ->
                artists.sortWith(Comparator.comparing(ArtistID3::starred, Comparator.nullsLast(Comparator.reverseOrder())))
            Constants.ARTIST_ORDER_BY_LEAST_RECENTLY_STARRED ->
                artists.sortWith(Comparator.comparing(ArtistID3::starred, Comparator.nullsLast(Comparator.naturalOrder())))
        }
        notifyDataSetChanged()
    }
}
