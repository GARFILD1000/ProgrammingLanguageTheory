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
    var terminals = ""
    var nonTerminals = ""
    @TypeConverters(RulesConverter::class)
    var rules = LinkedList<GrammarRule>()
    var startSymbol: Char? = null

    class RulesConverter{
        @TypeConverter
        fun fromRules(fromRules: LinkedList<GrammarRule>):String{
            val stringBuilder = StringBuilder()
            for (rule in fromRules){
                stringBuilder.append(rule.leftPart)
                stringBuilder.append(GrammarRule.delimeter)
                stringBuilder.append(rule.rightPart)
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
                    leftPart = ruleParts.getOrNull(0)?:""
                    rightPart = ruleParts.getOrNull(1)?:""
                }
                if (parsedRule.leftPart.isNotEmpty()) {
                    allRules.add(parsedRule)
                }
            }
            return allRules
        }

    }

    fun parseTerminalsFromRules(targetRules: LinkedList<GrammarRule>): String{
        val newTerminals = LinkedList<Char>()
        val stringBuilder = StringBuilder()
        for (rule in targetRules){
            val chars = rule.rightPart.toCharArray()
            for (char in chars){
                if (!nonTerminals.contains(char)) {
                    stringBuilder.append(char)
                }
            }
        }
        return stringBuilder.toString()
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
        startSymbol?:return createdChains

        var startRuleNumber = -1

        for (i in rules.indices){
            if (rules[i].leftPart.contains(startSymbol!!)){
                startRuleNumber = i
            }
        }
        if (startRuleNumber < 0){
            return createdChains
        }
        var newChain = Chain().apply{
            data="$startSymbol"
            graph = TreeVertex(GraphElement().apply{data = "$startSymbol"})
        }

//        createChain(newChain, newChain.graph!!)
        createChainWithTree(newChain)

        Log.d(LOG_TAG, "Creating completed!")
        for (chain in createdChains){
            Log.d(LOG_TAG,"Created chain: ${chain.data}")
        }
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

    private fun buildTree(chain: Chain){
        for(rule in rules){

        }
    }

    private fun createChain(chain: Chain, currentRoot: TreeVertex<GraphElement>){
        Log.d(LOG_TAG,"chain: $chain")
        if (chain.data.length > maxLength*3){
            Log.d(LOG_TAG,"----------end recursion------\n")
            return
        }
        Log.d(LOG_TAG,"$chain")

        if (!hasNonTerminals(chain.data)){
            if (chain.data.length >= minLength && chain.data.length <= maxLength) {
                Log.d(LOG_TAG,"Check has chain: ${chain.data}")
                //if (!createdChains.contains(chain)) {
                    Log.d(LOG_TAG,"Add chain: ${chain.data}")
                    createdChains.add(chain)
                //}
            }
            return
        }

        for (rule in rules){
            if (chain.data.contains(rule.leftPart)){
                val newChain = chain.data.replaceFirst(rule.leftPart,rule.rightPart)
                val newRoot = currentRoot.copy()
                val childChain = Chain().apply{
                    data = newChain
                    graph = newRoot
                }
                //val childRoot = 0
                rule.rightPart.toCharArray().forEach {
                    newRoot.addChild(GraphElement().apply{data = it.toString()})
                }
                val childRoot = newRoot.addChild(GraphElement().apply{
                    data = childChain.data
                })

                createChain(childChain, childRoot)
            }
        }
        return
    }

    private fun createChainWithTree(chain: Chain){

        Log.d(LOG_TAG,"chain: ${chain.data}")
        if (filterTerminal(chain.data).length > maxLength){
            return
        }

        if (!hasNonTerminals(chain.data)){
            if (chain.data.length >= minLength && chain.data.length <= maxLength) {
                val findedChain = createdChains.find { element->
                    element.data == chain.data
                }
                if (findedChain == null) {
                    Log.d(LOG_TAG,"Add chain: ${chain.data}")
                    createdChains.add(chain)
                }
            }
            return
        }
        var rulesApplied = false
        for (rule in rules){
            val newGraph = chain.graph!!.copy()
            val currentSymbols = newGraph.getTreeTopElements()
            Log.d(LOG_TAG, "Top tree elements ${currentSymbols.toString()}")
            for (symbol in currentSymbols){
                Log.d(LOG_TAG, "Compare symbol ${symbol} and rule ${rule.leftPart}")
                if (symbol.data.data == rule.leftPart){
                    rulesApplied = true
                    Log.d(LOG_TAG, "Use rule ${rule} for ${chain.data}")
                    if (rule.rightPart.isEmpty()){
                        symbol.addChild(GraphElement().apply {data = ""})
                    }
                    else{
                        rule.rightPart.toCharArray().forEach {ruleSymbol ->
                            symbol.addChild(GraphElement().apply {data = ruleSymbol.toString()})
                        }
                    }
                    break
                }
            }
            //currentSymbols.forEach{ Log.d(LOG_TAG,"Current symbol: $it")}
            if (rulesApplied) {
                val stringBuilder = StringBuilder()
                newGraph.getTreeTopElements().forEach { symbol ->
                    stringBuilder.append(symbol)
                }

                val newChain = Chain().apply {
                    data = stringBuilder.toString()
                    graph = newGraph
                }
                //Log.d(LOG_TAG, "New chain ${chain.data}")
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

    fun hasNonTerminals(chain: String): Boolean{
        var hasNonTerminals = false
        for (symbol in chain.toCharArray()){
            if (nonTerminals.contains(symbol)){
                hasNonTerminals = true
                break
            }
        }
        return hasNonTerminals
    }

    fun filterTerminal(chain: String): String{
        return chain.filter{terminals.contains(it)}
    }
}