package com.example.grammar.model

import com.example.grammar.utils.TreeVertex
import java.util.*

class Chain{
    var symbols = LinkedList<Symbol>()
    var size = 0
    private set
    get() = symbols.size
    var rules = LinkedList<GrammarRule>()

    override fun toString(): String {
        return symbols.joinToString("")
    }

    fun terminalsCount(): Int{
        var counter = 0
        for (symbol in symbols) {
            if (symbol.isTerminal) {
                counter += symbol.value.length
            }
        }
        return counter
    }

    fun copy(): Chain{
        val newChain = Chain()
        val newRules = LinkedList<GrammarRule>()
        rules.forEach {
            newRules.add(it)
        }
        val newSymbols = LinkedList<Symbol>()
        symbols.forEach {
            newSymbols.add(it)
        }
        newChain.symbols = newSymbols
        newChain.rules = newRules
        return newChain
    }
}