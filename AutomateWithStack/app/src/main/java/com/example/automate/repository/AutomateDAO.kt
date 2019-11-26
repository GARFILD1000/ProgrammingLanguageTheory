package com.example.automate.repository

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.automate.model.Automate
import com.example.automate.model.Grammar

//interface that describes API for storing and restoring projects in database
@Dao
interface AutomateDAO{

    @Query("SELECT * FROM automate")
    fun getAll() : LiveData<List<Automate>>

    @Query("SELECT * FROM automate LIMIT 1")
    suspend fun getFirst() : Automate?

    @Query("SELECT * FROM automate WHERE id LIKE :id")
    suspend fun getById(id : Int) : Automate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automate: Automate)

    @Update
    suspend fun update(automate: Automate)

    @Delete
    suspend fun delete(automate: Automate)

    @Query("DELETE FROM automate")
    suspend fun deleteAll()
}