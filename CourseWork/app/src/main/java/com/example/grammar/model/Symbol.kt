package com.example.grammar.model

class Symbol(
    var value: String = "",
    var isTerminal: Boolean = true
){
    companion object{
        const val LAMBDA = "λ"
        val EMPTY = Symbol("", true)
    }

    override fun equals(other: Any?): Boolean {
        return other is Symbol && other.value == value && other.isTerminal == isTerminal
    }

    override fun toString(): String {
        return value
    }
}