package com.example.grammar.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.grammar.R
import com.example.grammar.model.Chain
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

    var createdChains = LinkedList<Chain>()
    var chainToShow = Chain()

    fun goToChainsList() {
        navController.navigate(R.id.chainsFragment)
    }

    fun goToGraph() {
        navController.navigate(R.id.graphFragment)
    }
}
