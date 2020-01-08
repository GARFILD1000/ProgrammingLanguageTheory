package com.example.grammar.model

import java.util.*

class GraphElement(){
    companion object{
        private var idCounter = 0
    }
    var id = ""

    constructor(newData: Symbol): this() {
        data = newData
    }

    var data = Symbol()

    override fun toString(): String {
        return data.toString()
    }

    fun generateId(){
        idCounter++
        id = idCounter.toString()
    }

}