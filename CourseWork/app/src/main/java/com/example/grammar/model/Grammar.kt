package com.example.grammar.model

import android.util.Log
import androidx.room.*
import com.example.grammar.utils.TreeVertex
import java.util.*

class Grammar {
    @Ignore
    private val LOG_TAG = "Grammar"

    var terminals = listOf<Symbol>()
    var nonTerminals = listOf<Symbol>()
    var rules = LinkedList<GrammarRule>()
    var startSymbol = Symbol()
    private var minLength = 0
    private var maxLength = 0
    private var createdChains = LinkedList<Chain>()

    fun createChains(minLength: Int, maxLength: Int): LinkedList<Chain> {
        this.minLength = minLength
        this.maxLength = maxLength
        createdChains = LinkedList<Chain>()

        var startRuleNumber = -1

        for (i in rules.indices) {
            if (rules[i].leftPart.contains(startSymbol)) {
                startRuleNumber = i
                break
            }
        }
        if (startRuleNumber < 0) {
            return createdChains
        }
        val newChain = Chain().apply {
            symbols = LinkedList<Symbol>().apply { add(startSymbol) }
        }
        createChain(newChain)
        return createdChains
    }

    private fun createChain(chain: Chain, checkUnique: Boolean = false) {
        if (chain.terminalsCount() > maxLength) {
            return
        }
        if (!hasNonTerminals(chain)) {
            if (chain.terminalsCount() in minLength..maxLength) {
                if (checkUnique) {
                    val findedChain = createdChains.find { element ->
                        element.symbols == chain.symbols
                    }
                    if (findedChain == null) {
                        createdChains.add(chain)
                    }
                } else {
                    createdChains.add(chain)
                }
            }
            return
        }

        for (rule in rules) {
            for (symbol in chain.symbols) {
                if (rule.leftPart.contains(symbol)) {
                    val newChain = chain.copy()
                    newChain.rules.addLast(rule)
                    newChain.symbols.replaceFirst(rule.leftPart, rule.rightPart)
                    createChain(newChain)
                    break
                }
            }
        }
    }

    fun createTree(chain: Chain): TreeVertex<GraphElement>{
        val graph = TreeVertex(GraphElement().apply { data = startSymbol })
        for (rule in chain.rules) {
            val currentSymbols = graph.getTreeTopElements()
            for (symbol in currentSymbols) {
                if (rule.leftPart.contains(symbol.data.data)) {
                    if (rule.rightPart.isEmpty() || rule.rightPart.contains(Symbol.EMPTY)) {
                        symbol.addChild(GraphElement(Symbol.EMPTY))
                    } else {
                        rule.rightPart.forEach { ruleSymbol ->
                            symbol.addChild(GraphElement().apply { data = ruleSymbol })
                        }
                    }
                    break
                }
            }
        }
        return graph
    }

    private fun hasNonTerminals(chain: Chain): Boolean {
        return chain.symbols.indexOfFirst { !it.isTerminal } != -1
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Alphabet: \nTerminals: ${terminals}\nNonTerminals: ${nonTerminals}\n")
        stringBuilder.append("Start non-terminal: ${startSymbol}\n")
        stringBuilder.append("Rules: \n${rules.joinToString("\n")}\n")
        return stringBuilder.toString()
    }
}

fun <T> LinkedList<T>.replaceFirst(replacedSymbol: List<T>, newSymbols: List<T>) {
    if (replacedSymbol.isEmpty()) return
    var replacePosition = indexOfFirst(replacedSymbol)

    if (replacePosition >= 0) {
        this.removeRange(replacePosition, replacePosition + replacedSymbol.size - 1)
        newSymbols.forEach {
            this.add(replacePosition, it)
            replacePosition++
        }
    }
}

fun <T> List<T>.indexOfFirst(list: List<T>): Int {
    var indexOfFirst = -1
    for (i in this.indices) {
        var matches = true
        var idx = i
        for (symbol in list) {
            if (symbol != this.getOrNull(idx)) {
                matches = false
                break
            }
            idx++
        }
        if (matches) {
            indexOfFirst = i
            break
        }
    }
    return indexOfFirst
}

fun <T> LinkedList<T>.removeRange(startIndex: Int, endIndex: Int) {
    for (i in startIndex..endIndex) {
        if (startIndex in 0 until this.size) {
            this.removeAt(startIndex)
        }
    }
}

fun main() {
    var list = LinkedList<String>()
    list.addLast("0")
    list.replaceFirst(listOf("0"), listOf("a", "1"))
    println("Replace first: " + list)
}