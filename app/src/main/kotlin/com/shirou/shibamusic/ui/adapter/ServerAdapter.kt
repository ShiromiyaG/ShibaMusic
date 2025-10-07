package com.shirou.shibamusic.ui.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.ItemLoginServerBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.model.Server

class ServerAdapter(private val click: ClickCallback) : RecyclerView.Adapter<ServerAdapter.ViewHolder>() {

    private var servers: List<Server> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemLoginServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val server = servers[position]
        holder.item.serverNameTextView.text = server.serverName
        holder.item.serverAddressTextView.text = server.address
    }

    override fun getItemCount(): Int = servers.size

    fun setItems(servers: List<Server>) {
        this.servers = servers
        notifyDataSetChanged()
    }

    fun getItem(id: Int): Server = servers[id]

    inner class ViewHolder(val item: ItemLoginServerBinding) : RecyclerView.ViewHolder(item.root) {

        init {
            item.serverNameTextView.isSelected = true

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { onLongClick() }
        }

        fun onClick() {
            val bundle = Bundle().apply {
                putParcelable("server_object", servers[bindingAdapterPosition])
            }
            click.onServerClick(bundle)
        }

        fun onLongClick(): Boolean {
            val bundle = Bundle().apply {
                putParcelable("server_object", servers[bindingAdapterPosition])
            }
            click.onServerLongClick(bundle)
            return true
        }
    }
}
