package com.example.automate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.automate.model.GrammarRule
import com.example.automate.R
import com.example.automate.databinding.ListRuleItemBinding
import java.util.*

class RulesAdapter() : RecyclerView.Adapter<RulesAdapter.ViewHolder>(){
    private var items: LinkedList<GrammarRule> = LinkedList()
    var onDeleteItem: (itemNumber: Int)->Unit = {}

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
        val itemToBind = items[position]
        holder.bind(itemToBind)
    }

    inner class ViewHolder(binding: ListRuleItemBinding): RecyclerView.ViewHolder(binding.root){
        val ruleLeft = binding.ruleLeft
        val ruleRight = binding.ruleRight
        val deleteButton = binding.deleteRule
        val addRulePartButton = binding.addRulePart
        val editRightPart = binding.ruleRight

        fun bind(item: GrammarRule){
            ruleLeft.setText(item.leftPart)
            ruleRight.setText(item.rightPart)
            editRightPart.setSelection(editRightPart.text.length)
//            ruleRight.addTextChangedListener(object: TextWatcher{
//                override fun afterTextChanged(p0: Editable?) {}
//                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//
//                }
//            })
            ruleLeft.doOnTextChanged { text, start, count, after ->
                item.leftPart = text.toString()
            }
            ruleRight.doOnTextChanged { text, start, count, after ->
                item.rightPart = text.toString()
            }
            deleteButton.setOnClickListener {
                if (this.adapterPosition != RecyclerView.NO_POSITION){
                    onDeleteItem(this.adapterPosition)
                }
            }
            addRulePartButton.setOnClickListener {
                val position = this.adapterPosition
                if (position != RecyclerView.NO_POSITION){
                    val rulePartSeparator = " | "
                    items.getOrNull(position)?.let{
                        if (!it.rightPart.endsWith(rulePartSeparator)){
                            it.rightPart += rulePartSeparator
                            notifyItemChanged(position)
                        }
                    }
                }
            }
        }
    }
}