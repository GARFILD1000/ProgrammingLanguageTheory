package com.example.grammar.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.grammar.R
import com.example.grammar.databinding.FragmentGraphBinding
import kotlinx.android.synthetic.main.fragment_graph.*

class GraphFragment: Fragment() {
    lateinit var binding: FragmentGraphBinding
    private val LOG_TAG = "GraphFragment"
    
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
        (context as MainActivity).chainToShow.let{
            chainView.setText(it.data)
            if (it.graph != null) {
                Log.d(LOG_TAG, "Show graph")
                graphView.setChain(it)
            }
        }
    }
}