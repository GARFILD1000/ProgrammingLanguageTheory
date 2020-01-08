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

    fun reverseBlocks(){
        blocks.reverse()
        for (i in blocks.indices){
            blocks[i].chain = blocks[i].chain.reversed()
        }
    }

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
        if (leftLinearOutput){
            reverseBlocks()
        }
        var minSize = 0
        for (block in blocks) {
            if (!block.isFlexible) {
                minSize += block.chain.length
            }
        }
        val minFlexibleSize = minSize % multiplicity

        for (block in blocks) {
            if (block.isFlexible) {
                generateFlexibleRules(block, minFlexibleSize)
            } else {
                generateNonFlexibleRules(block)
            }
        }
        val newGrammar = Grammar()
        newGrammar.rules = rules
        newGrammar.nonTerminals = nonTerminals
        newGrammar.startSymbol = startNonTerminal
        newGrammar.terminals = terminals
        return newGrammar
    }

    private fun hasNonTerminals(symbols: LinkedList<Symbol>): Boolean {
        val indexOfFirst = symbols.indexOfFirst { symbol -> !symbol.isTerminal }
        return indexOfFirst != -1
    }

    private fun generateNonFlexibleRules(block: Block) {
        for (symbol in block.chain) {
            if (rules.last.rightPart.isNotEmpty()){
                //add terminal to empty rule and non-terminal of next rule's left part
                nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
                rules.last.addToRight(nonTerminals.last, leftLinearOutput)
                //create new empty rule
                val newRule = GrammarRule()
                newRule.addToLeft(nonTerminals.last)
                rules.addLast(newRule)
            }
            rules.last.addToRight(Symbol(symbol.toString()), leftLinearOutput)
        }
        if (rules.last.rightPart.isNotEmpty()) {
            //add empty rule
            nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
            rules.last.addToRight(nonTerminals.last, leftLinearOutput)
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
                //create rule for every possible terminal symbol
                val newRule = GrammarRule()
                newRule.addToLeft(nonTerminals.last)
                newRule.addToRight(terminal, leftLinearOutput)
                newRules.add(newRule)
            }
            nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
            val lastLeftPart = newRules.last.leftPart
            newRules.forEach { newRule ->
                //make transition to next (empty) rule
                if (newRule.leftPart == lastLeftPart) {
                    newRule.addToRight(nonTerminals.last, leftLinearOutput)
                }
            }
            if (i == multiplicity - 1) {
                //make transition to first new rule
                nonTerminals.removeLast()
                val firstAddedRule = newRules.first
                for (terminal in terminals) {
                    val newRule = GrammarRule()
                    newRule.addToLeft(nonTerminals.last)
                    newRule.addToRight(terminal, leftLinearOutput)
                    newRule.addToRight(firstAddedRule.leftPart.first, leftLinearOutput)
                    newRules.add(newRule)
                }
            }
        }

        val startRule =
            Symbol((nonTerminals.size - (multiplicity - minFlexibleSize) ).toString(), false)
        val lastLeftPart = rules.lastOrNull()?.leftPart
        if (rules.isNotEmpty()) {
            //make transition from previous block to needed position in this block (there we can set minimum length of this block)
            rules.forEach {
                if (it.leftPart == lastLeftPart) {
                    it.removeFromRight(leftLinearOutput)
                    it.addToRight(startRule, leftLinearOutput)
                }
            }
        }
        //create non-terminal for empty rule
        nonTerminals.addLast(Symbol(nonTerminals.size.toString(), false))
        //if flexible size may be 0, make transition to empty rule from previous block (to skip rules)
        if (minFlexibleSize == 0) {
            rules.forEach {
                if (it.leftPart == lastLeftPart) {
                    val newRule = GrammarRule()
                    lastLeftPart.forEach { symbol ->
                        newRule.addToLeft(symbol)
                    }
                    it.rightPart.firstOrNull { it.isTerminal }?.let {terminal ->
                        newRule.addToRight(terminal, leftLinearOutput)
                    }
                    newRule.addToRight(nonTerminals.last, leftLinearOutput)
                    newRules.add(newRule)
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
    var startBlock = RegularRulesGenerator.Block("ab", false)
    var endBlock = RegularRulesGenerator.Block("bc", false)
    var flexibleBlock = RegularRulesGenerator.Block("", true)
    var generator = RegularRulesGenerator(terminals, true, multiplicy)
    generator.addLastBlock(startBlock)
    generator.addLastBlock(flexibleBlock)
    generator.addLastBlock(endBlock)

    val grammar = generator.generateGrammar()
    println(grammar.toString())

    grammar.createChains(0, 9).forEach {
        println("Chain: $it")
    }
}