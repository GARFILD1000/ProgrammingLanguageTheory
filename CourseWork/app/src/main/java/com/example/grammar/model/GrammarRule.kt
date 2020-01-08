package com.example.grammar.model

import java.util.*

class GrammarRule(){
    companion object{
        const val delimeter = "->"
    }
    constructor(leftPart: LinkedList<Symbol>, rightPart: LinkedList<Symbol>): this() {
        this.leftPart = leftPart
        this.rightPart = rightPart
    }

    var leftPart = LinkedList<Symbol>()
    var rightPart = LinkedList<Symbol>()

    override fun toString(): String{
        return "$leftPart $delimeter $rightPart"
    }
}