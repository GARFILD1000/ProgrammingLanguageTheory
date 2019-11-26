package com.example.automate.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.automate.R
import com.example.automate.model.Automate
import com.example.automate.model.Chain
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navController = Navigation.findNavController(
            this,
            R.id.nav_host_fragment
        )
    }

    var automate: Automate? = null

    fun goToChainsList() {
        navController.navigate(R.id.chainsFragment)
    }

    fun goToGraph() {
        navController.navigate(R.id.graphFragment)
    }
}
