package com.example.automate.model

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.automate.util.StringArrayConverter
import com.example.automate.util.TransitionFunctionConverter
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

data class RecognitionResult(var isRecognized: Boolean, var description: String)

@Entity(tableName = "automate")
class Automate {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @TypeConverters(StringArrayConverter::class)
    var states: Array<String> = arrayOf<String>()
    @TypeConverters(StringArrayConverter::class)
    var alphabet: Array<String> = arrayOf<String>()
    @TypeConverters(TransitionFunctionConverter::class)
    var transitionFunction: TransitionFunction
    var startState: String = ""
    @TypeConverters(StringArrayConverter::class)
    var acceptStates: Array<String> = arrayOf<String>()


    constructor(states: Array<String>, alphabet: Array<String>, transitionFunction: TransitionFunction,
        startState: String, acceptStates: Array<String>) {
        this.states = states
        this.alphabet = alphabet
        this.transitionFunction = transitionFunction
        this.startState = startState
        this.acceptStates = acceptStates
    }

    fun recognize(str: String): RecognitionResult {
        val alternatives = Stack<Configuration>()
        val fails = LinkedList<String>()
        var currConf = Configuration(null, startState, str, 0, Stack())
        var hasNextState = false
        var shift = 0

        do {
            var currState = currConf.state
            var currentStack = currConf.stack
            var currentStackTop = mutableListOf<String>()
            currConf.stack.firstOrNull()?.let{ currentStackTop.add(it) }
            var i = currConf.charIndex
            var currSymbol = str.getOrNull(i)?.toString() ?: ""

            //(transitionFunction.transit(TransitionRule(currState!!, currSymbol, currentStackTop)) == null) {

            while (i < str.length || !acceptStates.contains(currState)) {
                if (!alphabet.contains(currSymbol) && currSymbol.isNotEmpty()) {
                    return RecognitionResult(
                        false,
                        "{${currConf.reconstructHistory()}\nIllegal symbol '${currSymbol}'."
                    )
                }
                Log.d("Automate","Current chain ${str} index ${i} state ${currState} stack ${currentStackTop.joinToString()}")
                //currState?: continue
                val nextPossibleStates = LinkedList<DestinationRule>()
                val transitWithShift = transitionFunction.transit(TransitionRule(currState!!, currSymbol, currentStackTop))
                if (transitWithShift != null){
                    nextPossibleStates.add(transitWithShift)
                    shift = currSymbol.length
                    Log.d("Automate","Transition (${transitWithShift.state},${transitWithShift.stack.joinToString()}) ")
                } else {
                    val transitWithoutShift = transitionFunction.transit(TransitionRule(currState, "", currentStackTop))
                    shift = 0
                    transitWithoutShift?.let {
                        nextPossibleStates.add(it)
                        Log.d("Automate","Transition (${transitWithoutShift.state},${transitWithoutShift.stack.joinToString()}) ")
                    }
                }

                if (nextPossibleStates.isEmpty()) {
                    fails.add("${currConf.reconstructHistory()}: No transition for (${currState}, ${currSymbol}).")
                    break
                }
                val nextState = nextPossibleStates.first
                val alternativeStates = LinkedList<DestinationRule>().apply{
                    addAll(nextPossibleStates)
                    removeFirst()
                }
                val newStack = LinkedList<String>() .apply{
                    currentStack.forEach{ add(it) }
                    if (isNotEmpty()) {
                        removeFirst()
                    }
                    nextState.stack.forEach {
                        addFirst(it)
                    }
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
                currState = nextState.state
                currConf = Configuration(currConf, currState, str, i + shift, newStack)
                currentStack = newStack
                currentStackTop = mutableListOf<String>()
                currentStack.firstOrNull()?.let{ currentStackTop.add(it) }
                i += shift
                shift = 0
                currSymbol = str.getOrNull(i)?.toString() ?: ""
            }
            if (fails.isNotEmpty()) break
            if (i == str.length) {
                if (acceptStates.contains(currState)) {
                    return RecognitionResult(true, currConf.reconstructHistory())
                } else {
                    fails.add("${currConf.reconstructHistory()}: Stopped not in final configuration.")
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

    inner class Configuration {
        var previous: Configuration? = null
        var state: String? = null
        var remainingString = ""
        var charIndex: Int = 0
        var stack: List<String> = LinkedList<String>()

        constructor(previous: Configuration?, state: String, str: String, charIndex: Int, stack: List<String>) {
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