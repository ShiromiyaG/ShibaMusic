package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemHomeYearBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.util.Constants

class YearAdapter(private val click: ClickCallback) : RecyclerView.Adapter<YearAdapter.ViewHolder>() {

    var years: List<Int> = emptyList()
        private set // Make setter private to enforce `setItems` usage

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHomeYearBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val year = years[position]
        holder.item.yearLabel.text = year.toString()
    }

    override fun getItemCount(): Int = years.size

    fun getItem(position: Int): Int = years[position]

    fun setItems(years: List<Int>) {
        this.years = years
        notifyDataSetChanged()
    }

    inner class ViewHolder(val item: ItemHomeYearBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            itemView.setOnClickListener { onClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putString(Constants.MEDIA_BY_YEAR, Constants.MEDIA_BY_YEAR)
                // Note: If getBindingAdapterPosition() returns RecyclerView.NO_POSITION (-1),
                // this will result in an IndexOutOfBoundsException, matching the Java behavior.
                putInt("year_object", years[bindingAdapterPosition])
            }
            click.onYearClick(bundle)
        }
    }
}
