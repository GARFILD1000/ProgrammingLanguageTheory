package com.example.grammar.adapter

import com.example.grammar.databinding.ListRuleItemBinding
import com.example.grammar.model.GrammarRule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.App
import com.example.grammar.model.Chain
import com.example.grammar.R
import com.example.grammar.databinding.ListChainItemBinding
import com.example.grammar.databinding.ListFileItemBinding
import java.io.File
import java.util.*

class FilesAdapter() : RecyclerView.Adapter<FilesAdapter.ViewHolder>(){
    private var items: LinkedList<File> = LinkedList()
    var onDeleteItem: (itemNumber: Int)->Unit = {}
    var onItemClick: (itemNumber: Int)-> Unit = {}
    fun getItems(): LinkedList<File> = items
    override fun getItemCount(): Int = items.size
    var selectedItem = -1

    fun getItem(idx: Int): File? = items.getOrNull(idx)

    fun setItems(newItems: Collection<File>){
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(newItem: File){
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

    fun selectItem(item: Int){
        val previousSelection = selectedItem
        selectedItem = item
        if (previousSelection in items.indices) {
            notifyItemChanged(previousSelection)
        }
        notifyItemChanged(selectedItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ListFileItemBinding>(inflater,
            R.layout.list_file_item, parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemToBind = items[holder.adapterPosition]
        holder.bind(itemToBind)
    }

    inner class ViewHolder(binding: ListFileItemBinding): RecyclerView.ViewHolder(binding.root){
        val fileName = binding.filenameTextView
        val deleteFileButton = binding.deleteFileButton
        val background = binding.background
        fun bind(item: File){
            if (adapterPosition == selectedItem) {
                background.setBackgroundColor(ContextCompat.getColor(App.getContext(), R.color.colorSelection))
            } else {
                background.setBackgroundColor(ContextCompat.getColor(App.getContext(), R.color.colorBackground))
            }
            fileName.setText(item.name)
            fileName.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(adapterPosition)
                }
            }
            deleteFileButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteItem(adapterPosition)
                }
            }
        }
    }
}