package com.example.grammar.util

import com.example.grammar.model.Grammar
import com.example.grammar.model.GrammarRule
import com.example.grammar.model.Symbol
import java.util.*

class RegularRulesGenerator(
    val terminals: List<Symbol>,
    val leftLinearOutput: Boolean,
    val multiplicity: Int
) {
    private var hasFlexibleBlock = false
    private var blocks = LinkedList<Block>()

    fun addLastBlock(block: Block): Boolean {
        return if (block.isFlexible) {
            if (hasFlexibleBlock) {
                false
            } else {
                hasFlexibleBlock = true
                blocks.addLast(block)
                true
            }
        } else {
            blocks.addLast(block)
            true
        }
    }

    fun addFirstBlock(block: Block): Boolean {
        return if (block.isFlexible) {
            if (hasFlexibleBlock) {
                false
            } else {
                hasFlexibleBlock = true
                blocks.addFirst(block)
                true
            }
        } else {
            blocks.addFirst(block)
            true
        }
    }

    private var nonTerminals = LinkedList<Symbol>()
    private var rules = LinkedList<GrammarRule>()
    private var startNonTerminal = Symbol("0", false)

    fun generateGrammar(): Grammar {
        nonTerminals.clear()
        rules.clear()
        nonTerminals.add(startNonTerminal)
        //add empty rule
        rules.add(
            GrammarRule(
                LinkedList<Symbol>().apply { addFirst(nonTerminals.last) },
                LinkedList<Symbol>()
            )
        )

        var minSize = 0
        for (block in blocks) {
            if (!block.isFlexible) {
                minSize += block.chain.length
            }
        }

        val minFlexibleSize = minSize % multiplicity
        println("Min flexible size = $minFlexibleSize")
        for (block in blocks) {
            println("Block: $block")
            if (block.isFlexible) {
                generateFlexibleRules(block, minFlexibleSize)
            } else {
                generateNonFlexibleRules(block)
            }
        }

        return Grammar().apply {
            this.rules = this@RegularRulesGenerator.rules
            this.terminals = this@RegularRulesGenerator.terminals
            this.nonTerminals = this@RegularRulesGenerator.nonTerminals
            this.startSymbol = this@RegularRulesGenerator.startNonTerminal
        }
    }

    private fun hasNonTerminals(symbols: LinkedList<Symbol>): Boolean {
        val indexOfFirst = symbols.indexOfFirst { symbol -> !symbol.isTerminal }
        return indexOfFirst != -1
    }

    private fun generateNonFlexibleRules(block: Block) {
        for (symbol in block.chain) {
            if (rules.last.rightPart.isNotEmpty()){
                nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
                rules.last.rightPart.addLast(nonTerminals.last)
                val newRule = GrammarRule()
                newRule.leftPart.add(nonTerminals.last)
                rules.addLast(newRule)
            }
            rules.last.rightPart.addLast(Symbol(symbol.toString(), true))
        }
        if (rules.last.rightPart.isNotEmpty()) {
            //add empty rule
            nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
            rules.last.rightPart.addLast(nonTerminals.last)
            rules.add(
                GrammarRule(
                    LinkedList<Symbol>().apply { addFirst(nonTerminals.last) },
                    LinkedList<Symbol>()
                )
            )
        }
    }

    private fun generateFlexibleRules(block: Block, minFlexibleSize: Int) {
        val newRules = LinkedList<GrammarRule>()
        rules.removeLast()
        for (i in 0 until multiplicity) {
            for (terminal in terminals) {
                val leftPart = LinkedList<Symbol>()
                val rightPart = LinkedList<Symbol>()
                leftPart.add(nonTerminals.last)
                rightPart.addLast(terminal)
                newRules.add(GrammarRule(leftPart, rightPart))
            }
            nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
            val lastLeftPart = newRules.last.leftPart
            newRules.forEach {
                if (it.leftPart == lastLeftPart) {
                    it.rightPart.addLast(nonTerminals.last)
                }
            }
            if (i == multiplicity - 1) {
                nonTerminals.removeLast()
                val firstAddedRule = newRules.first
                for (terminal in terminals) {
                    val leftPart = LinkedList<Symbol>()
                    val rightPart = LinkedList<Symbol>()
                    leftPart.add(nonTerminals.last)
                    rightPart.addLast(terminal)
                    rightPart.addLast(firstAddedRule.leftPart.first)
                    newRules.add(GrammarRule(leftPart, rightPart))
                }
            }
        }

        val startRule =
            Symbol((nonTerminals.size - (multiplicity - minFlexibleSize) ).toString(), false)
        val lastLeftPart = rules.getOrNull(rules.size-1)?.leftPart
        if (rules.isNotEmpty()) {
            rules.forEach {
                if (it.leftPart == lastLeftPart) {
                    it.rightPart.removeLast()
                    it.rightPart.addLast(startRule)
                }
            }
        }
        //create non-terminal for empty rule
        nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
        //if flexible size may be 0, make transition to empty rule from previous block
        if (minFlexibleSize == 0) {
            rules.forEach {
                if (it.leftPart == lastLeftPart) {
                    val rightPart = LinkedList<Symbol>()
                    it.rightPart.firstOrNull { it.isTerminal }?.let {
                        rightPart.addLast(it)
                    }
                    rightPart.addLast(nonTerminals.last)
                    newRules.add(GrammarRule(lastLeftPart, rightPart))
                }
            }
        }
        //save all created rules
        newRules.forEach {
            rules.addLast(it)
        }
        //add empty rule
        rules.add(
            GrammarRule(
                LinkedList<Symbol>().apply { addFirst(nonTerminals.last) },
                LinkedList<Symbol>()
            )
        )
    }

    data class Block(
        var chain: String = "",
        var isFlexible: Boolean = false
    )
}

fun main() {
    var terminals = listOf(Symbol("a"), Symbol("b"), Symbol("c"))
    var multiplicy = 2
    var startBlock = RegularRulesGenerator.Block("abc", false)
    var endBlock = RegularRulesGenerator.Block("bca", false)
    var flexibleBlock = RegularRulesGenerator.Block("", true)
    var generator = RegularRulesGenerator(terminals, true, multiplicy)
    generator.addLastBlock(startBlock)
    generator.addLastBlock(flexibleBlock)
    generator.addLastBlock(endBlock)

    val grammar = generator.generateGrammar()
    println(grammar.toString())

    grammar.createChains(0, 12).forEach {
        println("Chain: $it")
    }
}