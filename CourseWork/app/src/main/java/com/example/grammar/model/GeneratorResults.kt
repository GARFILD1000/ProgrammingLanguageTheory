package com.example.grammar.model

import java.util.*

class GeneratorResults(){
    var grammar = Grammar()
    var chains = LinkedList<Chain>()

    constructor(grammar: Grammar, chains: LinkedList<Chain>): this() {
        this.grammar = grammar
        this.chains = chains
    }
}