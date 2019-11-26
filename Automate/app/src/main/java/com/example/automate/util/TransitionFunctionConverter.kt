package com.example.automate.util

import androidx.room.TypeConverter
import com.example.automate.model.TransitionFunction

class TransitionFunctionConverter{

    @TypeConverter
    fun fromTransitionFunction(transitionFunction: TransitionFunction): String{
        return transitionFunction.toString()
    }

    @TypeConverter
    fun toTransitionFunction(string: String): TransitionFunction{
        return TransitionFunction.parse(string)
    }

}