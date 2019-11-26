package com.example.grammar.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.Adapters.ChainsAdapter
import com.example.grammar.model.Chain
import com.example.grammar.R
import com.example.grammar.databinding.FragmentChainsBinding
import kotlinx.android.synthetic.main.fragment_chains.*

class ChainsFragment: Fragment() {
    lateinit var binding: FragmentChainsBinding
    lateinit var chainsListAdapter: ChainsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_chains, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chainsListAdapter = ChainsAdapter()
        chainsList.adapter = chainsListAdapter
        chainsList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        chainsList.setHasFixedSize(true)
        val chains = (context as MainActivity).createdChains
        for (chain in chains){
            chainsListAdapter.addItem(chain)
        }
        chainsCount.setText(chainsCount.text.toString() + chains.size)
        chainsListAdapter.onChainClick = { chain ->
            chain.graph = chain.graph?.root
            (context as MainActivity).chainToShow = chain
            (context as MainActivity).goToGraph()
        }
    }
}