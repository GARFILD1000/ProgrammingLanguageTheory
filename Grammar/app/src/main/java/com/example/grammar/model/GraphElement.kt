package com.example.grammar.model

import java.util.*

class GraphElement{
    companion object{
        private var idCounter = 0
    }
    var id = ""
    var data: String = ""
    override fun toString(): String {
        return data
    }

    fun generateId(){
        idCounter++
        id = idCounter.toString()
    }

}