package com.example.grammar.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.grammar.model.Grammar

@Database(entities = [Grammar::class], version = 2)
abstract class GrammarDatabase : RoomDatabase(){
    abstract fun grammarDao() : GrammarDAO
    companion object{
        private var instance: GrammarDatabase? = null
        fun getInstance(context : Context) : GrammarDatabase?{
            if (instance == null){
                synchronized(GrammarDatabase::class){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        GrammarDatabase::class.java,
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