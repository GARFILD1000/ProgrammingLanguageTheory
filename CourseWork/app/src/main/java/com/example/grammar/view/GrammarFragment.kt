package com.example.grammar.view

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.model.Grammar
import com.example.grammar.R
import com.example.grammar.repository.GrammarRepository
import com.example.grammar.databinding.FragmentGrammarBinding
import com.example.grammar.model.GrammarRule
import com.example.grammar.model.Symbol
import com.example.grammar.util.RegularRulesGenerator
import kotlinx.android.synthetic.main.fragment_grammar.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class GrammarFragment: Fragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisorJob
    val supervisorJob = SupervisorJob()

    val LOG_TAG = "GrammarFragment"

    lateinit var repository: GrammarRepository
    var generatedGrammar: Grammar? = null
    lateinit var binding: FragmentGrammarBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_grammar, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createButton.setOnClickListener {
            createChains()
        }

    }

    private fun loadGrammarFromDatabase(){
        var loadedGrammar: Grammar? = null
        repository = GrammarRepository(activity!!.application)
        launch {
            withContext(Dispatchers.IO) {
                loadedGrammar = repository.getFirst()
                if (loadedGrammar == null) {
                    loadedGrammar = Grammar()
                    repository.insertGrammar(loadedGrammar!!)
                }
            }
            Log.d(LOG_TAG, "Loaded grammar: \n$loadedGrammar")
        }
    }

    private fun createChains(){
        val minLength = minLengthEditText.text.toString().toIntOrNull()
        val maxLength = maxLengthEditText.text.toString().toIntOrNull()
        val multiplicity = multiplicityEditText.text.toString().toIntOrNull()
        val terminals = terminalEditText.text.toString().toCharArray().map{ Symbol(it.toString(), true)}
        var startChain = startChainEditText.text.toString()
        var endChain = endChainEditText.text.toString()
        var leftLinearOutput = leftLinearOutputButton.isChecked
        minLength?:return
        maxLength?:return
        multiplicity?:return

        val generator = RegularRulesGenerator(terminals, leftLinearOutput, multiplicity)
        generator.addLastBlock(RegularRulesGenerator.Block(startChain, false))
        generator.addLastBlock(RegularRulesGenerator.Block("", true))
        generator.addLastBlock(RegularRulesGenerator.Block(endChain, false))
        val grammar = generator.generateGrammar()
        Log.d(LOG_TAG, "Generated grammar:\n$grammar")
//
//        launch {
//            val createdChains = withContext(Dispatchers.Default) {
//                grammar.createChains(minLength, maxLength)
//            }
//            (context as MainActivity).createdChains = createdChains
//            (context as MainActivity).goToChainsList()
//        }
        Toast.makeText(context, "Creating chains...", Toast.LENGTH_SHORT).show()

    }

    override fun onPause() {
        super.onPause()
    }
}