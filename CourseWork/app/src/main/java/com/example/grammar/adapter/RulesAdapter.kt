package com.example.grammar.adapter

import com.example.grammar.databinding.ListRuleItemBinding
import com.example.grammar.model.GrammarRule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.model.Chain
import com.example.grammar.R
import com.example.grammar.databinding.ListChainItemBinding
import java.util.*

class RulesAdapter() : RecyclerView.Adapter<RulesAdapter.ViewHolder>(){
    private var items: LinkedList<GrammarRule> = LinkedList()
    var onDeleteItem: (itemNumber: Int)->Unit = {}
    var onChainClick: (Chain)-> Unit = {}


    fun getItems(): LinkedList<GrammarRule> = items
    override fun getItemCount(): Int = items.size

    fun setItems(newItems: LinkedList<GrammarRule>){
        items = newItems
        notifyDataSetChanged()
    }

    fun addItem(newItem: GrammarRule){
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
        val binding = DataBindingUtil.inflate<ListRuleItemBinding>(inflater,
            R.layout.list_rule_item, parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemToBind = items[holder.adapterPosition]
        holder.bind(itemToBind)
    }

    inner class ViewHolder(binding: ListRuleItemBinding): RecyclerView.ViewHolder(binding.root){
        val ruleText = binding.ruleTextView
        val ruleNumberText = binding.ruleNumberTextView

        fun bind(item: GrammarRule){
            ruleText.setText(item.toString())
            ruleNumberText.setText("${adapterPosition+1})")
        }
    }
}