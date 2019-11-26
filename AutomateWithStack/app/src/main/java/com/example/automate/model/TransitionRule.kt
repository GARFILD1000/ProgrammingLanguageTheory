package com.example.automate.model

data class TransitionRule(var state: String, var symbol: String, var stack: List<String>)