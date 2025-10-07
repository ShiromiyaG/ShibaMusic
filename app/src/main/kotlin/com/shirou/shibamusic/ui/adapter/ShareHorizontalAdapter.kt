package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.ItemHorizontalShareBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Share
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.UIUtil

class ShareHorizontalAdapter(private val click: ClickCallback) : RecyclerView.Adapter<ShareHorizontalAdapter.ViewHolder>() {

    private var shares: List<Share> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHorizontalShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val share = shares[position]

        holder.item.shareTitleTextView.text = share.description.orEmpty()
        holder.item.shareSubtitleTextView.text = holder.itemView.context.getString(
            R.string.share_subtitle_item,
            UIUtil.getReadableDate(share.expires ?: java.util.Date())
        )

        share.entries?.firstOrNull()?.let { entry ->
            CustomGlideRequest.Builder
                .from(holder.itemView.context, entry.coverArtId, CustomGlideRequest.ResourceType.Album)
                .build()
                .into(holder.item.shareCoverImageView)
        }
    }

    override fun getItemCount(): Int {
        return shares.size
    }

    fun setItems(shares: List<Share>) {
        this.shares = shares
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Share {
        return shares[id]
    }

    inner class ViewHolder(val item: ItemHorizontalShareBinding) : RecyclerView.ViewHolder(item.root) {
        init {
            item.shareTitleTextView.isSelected = true
            item.shareSubtitleTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }

            item.shareButton.setOnClickListener { onLongClick() }
        }

        private fun onClick() {
            val bundle = Bundle()
            bundle.putParcelable(Constants.SHARE_OBJECT, shares[bindingAdapterPosition])
            click.onShareClick(bundle)
        }

        private fun onLongClick(): Boolean {
            val bundle = Bundle()
            bundle.putParcelable(Constants.SHARE_OBJECT, shares[bindingAdapterPosition])
            click.onShareLongClick(bundle)
            return true
        }
    }
}
