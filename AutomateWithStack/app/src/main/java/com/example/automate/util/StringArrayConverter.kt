package com.example.automate.util

import androidx.room.TypeConverter
import com.example.automate.model.GrammarRule
import java.util.*

class StringArrayConverter{
    @TypeConverter
    fun fromArray(fromList: Array<String>):String{
        val stringBuilder = StringBuilder()
        for (string in fromList){
            stringBuilder.append(string)
            stringBuilder.append("\n")
        }
        val lastLineDivider = stringBuilder.lastIndexOf("\n")
        if (lastLineDivider >= 0) stringBuilder.removeRange(lastLineDivider, lastLineDivider)
        return  stringBuilder.toString()
    }

    @TypeConverter
    fun toArray(toList: String): Array<String> {
        val allStrings = toList.split("\n").toTypedArray()
        return allStrings
    }
}