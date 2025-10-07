package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLibraryCatalogueArtistBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.util.Constants
import java.util.Locale

class ArtistCatalogueAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<ArtistCatalogueAdapter.ViewHolder>(), Filterable {

    private val filtering = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<ArtistID3>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(artistFull)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                for (item in artistFull) {
                    val artistName = item.name?.lowercase(Locale.getDefault()) ?: ""
                    if (artistName.contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            return FilterResults().apply {
                values = filteredList
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            artists.clear()
            results?.let {
                if (it.count > 0) {
                    @Suppress("UNCHECKED_CAST")
                    artists.addAll(it.values as List<ArtistID3>)
                }
            }
            notifyDataSetChanged()
        }
    }

    private var artists: MutableList<ArtistID3> = mutableListOf()
    private var artistFull: MutableList<ArtistID3> = mutableListOf()

    init {
        this.artists = mutableListOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLibraryCatalogueArtistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]

    holder.item.artistNameLabel.text = artist.name.orEmpty()

        CustomGlideRequest.Builder
            .from(holder.itemView.context, artist.coverArtId, CustomGlideRequest.ResourceType.Artist)
            .build()
            .into(holder.item.artistCatalogueCoverImageView)
    }

    override fun getItemCount(): Int = artists.size

    fun getItem(position: Int): ArtistID3 = artists[position]

    fun setItems(artists: List<ArtistID3>) {
        this.artists = artists.toMutableList()
        this.artistFull = artists.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = filtering

    inner class ViewHolder(val item: ItemLibraryCatalogueArtistBinding) :
        RecyclerView.ViewHolder(item.root) {

        init {
            item.artistNameLabel.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        fun onClick() {
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
            Constants.ARTIST_ORDER_BY_RANDOM -> artists.shuffle()
        }
        notifyDataSetChanged()
    }
}
