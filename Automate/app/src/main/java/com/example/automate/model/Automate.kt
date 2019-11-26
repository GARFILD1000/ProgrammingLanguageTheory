package com.example.automate.model

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
        var currConf = Configuration(null, startState, str, 0)
        var hasNextState = false

        do {
            var currState = currConf.state
            var i = currConf.charIndex
            while (i < str.length) {
                val currSymbol = str[i].toString()
                if (!alphabet.contains(currSymbol)) {
                    return RecognitionResult(
                        false,
                        "{${currConf.reconstructHistory()}\nIllegal symbol '${currSymbol}'."
                    )
                }
                //currState?: continue
                val nextPossibleStates = transitionFunction.transit(TransitionRule(currState!!, currSymbol))

                if (nextPossibleStates.size == 0) {
                    fails.add("${currConf.reconstructHistory()}: No transition for (${currState}, ${currSymbol}).")
                    break
                }

                val nextState = nextPossibleStates[0]
                val alternativeStates = LinkedList<String>().apply{
                    addAll(nextPossibleStates)
                    removeFirst()
                }
                alternativeStates.forEach { altState ->
                    alternatives.add(Configuration(currConf, altState, str, i + 1))
                }
                currState = nextState
                currConf = Configuration(currConf, currState, str, i + 1)
                i++
            }
            if (i == str.length) {
                if (acceptStates.contains(currState)) {
                    return RecognitionResult(true, currConf.reconstructHistory())
                } else {
                    fails.add("${currConf.reconstructHistory()}: Stopped not in final configuration.")
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

        constructor(previous: Configuration?, state: String, str: String, charIndex: Int) {
            this.previous = previous
            this.state = state
            this.charIndex = charIndex
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
            return "(${state}, ${remainingString})"
        }
    }
}