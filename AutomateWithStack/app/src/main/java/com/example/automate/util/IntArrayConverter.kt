package com.example.automate.util

import androidx.room.TypeConverter
import com.example.automate.model.GrammarRule
import java.lang.StringBuilder
import java.util.*

class IntArrayConverter{
    @TypeConverter
    fun fromArray(fromList: Array<Int>):String{
        val stringBuilder = StringBuilder()
        fromList.forEach {
            stringBuilder.append(it.toString())
            stringBuilder.append("\n")
        }
        val lastLineDivider = stringBuilder.lastIndexOf("\n")
        if (lastLineDivider >= 0) stringBuilder.removeRange(lastLineDivider, lastLineDivider)
        return stringBuilder.toString()
    }

    @TypeConverter
    fun toArray(toList: String): Array<Int> {
        val list = mutableListOf<Int>()
        toList.split("\n").forEach{
            it.toIntOrNull()?.let{
                list.add(it)
            }
        }
        return list.toTypedArray()
    }
}