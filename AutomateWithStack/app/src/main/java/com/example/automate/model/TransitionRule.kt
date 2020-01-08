package com.example.automate.model

data class TransitionRule(var state: Int, var symbol: String, var stack: List<String>)