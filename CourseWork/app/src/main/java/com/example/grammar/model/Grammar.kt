package com.example.grammar.model

import android.util.Log
import androidx.room.*
import com.example.grammar.utils.TreeVertex
import java.util.*

@Entity (tableName = "grammar")
class Grammar{
    @Ignore
    private val LOG_TAG = "Grammar"

    @PrimaryKey(autoGenerate = true)
    var id = 0
    @Ignore
    var terminals = listOf<Symbol>()
    @Ignore
    var nonTerminals = listOf<Symbol>()
    @TypeConverters(RulesConverter::class)
    var rules = LinkedList<GrammarRule>()
    @Ignore
    var startSymbol = Symbol()

    class RulesConverter{
        @TypeConverter
        fun fromRules(fromRules: LinkedList<GrammarRule>):String{
            val stringBuilder = StringBuilder()
            for (rule in fromRules){
                rule.leftPart.forEach{
                    if (it.isTerminal) {
                        stringBuilder.append("T${it.value}\n")
                    } else {
                        stringBuilder.append("N${it.value}\n")
                    }
                }
                stringBuilder.append("${GrammarRule.delimeter}\n")
                rule.rightPart.forEach{
                    if (it.isTerminal) {
                        stringBuilder.append("T${it.value}\n")
                    } else {
                        stringBuilder.append("N${it.value}\n")
                    }
                }
                stringBuilder.append("\n")
            }
            val lastLineDivider = stringBuilder.lastIndexOf("\n")
            if (lastLineDivider >= 0) stringBuilder.removeRange(lastLineDivider, lastLineDivider)
            return  stringBuilder.toString()
        }

        @TypeConverter
        fun toRules(toRules: String): LinkedList<GrammarRule>{
            val allRules = LinkedList<GrammarRule>()
            val everyRulesString = toRules.split("\n")
            for (ruleString in everyRulesString){
                val ruleParts = ruleString.split(GrammarRule.delimeter)
                val parsedRule = GrammarRule().apply{
                    ruleParts.getOrNull(0)?.let{
                        it.split("\n").forEach {
                            if (it.isNotEmpty() && it.startsWith("T")){
                                leftPart.addLast(Symbol(it.removeRange(0,1), true))
                            } else if (it.isNotEmpty() && it.startsWith("N")){
                                leftPart.addLast(Symbol(it.removeRange(0,1), false))
                            }
                        }
                    }
                    ruleParts.getOrNull(1)?.let{
                        it.split("\n").forEach {
                            if (it.isNotEmpty() && it.startsWith("T")){
                                rightPart.addLast(Symbol(it.removeRange(0,1), true))
                            } else if (it.isNotEmpty() && it.startsWith("N")){
                                rightPart.addLast(Symbol(it.removeRange(0,1), false))
                            }
                        }
                    }
                }
                if (parsedRule.leftPart.isNotEmpty()) {
                    allRules.add(parsedRule)
                }
            }
            return allRules
        }
    }

    fun parseTerminalsFromRules(targetRules: LinkedList<GrammarRule>): List<Symbol>{
        val newTerminals = LinkedList<Symbol>()
        for (rule in targetRules){
            val symbols = rule.rightPart
            for (symbol in symbols){
                if (symbol.isTerminal && !newTerminals.contains(symbol)) {
                    newTerminals.add(symbol)
                }
            }
        }
        return newTerminals
    }

    @Ignore
    private var minLength = 0
    @Ignore
    private var maxLength = 0
    @Ignore
    private var createdChains = LinkedList<Chain>()

    fun createChains(minLength: Int, maxLength: Int): LinkedList<Chain>{
        this.minLength = minLength
        this.maxLength = maxLength
        createdChains = LinkedList<Chain>()

        var startRuleNumber = -1

        for (i in rules.indices){
            if (rules[i].leftPart.contains(startSymbol)){
                startRuleNumber = i
            }
        }
        if (startRuleNumber < 0){
            return createdChains
        }
        val newChain = Chain().apply{
            data = LinkedList<Symbol>().apply{add(startSymbol)}
            graph = TreeVertex(GraphElement().apply{data = startSymbol})
        }

//        createChain(newChain, newChain.graph!!)
        createChainWithTree(newChain)

//        Log.d(LOG_TAG, "Creating completed!")
//        for (chain in createdChains){
//            Log.d(LOG_TAG,"Created chain: ${chain.data}")
//        }
        return createdChains
    }

