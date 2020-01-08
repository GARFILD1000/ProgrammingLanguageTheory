package com.example.automate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.automate.model.GrammarRule
import com.example.automate.R
import com.example.automate.databinding.ListRuleItemBinding
import com.example.automate.databinding.ListTransitionRuleItemBinding
import com.example.automate.model.DestinationRule
import com.example.automate.model.TransitionRule
import java.util.*

class TransitionRulesAdapter() : RecyclerView.Adapter<TransitionRulesAdapter.ViewHolder>(){
    private var items: LinkedList<Pair<TransitionRule,DestinationRule>> = LinkedList()
    var onDeleteItem: (itemNumber: Int)->Unit = {}

    fun getItems(): LinkedList<Pair<TransitionRule,DestinationRule>> = items
    override fun getItemCount(): Int = items.size

    fun setItems(newItems: LinkedList<Pair<TransitionRule,DestinationRule>>){
        items = newItems
        notifyDataSetChanged()
    }

    fun addItem(newItem: Pair<TransitionRule,DestinationRule>){
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
        val binding = DataBindingUtil.inflate<ListTransitionRuleItemBinding>(inflater,
            R.layout.list_transition_rule_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemToBind = items[position]
        holder.bind(itemToBind)
    }

    inner class ViewHolder(binding: ListTransitionRuleItemBinding): RecyclerView.ViewHolder(binding.root){
        val ruleLeftState = binding.ruleLeftState
        val ruleLeftSymbol = binding.ruleLeftSymbol
        val ruleLeftStack = binding.ruleLeftStack

        val ruleRightState = binding.ruleRightState
        val ruleRightStack = binding.ruleRightStack
        val deleteRule = binding.deleteRule

        fun bind(item: Pair<TransitionRule, DestinationRule>){
            ruleLeftState.setText(item.first.state.toString())
            ruleLeftSymbol.setText(item.first.symbol)
            ruleLeftStack.setText(item.first.stack.joinToString(""))

            ruleRightState.setText(item.second.state.toString())
            ruleRightStack.setText(item.second.stack.joinToString(""))
            ruleLeftState.doOnTextChanged { text, start, count, after ->
                text.toString().toIntOrNull()?.let{
                    item.first.state = it
                }
            }
            ruleLeftSymbol.doOnTextChanged { text, start, count, after ->
                item.first.symbol = text.toString()
            }
            ruleLeftStack.doOnTextChanged { text, start, count, after ->
                item.first.stack = text?.map { it.toString() } ?: emptyList()
            }
            ruleRightState.doOnTextChanged { text, start, count, after ->
                text.toString().toIntOrNull()?.let{
                    item.second.state = it
                }
            }
            ruleRightStack.doOnTextChanged { text, start, count, after ->
                item.second.stack = text?.map { it.toString() } ?: emptyList()
            }
            deleteRule.setOnClickListener {
                if (this.adapterPosition != RecyclerView.NO_POSITION){
                    onDeleteItem(this.adapterPosition)
                }
            }
        }
    }
}