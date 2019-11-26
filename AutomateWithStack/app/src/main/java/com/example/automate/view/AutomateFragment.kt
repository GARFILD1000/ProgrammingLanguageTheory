package com.example.automate.view

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.automate.R
import com.example.automate.adapters.TransitionRulesAdapter
import com.example.automate.databinding.FragmentAutomateBinding
import com.example.automate.model.*
import com.example.automate.repository.LanguageRepository
import kotlinx.android.synthetic.main.fragment_automate.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class AutomateFragment : Fragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisorJob
    val supervisorJob = SupervisorJob()
    val LOG_TAG = "AutomateFragment"
    val adapter = TransitionRulesAdapter()


    lateinit var binding: FragmentAutomateBinding


    lateinit var spinnerArrayAdapter: ArrayAdapter<String>
    var startState = ""
    var states = emptyArray<String>()
    var alphabet = emptyArray<String>()
    var transitionFunction = TransitionFunction()
    var acceptStates = emptyArray<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_automate, container, false
        )
        binding.lifecycleOwner = this
        debug()
        return binding.root
    }

    fun debug() {
        val chainToRecognize = "000111"
        val transitionFunction = TransitionFunction()
        transitionFunction.add(TransitionRule("A", "0", emptyList<String>()), DestinationRule("A",listOf("0")))
        transitionFunction.add(TransitionRule("A", "0", listOf<String>("0")), DestinationRule("A",listOf("0","0")))
        transitionFunction.add(TransitionRule("A", "1", listOf<String>("0")), DestinationRule("B",emptyList<String>()))
        transitionFunction.add(TransitionRule("B", "1", listOf<String>("0")), DestinationRule("B",emptyList<String>()))
        transitionFunction.add(TransitionRule("B", "", emptyList<String>()), DestinationRule("C",emptyList<String>()))
        transitionFunction.add(TransitionRule("A", "", emptyList<String>()), DestinationRule("C",emptyList<String>()))
        val acceptStates = arrayOf("C")
        val states = arrayOf("A", "B", "C")
        val alphabet = arrayOf("0", "1")
        val startState = "A"
        val automate = Automate(states, alphabet, transitionFunction, startState, acceptStates)
        automate.recognize(chainToRecognize).let {
            Log.d(
                "Debug",
                "chain ${chainToRecognize} is recognized ${it.isRecognized}: ${it.description}"
            )
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAutomate()
        spinnerArrayAdapter =
            ArrayAdapter(context!!, android.R.layout.simple_spinner_item, LinkedList<String>())
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        startStateSpinner.adapter = spinnerArrayAdapter

        statesEditText.doOnTextChanged { text: CharSequence?, start: Int, count: Int, after: Int ->
            spinnerArrayAdapter.clear()
            text?.let {
                spinnerArrayAdapter.addAll(it.toList().map { "$it" })
            }
        }

        addRuleButton.setOnClickListener {
            addTransitionRule()
        }

        checkButton.setOnClickListener {
            prepareStates()
            checkChain()
        }


        addNonTerminalFilter()
        createRuleList()
    }

    fun loadAutomate() {
        launch {
            LanguageRepository.getFirstAutomate()?.let { automate ->
                val builder = StringBuilder()
                automate.states.forEach { builder.append(it) }
                statesEditText.setText(builder.toString())
                builder.clear()

                automate.alphabet.forEach { builder.append(it) }
                alphabetEditText.setText(builder.toString())
                builder.clear()

                automate.acceptStates.forEach { builder.append(it) }
                acceptStatesEditText.setText(builder.toString())
                builder.clear()

                spinnerArrayAdapter.clear()
                spinnerArrayAdapter.addAll(automate.states.toList())
                Log.d(
                    "AutomateFragment",
                    "Selected index ${automate.states.indexOf(automate.startState)} of element${automate.startState}"
                )
                startStateSpinner.setSelection(automate.states.indexOf(automate.startState))

                prepareStates()
                adapter.setItems(automate.transitionFunction.table)
            }
        }
    }

    fun createRuleList(){
        ruleList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        ruleList.adapter = adapter
        adapter.onDeleteItem = {
            adapter.removeItem(it)
        }
    }

    fun checkChain() {
        val chain = inputChain.text.toString()
        val automate = Automate(states, alphabet, transitionFunction, startState, acceptStates)
        launch {
            LanguageRepository.deleteAllAutomate()
            LanguageRepository.insertAutomate(automate)
        }
        val result = automate.recognize(chain)
        Toast.makeText(context, "Is recognized = ${result.isRecognized}", Toast.LENGTH_LONG).show()
        resultTextView.setText(result.description)
    }

    fun prepareStates() {
        states = statesEditText.text.toString().toCharArray().toList().map { it.toString() }
            .toTypedArray()
        alphabet = alphabetEditText.text.toString().toCharArray().toList().map { it.toString() }
            .toTypedArray()
        acceptStates =
            acceptStatesEditText.text.toString().toCharArray().toList().map { it.toString() }
                .toTypedArray()
        startStateSpinner.selectedItem?.toString()?.let {
            startState = it
        }
        transitionFunction.clear()
        adapter.getItems().forEach{
            transitionFunction.add(it.first, it.second)
        }
    }

    fun addTransitionRule(){
        adapter.addItem(Pair(TransitionRule("","", emptyList()), DestinationRule("", emptyList())))
    }


    private fun addNonTerminalFilter() {
//        val editFilters = nonTerminalEditText.filters
//        val newFilters = arrayOfNulls<InputFilter>(editFilters.size + 1)
////        for (index in 0 until editFilters.size){
////            newFilters[index] = editFilters[index]
////        }
//        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.size)
//        newFilters[newFilters.size - 1] =  InputFilter.AllCaps()
//        nonTerminalEditText.filters = newFilters
//
//        nonTerminalEditText.addTextChangedListener(object: TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                s?:return
//                for (i in 0 until s.length) {
//                    val character = s.get(i)
//                    if (i < s.length-1) {
//                        val nextIndex = s.indexOf(character, i + 1)
//                        if (nextIndex > 0) {
//                            s.replace(nextIndex, nextIndex+1, "")
//                            break
//                        }
//                    }
////                    if (!Character.isLetter(s.get(i))) {
////
////                    }
//                }
//            }
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//        })
    }

    private fun parseRules(unparsedRules: LinkedList<GrammarRule>): LinkedList<GrammarRule> {
        val parsedRules = LinkedList<GrammarRule>()
        for (unparsedRule in unparsedRules) {
            val unparsedRightPart = unparsedRule.rightPart//.replace(" ", "")
            if (unparsedRightPart.contains("|")) {
                val rulesParts = unparsedRightPart.split("|")
                for (rulePart in rulesParts) {
                    var rulePartEmptyless = rulePart.replace(" ", "")
                    parsedRules.add(GrammarRule().apply {
                        leftPart = unparsedRule.leftPart
                        this.rightPart = rulePartEmptyless
                    })
                }
            } else {
                var rulePartEmptyless = unparsedRightPart.replace(" ", "")
                parsedRules.add(GrammarRule().apply {
                    this.leftPart = unparsedRule.leftPart
                    this.rightPart = rulePartEmptyless
                })
            }
        }
        return parsedRules
    }

    private fun simplifyRules(rules: LinkedList<GrammarRule>): LinkedList<GrammarRule> {
        val parsedRules = parseRules(rules)
        val simplifiedRules = LinkedList<GrammarRule>()
        val differentLeftParts = LinkedList<String>()
        for (parsedRule in parsedRules) {
            if (!differentLeftParts.contains(parsedRule.leftPart)) {
                differentLeftParts.addLast(parsedRule.leftPart)
            }
        }
        for (leftPart in differentLeftParts) {
            val ruleBuilder = StringBuilder()
            val newRule = GrammarRule().apply { this.leftPart = leftPart }
            for (parsedRule in parsedRules) {
                if (parsedRule.leftPart == leftPart) {
                    if (ruleBuilder.length > 0) {
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

    override fun onPause() {
        super.onPause()
    }
}