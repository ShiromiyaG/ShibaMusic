package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView

import com.shirou.shibamusic.databinding.ItemHomeInternetRadioStationBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import com.shirou.shibamusic.util.Constants

@UnstableApi
class InternetRadioStationAdapter(private val click: ClickCallback) :
    RecyclerView.Adapter<InternetRadioStationAdapter.ViewHolder>() {

    private var internetRadioStations: List<InternetRadioStation> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeInternetRadioStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val internetRadioStation = internetRadioStations[position]

        holder.item.internetRadioStationTitleTextView.text = internetRadioStation.name
        holder.item.internetRadioStationSubtitleTextView.text = internetRadioStation.streamUrl

        CustomGlideRequest.Builder
            .from(holder.itemView.context, internetRadioStation.streamUrl, CustomGlideRequest.ResourceType.Radio)
            .build()
            .into(holder.item.internetRadioStationCoverImageView)
    }

    override fun getItemCount(): Int = internetRadioStations.size

    fun setItems(internetRadioStations: List<InternetRadioStation>) {
        this.internetRadioStations = internetRadioStations
        notifyDataSetChanged()
    }

    fun getItem(position: Int): InternetRadioStation = internetRadioStations[position]

    inner class ViewHolder(val item: ItemHomeInternetRadioStationBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.internetRadioStationTitleTextView.isSelected = true
            item.internetRadioStationSubtitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.internetRadioStationMoreButton.setOnClickListener { onLongClick() }
        }

        fun onClick() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val bundle = Bundle().apply {
                    putParcelable(Constants.INTERNET_RADIO_STATION_OBJECT, internetRadioStations[position])
                }
                click.onInternetRadioStationClick(bundle)
            }
        }

        private fun onLongClick(): Boolean {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val bundle = Bundle().apply {
                    putParcelable(Constants.INTERNET_RADIO_STATION_OBJECT, internetRadioStations[position])
                }
                click.onInternetRadioStationLongClick(bundle)
            }
            return true
        }
    }
}
