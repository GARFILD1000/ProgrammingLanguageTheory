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
import com.example.grammar.model.GrammarRule
import com.example.grammar.model.Grammar
import com.example.grammar.R
import com.example.grammar.Adapters.RulesAdapter
import com.example.grammar.repository.GrammarRepository
import com.example.grammar.databinding.FragmentGrammarBinding
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
    var loadedGrammar: Grammar? = null
    lateinit var binding: FragmentGrammarBinding
    lateinit var rulesListAdapter: RulesAdapter
    lateinit var spinnerArrayAdapter: ArrayAdapter<Char>
    var nonTerminals = ""

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
        rulesListAdapter = RulesAdapter()
        rulesListAdapter.onDeleteItem = {
            rulesListAdapter.removeItem(it)
        }
        rulesList.adapter = rulesListAdapter
        rulesList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rulesList.setHasFixedSize(true)

        spinnerArrayAdapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, LinkedList<Char>())
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        startSelector.adapter = spinnerArrayAdapter

        nonTerminalEditText.doOnTextChanged { text, start, count, after ->
            text?.toString()?.toCharArray()?.toList()?.let{
                nonTerminals = text.toString()
                spinnerArrayAdapter.clear()
                spinnerArrayAdapter.addAll(it)
            }
        }

        addRuleButton.setOnClickListener{
            rulesListAdapter.addItem(GrammarRule())
        }
        createButton.setOnClickListener {
            createChains()
        }
        separateButton.setOnClickListener {
            val parsedRules = parseRules(rulesListAdapter.getItems())
            rulesListAdapter.removeItems()
            rulesListAdapter.setItems(parsedRules)
        }
        simplifyButton.setOnClickListener {
            val simplifiedRules = simplifyRules(rulesListAdapter.getItems())
            rulesListAdapter.removeItems()
            rulesListAdapter.setItems(simplifiedRules)
        }

        addNonTerminalFilter()
        loadGrammarFromDatabase()
    }

    private fun loadGrammarFromDatabase(){
        repository = GrammarRepository(activity!!.application)
        launch {
            loadedGrammar = repository.getFirst()
            if (loadedGrammar == null) {
                loadedGrammar = Grammar()
                repository.insertGrammar(loadedGrammar!!)
            }
            setGrammar(loadedGrammar!!)
        }
    }

    private fun setGrammar(grammar: Grammar){
        nonTerminalEditText.setText(grammar.nonTerminals)
        spinnerArrayAdapter.clear()
        spinnerArrayAdapter.addAll(grammar.nonTerminals.toCharArray().asList())
        rulesListAdapter.removeItems()
        rulesListAdapter.setItems(simplifyRules(grammar.rules))
    }

    private fun addNonTerminalFilter(){
        val editFilters = nonTerminalEditText.filters
        val newFilters = arrayOfNulls<InputFilter>(editFilters.size + 1)
//        for (index in 0 until editFilters.size){
//            newFilters[index] = editFilters[index]
//        }
        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.size)
        newFilters[newFilters.size - 1] =  InputFilter.AllCaps()
        nonTerminalEditText.filters = newFilters

        nonTerminalEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?:return
                for (i in 0 until s.length) {
                    val character = s.get(i)
                    if (i < s.length-1) {
                        val nextIndex = s.indexOf(character, i + 1)
                        if (nextIndex > 0) {
                            s.replace(nextIndex, nextIndex+1, "")
                            break
                        }
                    }
//                    if (!Character.isLetter(s.get(i))) {
//
//                    }
                }
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun parseRules(unparsedRules: LinkedList<GrammarRule>): LinkedList<GrammarRule>{
        val parsedRules = LinkedList<GrammarRule>()
        for (unparsedRule in unparsedRules) {
            val unparsedRightPart = unparsedRule.rightPart//.replace(" ", "")
            if (unparsedRightPart.contains("|")){
                val rulesParts = unparsedRightPart.split("|")
                for (rulePart in rulesParts){
                    var rulePartEmptyless = rulePart.replace(" ","")
                    parsedRules.add(GrammarRule().apply {
                        leftPart = unparsedRule.leftPart
                        this.rightPart = rulePartEmptyless
                    })
                }
            }
            else{
                var rulePartEmptyless = unparsedRightPart.replace(" ","")
                parsedRules.add(GrammarRule().apply {
                    this.leftPart = unparsedRule.leftPart
                    this.rightPart = rulePartEmptyless
                })
            }
        }
        return parsedRules
    }

    private fun simplifyRules(rules: LinkedList<GrammarRule>): LinkedList<GrammarRule>{
        val parsedRules = parseRules(rules)
        val simplifiedRules = LinkedList<GrammarRule>()
        val differentLeftParts = LinkedList<String>()
        for (parsedRule in parsedRules){
            if (!differentLeftParts.contains(parsedRule.leftPart)){
                differentLeftParts.addLast(parsedRule.leftPart)
            }
        }
        for (leftPart in differentLeftParts){
            val ruleBuilder = StringBuilder()
            val newRule = GrammarRule().apply { this.leftPart = leftPart }
            for (parsedRule in parsedRules){
                if (parsedRule.leftPart == leftPart){
                    if (ruleBuilder.length > 0){
                        ruleBuilder.append(" | ")
                    }
                    ruleBuilder.append(parsedRule.rightPart)
                }
            }
            newRule.rightPart = ruleBuilder.toString()
            simplifiedRules.add(newRule)
        }
        return simplifiedRules
    }

    private fun createChains(){
        val startLength = startLengthEditText.text.toString().toIntOrNull()
        val endLength = endLengthEditText.text.toString().toIntOrNull()
        startLength?:return
        endLength?:return

        var startNonTerminal = ' '
        startSelector.selectedItem?.let{
            startNonTerminal = it as Char
        }
        val parsedRules = parseRules(rulesListAdapter.getItems())
        Log.d(LOG_TAG, "Parsed rules: $parsedRules")
        for (rule in parsedRules) {
            Log.d("Rule: ", "${rule.leftPart} -> ${rule.rightPart}")
        }

        loadedGrammar?.apply{
            this.nonTerminals = this@GrammarFragment.nonTerminals
            this.rules = parsedRules
            this.terminals = parseTerminalsFromRules(this.rules)
            this.startSymbol = startNonTerminal

            Log.d(LOG_TAG, "Start symbol: ${this.startSymbol}")
            Log.d(LOG_TAG, "Non terminals: ${nonTerminals}")
            launch {
                repository.insertGrammar(loadedGrammar!!)
            }
            launch {
                val createdChains = withContext(Dispatchers.Default) {
                     createChains(startLength, endLength)
                }
                (context as MainActivity).createdChains = createdChains
                (context as MainActivity).goToChainsList()
            }
        }

        Toast.makeText(context, "Create chains length from $startLength to $endLength", Toast.LENGTH_SHORT).show()

    }

    override fun onPause() {
        loadedGrammar?.let {
            launch {
                repository.insertGrammar(it)
            }
        }
        super.onPause()
    }
}