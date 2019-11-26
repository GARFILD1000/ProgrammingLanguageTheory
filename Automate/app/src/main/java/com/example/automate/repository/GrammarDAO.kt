package com.example.automate.repository

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.automate.model.Grammar

//interface that describes API for storing and restoring projects in database
@Dao
interface GrammarDAO{

    @Query("SELECT * FROM grammar")
    fun getAll() : LiveData<List<Grammar>>

    @Query("SELECT * FROM grammar LIMIT 1")
    suspend fun getFirst() : Grammar?

    @Query("SELECT * FROM grammar WHERE id LIKE :id")
    suspend fun getById(id : Int) : Grammar?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grammar: Grammar)

    @Update
    suspend fun update(grammar: Grammar)

    @Delete
    suspend fun delete(grammar: Grammar)
}