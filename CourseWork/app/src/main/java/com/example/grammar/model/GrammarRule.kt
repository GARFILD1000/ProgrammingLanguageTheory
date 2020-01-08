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
        val stringBuilder = StringBuilder()
        leftPart.forEach {
            stringBuilder.append(it)
        }
        stringBuilder.append(" $delimeter ")
        if (rightPart.isEmpty() || rightPart.size == 1 && rightPart.firstOrNull() == Symbol.EMPTY) {
            stringBuilder.append(Symbol.LAMBDA)
        } else {
            rightPart.forEach{
                stringBuilder.append(it)
            }
        }
        return stringBuilder.toString()
    }

    fun addToLeft(newSymbol: Symbol, addFirst: Boolean = false){
        if (addFirst) {
            leftPart.addFirst(newSymbol)
        } else {
            leftPart.addLast(newSymbol)
        }
    }

    fun addToRight(newSymbol: Symbol, addFirst: Boolean = false){
        if (addFirst) {
            rightPart.addFirst(newSymbol)
        } else {
            rightPart.addLast(newSymbol)
        }
    }

    fun removeFromLeft(removeFirst: Boolean = false) {
        if (removeFirst) {
            leftPart.removeFirst()
        } else {
            leftPart.removeLast()
        }
    }

    fun removeFromRight(removeFirst: Boolean = false) {
        if (removeFirst) {
            rightPart.removeFirst()
        } else {
            rightPart.removeLast()
        }
    }
}