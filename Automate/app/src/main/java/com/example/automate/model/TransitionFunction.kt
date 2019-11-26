package com.example.automate.model

import java.util.*

class TransitionFunction {
    companion object {
        fun parse(string: String): TransitionFunction{
            val newFunction = TransitionFunction()
            val transitions = string.split("\n")
            transitions.forEach {
                val transitionParts = it.split("->")
                if (transitionParts.size == 2) {
                    val transitionRuleParts = transitionParts[0]
                        .replace("(", "")
                        .replace(")", "")
                        .split(",")
                    val transitionRule = TransitionRule(transitionRuleParts[0], transitionRuleParts[1])
                    val destination = transitionParts[1]
                        .replace("(","")
                        .replace(")","")
                    newFunction.add(transitionRule, arrayOf(destination))
                }
            }
            return newFunction
        }
    }

    var table: HashMap<TransitionRule, Array<String>> = HashMap()

    fun transit(transitionRule: TransitionRule): Array<String> {
        val result = table.get(transitionRule)
        return result ?: arrayOf()
    }

    fun add(rule: TransitionRule, result: Array<String>) {
        table.put(rule, result)
    }

    fun clear() {
        table.clear()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        table.forEach { transition ->
            transition.value.forEach {
                builder.append("(${transition.key.state},${transition.key.symbol})->(")
                builder.append("$it)\n")
            }
        }
        return builder.toString()
    }
}