package com.example.grammar.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.R
import com.example.grammar.adapter.RulesAdapter
import com.example.grammar.databinding.FragmentGraphBinding
import kotlinx.android.synthetic.main.fragment_graph.*

class GraphFragment: Fragment() {
    lateinit var binding: FragmentGraphBinding
    private val LOG_TAG = "GraphFragment"
    var rulesAdapter = RulesAdapter()
    lateinit var rulesLayoutManager: LinearLayoutManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_graph, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (context as MainActivity).chainToShow.let{chain ->
            chainView.setText(chain.symbols.joinToString(""))
            (context as MainActivity).generatorResults?.grammar?.createTree(chain)?.let{
                graphView.setGraph(it)
            }
            rulesList.adapter = rulesAdapter
            rulesLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            rulesList.layoutManager = rulesLayoutManager
            rulesAdapter.setItems(chain.rules)
        }
    }
}