    private fun hasNonTerminals(treeVertex: TreeVertex<GraphElement>): Boolean{
        if (treeVertex.childs.isEmpty()){
            return nonTerminals.contains(treeVertex.data.data)
        }
        else {
            var hasNonTerminals = false
            for(child in treeVertex.childs){
                hasNonTerminals = hasNonTerminals || hasNonTerminals(child)
                if (hasNonTerminals) break
            }
            return hasNonTerminals
        }
    }

    private fun createChainWithTree(chain: Chain){
        if (chain.data.filter { it.isTerminal }.size > maxLength){

            return
        }

        if (!hasNonTerminals(chain)){
            if (chain.size in minLength..maxLength) {
//                val findedChain = createdChains.find { element->
//                    element.data == chain.data
//                }
//                if (findedChain == null) {
                    createdChains.add(chain)
//                }
            } else {
                println("Chain : ${chain}")
                println( "Not in $maxLength")
            }
            return
        }
        var rulesApplied = false
        for (rule in rules){
            val newGraph = chain.graph!!.copy()
            val currentSymbols = newGraph.getTreeTopElements()
            for (symbol in currentSymbols){
                if (rule.leftPart.contains(symbol.data.data)){
                    rulesApplied = true
                    if (rule.rightPart.isEmpty()){
                        symbol.addChild(GraphElement().apply {data = Symbol() })
                    }
                    else{
                        rule.rightPart.forEach {ruleSymbol ->
                            symbol.addChild(GraphElement().apply {data = ruleSymbol})
                        }
                    }
                    break
                }
            }
            if (rulesApplied) {
                val topElements = LinkedList<Symbol>()
                newGraph.getTreeTopElements().forEach { symbol ->
                    topElements.addLast(symbol.data.data)
                }
                val newChain = Chain().apply {
                    data = topElements
                    graph = newGraph
                }
                createChainWithTree(newChain)
            }
            rulesApplied = false
        }
    }

    private fun String.insert(substring: String, fromPosition: Int):String{
        var builder = StringBuilder()
        if (fromPosition > 0){
            builder.append(this.substring(0, fromPosition-1))
        }
        builder.append(substring)
        if (fromPosition < this.length) {
            builder.append(this.substring(fromPosition))
        }
        return builder.toString()
    }

    private fun String.remove(atPosition: Int): String{
        var builder = StringBuilder()
        if (atPosition > 0) {
            builder.append(this.substring(0, atPosition-1))
        }
        if (atPosition < this.length-1) {
            builder.append(this.substring(atPosition + 1))
        }
        return builder.toString()
    }

    private fun hasNonTerminals(chain: Chain): Boolean{
        return chain.data.indexOfFirst { !it.isTerminal } != -1
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Alphabet: \nTerminals: ${terminals}\nNonTerminals: ${nonTerminals}\n")
        stringBuilder.append("Start non-terminal: ${startSymbol}\n")
        stringBuilder.append("Rules: \n${rules.joinToString("\n")}\n")
        return stringBuilder.toString()
    }
}

fun <T> LinkedList<T>.replaceFirst(replacedSymbol:List<T>, newSymbols: List<T>): LinkedList<T>{
    val newList = LinkedList<T>()
    this.forEach{
        newList.add(it)
    }
    if (replacedSymbol.isEmpty()) return newList
    var replacePosition = indexOfFirst(replacedSymbol)

    if (replacePosition >= 0) {
        newList.removeRange(replacePosition, replacePosition + replacedSymbol.size - 1)
        newSymbols.forEach {
            newList.add(replacePosition, it)
            replacePosition++
        }
    }
    return newList
}

fun <T> List<T>.indexOfFirst(list: List<T>): Int{
    var indexOfFirst = -1
    for (i in this.indices){
        var matches = true
        var idx = i
        for (symbol in list) {
            if (symbol != this.getOrNull(idx)){
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

fun <T> LinkedList<T>.removeRange(startIndex: Int, endIndex: Int){
    for(i in startIndex..endIndex) {
        if (startIndex in 0 until this.size) {
            this.removeAt(startIndex)
        }
    }
}

fun main(){
    var list = LinkedList<String>()
    list.addLast("1")
    list.addLast("2")
    list.addLast("3")
    list.addLast("4")
    list.addLast("5")
    println("Index of first: "+list.indexOfFirst(listOf("3", "4")))
    println("Index of first: "+list.indexOfFirst(listOf("2", "4")))
    println("Replace first: "+list.replaceFirst(listOf("2"), listOf("3", "4")))
}