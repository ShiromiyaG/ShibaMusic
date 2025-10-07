package com.shirou.shibamusic.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHorizontalHomeSectorBinding
import com.shirou.shibamusic.model.HomeSector

class HomeSectorHorizontalAdapter : RecyclerView.Adapter<HomeSectorHorizontalAdapter.ViewHolder>() {

    private val sectors: MutableList<HomeSector> = mutableListOf()

    fun setItems(homeSectors: List<HomeSector>) {
        sectors.clear()
        sectors.addAll(homeSectors)
        notifyDataSetChanged()
    }

    fun getItems(): MutableList<HomeSector> = sectors

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalHomeSectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sector = sectors[position]

        holder.item.homeSectorTitleCheckBox.text = sector.sectorTitle
    holder.item.homeSectorTitleCheckBox.isChecked = sector.isVisible
    }

    override fun getItemCount(): Int = sectors.size

    fun getItem(position: Int): HomeSector {
        return sectors[position]
    }

    inner class ViewHolder(val item: ItemHorizontalHomeSectorBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.homeSectorTitleCheckBox.setOnCheckedChangeListener { _, isChecked -> onCheck(isChecked) }
        }

        private fun onCheck(isChecked: Boolean) {
            if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                sectors[bindingAdapterPosition].isVisible = isChecked
            }
        }
    }
}
