package com.example.automate.repository

import com.example.automate.model.Grammar

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.example.automate.App
import com.example.automate.model.Automate


//class for accessing database
object LanguageRepository{
    private var grammarDao : GrammarDAO
    private var automateDAO : AutomateDAO
    private var grammars : LiveData<List<Grammar>>
    private var automates: LiveData<List<Automate>>

    val application = App.instance

    init{
        val database = LanguageDatabase.getInstance(application)
        grammarDao = database!!.grammarDao()
        automateDAO = database.automateDao()
        grammars = grammarDao.getAll()
        automates = automateDAO.getAll()
    }

    suspend fun getGrammarById(grammarId: Int, onGrammarLoaded : (Grammar)->Unit){
        val grammar = grammarDao.getById(grammarId)
        onGrammarLoaded(grammar!!)
    }

    suspend fun isGrammarExists(grammarId: Int):Boolean{
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

    suspend fun getFirstGrammar(): Grammar?{
        return grammarDao.getFirst()
    }

    suspend fun insertGrammar(grammar : Grammar){
        grammarDao.insert(grammar)
    }

    suspend fun updateGrammar(grammar : Grammar){
        grammarDao.update(grammar)
    }

    suspend fun deleteGrammar(grammar : Grammar){
        grammarDao.delete(grammar)
    }

    suspend fun getAutomateById(automateId: Int, onAutomateLoaded : (Automate)->Unit){
        val automate = automateDAO.getById(automateId)
        onAutomateLoaded(automate!!)
    }

    suspend fun isAutomateExists(automateId: Int):Boolean{
        var exists = false
        val automate = automateDAO.getById(automateId)
        if (automate != null){
            exists = true
        }
        return exists
    }

    fun getAllAutomates() : LiveData<List<Automate>>{
        return automates
    }

    suspend fun getFirstAutomate(): Automate?{
        return automateDAO.getFirst()
    }

    suspend fun insertAutomate(automate: Automate){
        automateDAO.insert(automate)
    }

    suspend fun updateAutomate(automate: Automate){
        automateDAO.update(automate)
    }

    suspend fun deleteAutomate(automate: Automate){
        automateDAO.delete(automate)
    }

    suspend fun deleteAllAutomate(){
        automateDAO.deleteAll()
    }

    class doAsync(val databaseOperation: () -> Unit) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            databaseOperation()
            return null
        }
    }
}