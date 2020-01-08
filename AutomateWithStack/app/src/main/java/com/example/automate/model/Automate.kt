package com.example.automate.model

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.automate.util.IntArrayConverter
import com.example.automate.util.StringArrayConverter
import com.example.automate.util.StringListConverter
import com.example.automate.util.TransitionFunctionConverter
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

data class RecognitionResult(var isRecognized: Boolean, var description: String)

@Entity(tableName = "automate")
class Automate {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @TypeConverters(IntArrayConverter::class)
    var states: Array<Int> = arrayOf()
    @TypeConverters(StringArrayConverter::class)
    var alphabet: Array<String> = arrayOf<String>()
    @TypeConverters(TransitionFunctionConverter::class)
    var transitionFunction: TransitionFunction
    var startState: Int = 0
    @TypeConverters(IntArrayConverter::class)
    var acceptStates: Array<Int> = arrayOf()
    @TypeConverters(StringArrayConverter::class)
    var startStack: Array<String> = arrayOf()

    constructor(states: Array<Int>, alphabet: Array<String>, transitionFunction: TransitionFunction,
        startState: Int, acceptStates: Array<Int>, startStack: Array<String>) {
        this.states = states
        this.alphabet = alphabet
        this.transitionFunction = transitionFunction
        this.startState = startState
        this.acceptStates = acceptStates
        this.startStack = startStack
    }

    fun recognize(str: String): RecognitionResult {
        val alternatives = Stack<Configuration>()
        val fails = LinkedList<String>()
        val startStackCopy = mutableListOf<String>()
        startStack.forEach { startStackCopy.add(it) }
        var currConf = Configuration(null, startState, str, 0, startStackCopy)
        var hasNextState = false
        var shift = 0

        do {
            var currState = currConf.state
            var currentStack = currConf.stack
            var currentStackTop = mutableListOf<String>()
            currConf.stack.firstOrNull()?.let{ currentStackTop.add(it) }
            var i = currConf.charIndex
            var currSymbol = str.getOrNull(i)?.toString() ?: ""

            while (i < str.length || !acceptStates.contains(currState)) {
                if (currSymbol.isNotEmpty() && !alphabet.contains(currSymbol)) {
                    return RecognitionResult(
                        false,
                        "{${currConf.reconstructHistory()}\nIllegal symbol '${currSymbol}'."
                    )
                }
                if (!states.contains(currState)) {
                    return RecognitionResult(
                        false,
                        "{${currConf.reconstructHistory()}\nIllegal state '${currState}'.")
                }
                Log.d("Automate","Current chain ${str} index ${i} state ${currState} stack ${currentStackTop.joinToString()}")
                // detect next possible states
                val nextPossibleStates = LinkedList<DestinationRule>()
                val transitWithShift = transitionFunction.transit(TransitionRule(currState!!, currSymbol, currentStackTop))
                if (transitWithShift != null) {
                    //read one symbol
                    nextPossibleStates.add(transitWithShift)
                    shift = currSymbol.length
                    Log.d("Automate","Transition (${transitWithShift.state},${transitWithShift.stack.joinToString()}) ")
                } else {
                    //empty cycle iteration
                    val transitWithoutShift = transitionFunction.transit(TransitionRule(currState, "", currentStackTop))
                    shift = 0
                    transitWithoutShift?.let {
                        nextPossibleStates.add(it)
                        Log.d("Automate","Transition (${transitWithoutShift.state},${transitWithoutShift.stack.joinToString()}) ")
                    }
                }
                if (nextPossibleStates.isEmpty()) {
                    //no transition rule found
                    fails.add("${currConf.reconstructHistory()}: No transition for (${currState}, ${currSymbol}, ${currentStackTop}).")
                    break
                }
                //get first possible transition
                val nextState = nextPossibleStates.first

                //save all other possible transitions (alternatives)
                val alternativeStates = LinkedList<DestinationRule>().apply{
                    addAll(nextPossibleStates)
                    removeFirst()
                }
                alternativeStates.forEach { altState ->
                    val altStack = LinkedList<String>() .apply{
                        currentStack.forEach{ add(it) }
                        removeFirst()
                        altState.stack.forEach {
                            addFirst(it)
                        }
                    }
                    alternatives.add(Configuration(currConf, altState.state, str, i + shift, altStack))
                }

                //create new stack
                val newStack = LinkedList<String>().apply{
                    currentStack.forEach{
                        addLast(it)
                    }
                    if (isNotEmpty()) {
                        removeFirst()
                    }
                    nextState.stack.forEach {
                        addLast(it)
                    }
                }
                //prepare to next transition
                i += shift
                shift = 0
                currState = nextState.state
                currSymbol = str.getOrNull(i)?.toString() ?: ""
                currentStack = newStack
                currentStackTop = mutableListOf()
                currentStack.firstOrNull()?.let{ currentStackTop.add(it) }
                currConf = Configuration(currConf, currState, str, i, newStack)
            }

            if (fails.isNotEmpty()) break
            if (i == str.length) {
                if (acceptStates.contains(currState) && currConf.stack.isEmpty()) {
                    //chain recognized!
                    return RecognitionResult(true, currConf.reconstructHistory())
                } else if (!acceptStates.contains(currState)){
                    //recognition stopped not in final configuration
                    fails.add("${currConf.reconstructHistory()}: Stopped not in final configuration.")
                    break
                } else if (currConf.stack.isNotEmpty()){
                    //after recognition stack wasn't empty
                    fails.add("${currConf.reconstructHistory()}: Stack is not empty!")
                    break
                }
            }
            hasNextState = alternatives.size > 0
            try {
                currConf = alternatives.pop()
            }
            catch(ex: Exception){
                fails.add("${currConf.reconstructHistory()}: Stopped not in final configuration.")
            }
        } while (hasNextState)

        var log = "None path accepted the string: "
        fails.forEach {
            log += it
        }
        return RecognitionResult(false, log)
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("States: ${states.joinToString(", ")}\n")
            .append("Start state: $startState\n")
            .append("Accept states: ${acceptStates.joinToString()}\n")
            .append("Alphabet: ${alphabet.joinToString("")}\n")
            .append("Transitions: \n$transitionFunction\n")
        return stringBuilder.toString()
    }

    inner class Configuration {
        var previous: Configuration? = null
        var state: Int = 0
        var remainingString = ""
        var charIndex: Int = 0
        var stack: List<String> = LinkedList<String>()

        constructor(previous: Configuration?, state: Int, str: String, charIndex: Int, stack: List<String>) {
            this.previous = previous
            this.state = state
            this.charIndex = charIndex
            this.stack = stack
            remainingString = if (charIndex < str.length) str.substring(charIndex) else "λ"
        }

        fun reconstructHistory(): String {
            val stack = LinkedList<Configuration>()
            var c: Configuration? = this
            while (c != null) {
                stack.addFirst(c)
                c = c.previous
            }
            val result = StringBuilder()
            stack.forEachIndexed{ index: Int, value: Configuration ->
                result.append(value)
                if (index+1 < stack.size) {
                    result.append(" |- ")
                }
            }
            return result.toString()
        }

        override fun toString(): String {
            return "(${state}, ${remainingString}, ${stack.joinToString("")})"
        }
    }
}