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
import com.example.automate.R
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
        val chainToRecognize = "abcd"
        val transitionFunction = TransitionFunction()
        transitionFunction.add(TransitionRule("A", "a"), arrayOf("A"))
        transitionFunction.add(TransitionRule("A", "b"), arrayOf("A"))
        transitionFunction.add(TransitionRule("A", "c"), arrayOf("A"))
        transitionFunction.add(TransitionRule("A", "d"), arrayOf("A"))
        val acceptStates = arrayOf("A", "B", "C", "D")
        val states = arrayOf("A", "B", "C", "D")
        val alphabet = arrayOf("a", "b", "c", "d")
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

        createTableButton.setOnClickListener {
            prepareStates()
            createTransitionsTable()
        }

        checkButton.setOnClickListener {
            prepareStates()
            createTransitionsFromTable()
            checkChain()
        }
        addNonTerminalFilter()
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
                createTransitionsTable()
                fillTransitionsTable(automate.transitionFunction)
            }

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
    }

    fun fillTransitionsTable(transitionFunction: TransitionFunction) {
        for (rowIndex in 0 until transitionsTable.childCount) {
            val row = transitionsTable.getChildAt(rowIndex) as TableRow
            for (colIndex in 0 until row.childCount) {
                val element = row.getChildAt(colIndex)
                if (rowIndex > 0 && colIndex > 0) {
                    val state = states.getOrNull(rowIndex - 1) ?: continue
                    val symbol = alphabet.getOrNull(colIndex - 1) ?: continue
                    val transition = transitionFunction.table.get(TransitionRule(state, symbol))?: continue
                    (element as? EditText)?.setText(transition.joinToString())
                }
            }
        }
    }

    fun createTransitionsTable() {
        transitionsTable.removeAllViews()
        val tableHeight = states.size + 1
        val tableWidth = alphabet.size + 1
        val previousHeight = transitionsTable.childCount
        val previousWidth = if (transitionsTable.getChildAt(0) != null) {
            (transitionsTable.getChildAt(0) as TableRow).childCount
        } else {
            0
        }

        val heightDifference = tableHeight - previousHeight
        val widthDifference = tableWidth - previousWidth

        for (i in 0 until tableHeight) {
            val tableRow = TableRow(context).apply {
                layoutParams = TableRow.LayoutParams().apply {
                    setGravity(Gravity.BOTTOM)
                }
            }
            for (j in 0 until tableWidth) {
                val viewToAdd = if (i == 0 || j == 0) {
                    TextView(context).apply {
                        val text = if (i == 0 && j > 0) {
                            alphabet.getOrNull(j - 1) ?: ""
                        } else if (i > 0 && j == 0) {
                            states.getOrNull(i - 1) ?: ""
                        } else ""
                        setText(text)
                        setBackgroundColor(Color.WHITE)
                        layoutParams = TableRow.LayoutParams().apply {
                            setMargins(2, 2, 2, 2)
                            setHeight(150)
                        }
                        gravity = Gravity.CENTER
                        setWidth(100)
                        setHeight(150)
                    }
                } else {
                    EditText(context).apply {
                        setBackgroundColor(Color.WHITE)
                        layoutParams = TableRow.LayoutParams().apply {
                            setMargins(2, 2, 2, 2)
                        }
                        gravity = Gravity.CENTER
                        setWidth(100)
                        setHeight(150)
                    }
                }
                tableRow.addView(viewToAdd)
            }
            transitionsTable.addView(tableRow)
        }
    }

    fun createTransitionsFromTable() {
        TransitionFunction()
        for (i in 1 until transitionsTable.childCount) {
            val tableRow = transitionsTable.getChildAt(i) as TableRow
            for (j in 1 until tableRow.childCount) {
                val transition = getTableElement(i, j)
                val state = getTableElement(i, 0)
                val symbol = getTableElement(0, j)
                transitionFunction.add(
                    TransitionRule(state, symbol),
                    transition.toCharArray().map { it.toString() }.toTypedArray()
                )
            }
        }
    }

    fun getTableElement(i: Int, j: Int): String {
        val view = (transitionsTable.getChildAt(i) as TableRow).getChildAt(j)
        return when (view) {
            is TextView -> view.text.toString()
            is EditText -> view.text.toString()
            else -> ""
        }
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