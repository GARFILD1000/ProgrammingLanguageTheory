package com.example.automate.model

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