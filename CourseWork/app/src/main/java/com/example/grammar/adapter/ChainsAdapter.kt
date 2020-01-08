package com.example.grammar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.model.Chain
import com.example.grammar.R
import com.example.grammar.databinding.ListChainItemBinding
import java.util.*

class ChainsAdapter() : RecyclerView.Adapter<ChainsAdapter.ViewHolder>(){
    private var items: LinkedList<Chain> = LinkedList()
    var onDeleteItem: (itemNumber: Int)->Unit = {}
    var onChainClick: (Chain)-> Unit = {}


    fun getItems(): LinkedList<Chain> = items
    override fun getItemCount(): Int = items.size

    fun setItems(newItems: LinkedList<Chain>){
        items = newItems
        notifyDataSetChanged()
    }

    fun addItem(newItem: Chain){
        items.add(newItem)
        notifyItemInserted(items.size - 1)
    }

    fun removeItems(){
        val previousSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, previousSize)
    }

    fun removeItem(index: Int){
        items.removeAt(index)
        notifyItemRemoved(index)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ListChainItemBinding>(inflater,
            R.layout.list_chain_item, parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemToBind = items[holder.adapterPosition]
        holder.bind(itemToBind)
    }

    inner class ViewHolder(binding: ListChainItemBinding): RecyclerView.ViewHolder(binding.root){
        val chainText = binding.chainTextView

        fun bind(item: Chain){
            chainText.setText(item.data.joinToString(""))
            chainText.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION){
                    onChainClick(item)
                }
            }
        }
    }
}