package com.example.grammar.model

import com.example.grammar.utils.TreeVertex
import java.util.*

class Chain{
    var data = LinkedList<Symbol>()
    var size = 0
    private set
    get() = data.size
    var graph: TreeVertex<GraphElement>? = null

    override fun toString(): String {
        return data.joinToString("")
    }
}