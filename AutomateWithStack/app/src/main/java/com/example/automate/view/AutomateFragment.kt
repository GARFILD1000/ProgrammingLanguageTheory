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
    var startState = 0
    var states = emptyArray<Int>()
    var alphabet = emptyArray<String>()
    var transitionFunction = TransitionFunction()
    var acceptStates = emptyArray<Int>()
    var startStack = emptyArray<String>()

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
//        debug()
        return binding.root
    }

    fun debug() {
        val chainToRecognize = "000111"
        val transitionFunction = TransitionFunction()
        transitionFunction.add(TransitionRule(0, "0", emptyList<String>()), DestinationRule(0,listOf("0")))
        transitionFunction.add(TransitionRule(0, "0", listOf<String>("0")), DestinationRule(0,listOf("0","0")))
        transitionFunction.add(TransitionRule(0, "1", listOf<String>("0")), DestinationRule(1,emptyList<String>()))
        transitionFunction.add(TransitionRule(0, "1", listOf<String>("0")), DestinationRule(1,emptyList<String>()))
        transitionFunction.add(TransitionRule(0, "", emptyList<String>()), DestinationRule(2,emptyList<String>()))
        transitionFunction.add(TransitionRule(0, "", emptyList<String>()), DestinationRule(2,emptyList<String>()))
        val acceptStates = arrayOf(2)
        val states = arrayOf(0, 1, 2)
        val alphabet = arrayOf("0", "1")
        val startState = 0
        val startStack = arrayOf<String>()
        val automate = Automate(states, alphabet, transitionFunction, startState, acceptStates, startStack)
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
                spinnerArrayAdapter.addAll(it.split(" "))
            }
        }

        addRuleButton.setOnClickListener {
            addTransitionRule()
        }

        checkButton.setOnClickListener {
            prepareStates()
            checkChain()
        }

        createRuleList()
    }

    fun loadAutomate() {
        launch {
            LanguageRepository.getFirstAutomate()?.let { automate ->
                Log.d(LOG_TAG, "Loaded automate: \n$automate")
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
                spinnerArrayAdapter.addAll(automate.states.map{it.toString()})
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
        val automate = Automate(states, alphabet, transitionFunction, startState, acceptStates, startStack)
        launch {
            LanguageRepository.deleteAllAutomate()
            LanguageRepository.insertAutomate(automate)
            Log.d(LOG_TAG, "Saved automate: \n$automate")
        }
        val result = automate.recognize(chain)
        Toast.makeText(context, "Is recognized = ${result.isRecognized}", Toast.LENGTH_LONG).show()
        resultTextView.setText(result.description)
    }

    fun prepareStates() {
        var statesList = mutableListOf<Int>()
        statesEditText.text.toString().split(" ").forEach {
            it.toIntOrNull()?.let{
                statesList.add(it)
            }
        }
        states = statesList.toTypedArray()
        alphabet = alphabetEditText.text.toString().toCharArray().toList().map { it.toString() }
            .toTypedArray()
        statesList = mutableListOf<Int>()
        acceptStatesEditText.text.toString().split(" ").forEach {
            it.toIntOrNull()?.let{
                statesList.add(it)
            }
        }
        acceptStates = statesList.toTypedArray()
        startStateSpinner.selectedItem?.toString()?.toIntOrNull()?.let {
            startState = it
        }
        transitionFunction.clear()
        adapter.getItems().forEach{
            transitionFunction.add(it.first, it.second)
        }
        startStack = startStackEditText.text.toString().toCharArray().toList().map{it.toString()}.toTypedArray()
    }

    fun addTransitionRule(){
        adapter.addItem(Pair(TransitionRule(0,"", emptyList()), DestinationRule(0, emptyList())))
    }

    override fun onPause() {
        super.onPause()
    }
}