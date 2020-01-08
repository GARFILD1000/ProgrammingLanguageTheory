package com.example.grammar.repository

import com.example.grammar.model.Grammar

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData


//class for accessing database
class GrammarRepository(application : Application){
    private var grammarDao : GrammarDAO
    private var grammars : LiveData<List<Grammar>>

    init{
        val database = GrammarDatabase.getInstance(application)
        grammarDao = database!!.grammarDao()
        grammars = grammarDao.getAll()
    }

    suspend fun getProjectById(projectId: String, onProjectLoaded : (Grammar)->Unit){
        val projectDescriptor = grammarDao.getById(projectId)
        onProjectLoaded(projectDescriptor!!)
    }

    suspend fun isGrammarExists(grammarId: String):Boolean{
        var exists = false
        val grammar = grammarDao.getById(grammarId)
        if (grammar != null){
            exists = true
        }
        return exists
    }

    fun getAllGrammars() : LiveData<List<Grammar>>{
        return grammars
    }

    fun getFirst(): Grammar?{
        return grammarDao.getFirst()
    }

    fun insertGrammar(grammar : Grammar){
        grammarDao.insert(grammar)
    }

    fun updateGrammar(grammar : Grammar){
        grammarDao.update(grammar)
    }

    fun deleteGrammar(grammar : Grammar){
        grammarDao.delete(grammar)
    }

    class doAsync(val databaseOperation: () -> Unit) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            databaseOperation()
            return null
        }
    }
}