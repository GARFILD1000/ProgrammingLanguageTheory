package com.example.grammar.model

import java.util.*

class GeneratorParams {
    var terminals = LinkedList<Symbol>()
    var minLength = 0
    var maxLength = 0
    var multiplicity = 1
    var startSubchain = ""
    var endSubchain = ""
    var leftLinearOutput = false
}