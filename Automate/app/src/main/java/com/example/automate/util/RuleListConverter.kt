package com.example.automate.util

import androidx.room.TypeConverter
import com.example.automate.model.GrammarRule
import java.util.*

class RuleListConverter{
    @TypeConverter
    fun fromList(fromRules: LinkedList<GrammarRule>):String{
        val stringBuilder = StringBuilder()
        for (rule in fromRules){
            stringBuilder.append(rule.leftPart)
            stringBuilder.append(GrammarRule.delimeter)
            stringBuilder.append(rule.rightPart)
            stringBuilder.append("\n")
        }
        val lastLineDivider = stringBuilder.lastIndexOf("\n")
        if (lastLineDivider >= 0) stringBuilder.removeRange(lastLineDivider, lastLineDivider)
        return  stringBuilder.toString()
    }

    @TypeConverter
    fun toList(toRules: String): LinkedList<GrammarRule> {
        val allRules = LinkedList<GrammarRule>()
        val everyRulesString = toRules.split("\n")
        for (ruleString in everyRulesString){
            val ruleParts = ruleString.split(GrammarRule.delimeter)
            val parsedRule = GrammarRule().apply{
                leftPart = ruleParts.getOrNull(0)?:""
                rightPart = ruleParts.getOrNull(1)?:""
            }
            if (parsedRule.leftPart.isNotEmpty()) {
                allRules.add(parsedRule)
            }
        }
        return allRules
    }

}