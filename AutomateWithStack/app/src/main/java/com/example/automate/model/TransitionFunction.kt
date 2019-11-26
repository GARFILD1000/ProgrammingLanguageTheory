package com.example.automate.model

import android.util.Log
import java.util.*

class TransitionFunction {
    companion object {
        fun parse(string: String): TransitionFunction {
            Log.d("Transition function", "Parse ${string}")
            val newFunction = TransitionFunction()
            val transitions = string.split("\n")
            transitions.forEach {
                val transitionParts = it.split("->")
                if (transitionParts.size == 2) {
                    val transitionRuleParts = transitionParts[0]
                        .replace("(", "")
                        .replace(")", "")
                        .split(",")
                    val transitionRule =
                        TransitionRule(transitionRuleParts[0], transitionRuleParts[1], transitionRuleParts[2].toCharArray().map { it.toString() })
                    val destinationRuleParts = transitionParts[1]
                        .replace("(", "")
                        .replace(")", "")
                        .split(",")
                    val destinationRule = DestinationRule(destinationRuleParts[0], destinationRuleParts[1].toCharArray().map { it.toString() })
                    newFunction.add(transitionRule, destinationRule)
                }
            }
            return newFunction
        }
    }

    var table: LinkedList<Pair<TransitionRule, DestinationRule>> = LinkedList()

    fun transit(transitionRule: TransitionRule): DestinationRule? {
        val result = table.find {
            it.first.state == transitionRule.state &&
            it.first.symbol == transitionRule.symbol &&
            it.first.stack == transitionRule.stack
        }
        return result?.second
    }

    fun add(rule: TransitionRule, result: DestinationRule) {
        table.add(Pair(rule, result))
    }

    fun clear() {
        table.clear()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        table.forEach { transition ->
            builder.append("(${transition.first.state},${transition.first.symbol},")
            transition.first.stack.forEach{
                builder.append("$it")
            }
            builder.append(")->(${transition.second.state},")
            transition.second.stack.forEach{
                builder.append("$it")
            }
            builder.append(")\n")
        }
        Log.d("Transition function", "To String ${builder}")
        return builder.toString()
    }
}