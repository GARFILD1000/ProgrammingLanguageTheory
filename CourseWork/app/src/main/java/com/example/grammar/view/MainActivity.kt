package com.example.grammar.view

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.grammar.App
import com.example.grammar.R
import com.example.grammar.model.Chain
import com.example.grammar.model.GeneratorParams
import com.example.grammar.model.GeneratorResults
import com.example.grammar.repository.LocalStorageService
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val PERMISSIONS_REQUEST_CODE = 20001
    }
    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navController = Navigation.findNavController(
            this,
            R.id.nav_host_fragment
        )
        checkAllNeededPermissions()
    }

    var generatorResults: GeneratorResults? = null
    var generatorParams: GeneratorParams? = null
    var chainToShow = Chain()
    var directoryToShow: File? = null
    var fileToLoad: File? = null
    var needLoadResults = false
    var needLoadParams = false

    private fun checkAllNeededPermissions() {
        val application = (applicationContext as App)
        val neededPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val allPermissionsGranted = application.checkPermissions(this, *neededPermissions)
        if (!allPermissionsGranted) {
            application.requestPermissions(this, PERMISSIONS_REQUEST_CODE, *neededPermissions)
        }
    }

    fun goToChainsList() {
        navController.navigate(R.id.chainsFragment)
    }

    fun goToGraph() {
        navController.navigate(R.id.graphFragment)
    }

    fun goLoadResults() {
        directoryToShow = LocalStorageService.getResultsDirPath()
        needLoadResults = true
        navController.navigate(R.id.filesFragment)
    }

    fun goLoadParams() {
        directoryToShow = LocalStorageService.getParamsDirPath()
        needLoadParams = true
        navController.navigate(R.id.filesFragment)
    }
}
