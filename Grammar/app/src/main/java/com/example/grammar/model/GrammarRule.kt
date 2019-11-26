package com.example.grammar.model

class GrammarRule(){
    companion object{
        const val delimeter = "->"
    }
    var leftPart: String = ""
    var rightPart: String = ""

    override fun toString(): String{
        return "$leftPart -> $rightPart"
    }
}