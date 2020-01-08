package com.example.automate.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.automate.model.Automate
import com.example.automate.model.Grammar

@Database(entities = [Grammar::class, Automate::class], version = 3)
abstract class LanguageDatabase : RoomDatabase(){
    abstract fun grammarDao() : GrammarDAO
    abstract fun automateDao(): AutomateDAO
    companion object{
        private var instance: LanguageDatabase? = null
        fun getInstance(context : Context) : LanguageDatabase?{
            if (instance == null){
                synchronized(LanguageDatabase::class){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        LanguageDatabase::class.java,
                        "project_urls.db")
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return instance
        }
        fun freeInstance(){
            instance = null
        }
    }
}