package com.example.grammar.repository

import android.Manifest
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import android.media.MediaScannerConnection
import android.R
import android.app.Activity
import android.content.ContentProvider
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.grammar.App
import com.example.grammar.model.GeneratorParams
import com.example.grammar.model.GeneratorResults
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.*
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

object LocalStorageService{
    const val SUCCESS = 0
    const val ERROR_SAVING_PROJECT_FILE = 1
    const val ERROR_CREATING_PROJECT_DIR = 2

    const val LOG_TAG = "LocalStorageService"
    const val PARAMS_FOLDER: String = "params"
    const val RESULTS_FOLDER: String = "results"
    const val SUFFIX_TEXT_FILE = ".txt"

    private var copyingEnabled = true
    private val storageJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + storageJob)
    private val ioScope = CoroutineScope(Dispatchers.IO + storageJob)
    val externalDir = App.getContext().getExternalFilesDir(null)

    var gson = GsonBuilder().create()

    fun createParamsDir(): Boolean{
        var dirCreated : Boolean = false
        if (externalDir != null) {
            val paramsDir = File(externalDir, PARAMS_FOLDER)
            dirCreated = if (!paramsDir.exists()) paramsDir.mkdir() else true
        }
        return dirCreated
    }

    fun createResultsDir(): Boolean{
        var dirCreated : Boolean = false
        if (externalDir != null) {
            val resultsDir = File(externalDir, RESULTS_FOLDER)
            dirCreated = if (!resultsDir.exists()) resultsDir.mkdir() else true
        }
        return dirCreated
    }

    fun getParamsDirPath(): File {
        return File(externalDir, PARAMS_FOLDER)
    }

    fun getResultsDirPath(): File {
        return File(externalDir, RESULTS_FOLDER)
    }

    fun getMediaDir(): File {
        return App.getContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)!!
    }

    fun saveResults(generatorResults: GeneratorResults): File?{
        return if (createResultsDir()){
            val resultsFile = createFile(getResultsDirPath(), SUFFIX_TEXT_FILE)
            val jsonData = gson.toJson(generatorResults)
            writeFile(resultsFile.absolutePath, jsonData)
            resultsFile
        } else {
            null
        }
    }

    fun saveParams(generatorParams: GeneratorParams): File?{
        return if (createParamsDir()){
            val resultsFile = createFile(getParamsDirPath(), SUFFIX_TEXT_FILE)
            val jsonData = gson.toJson(generatorParams)
            writeFile(resultsFile.absolutePath, jsonData)
            resultsFile
        } else {
            null
        }
    }

    fun loadParams(filePath: String): GeneratorParams {
        val jsonData = readFile(filePath)
        return gson.fromJson<GeneratorParams>(jsonData, GeneratorParams::class.java)
    }

    fun loadResults(filePath: String): GeneratorResults? {
        val jsonData = readFile(filePath)
        return gson.fromJson<GeneratorResults>(jsonData, GeneratorResults::class.java)
    }

    fun readFile(fullFilePath: String): String{
        var readedValue = ""
        try{
            readedValue = FileInputStream(fullFilePath).run{
                bufferedReader().use {
                    it.readText()
                }
            }
        }
        catch(ex : IOException){
            ex.printStackTrace()
        }
        return readedValue
    }

    fun writeFile(fullFilePath: String, data: String) : Boolean{
        var writed = false
        try{
            val writer = OutputStreamWriter(FileOutputStream(fullFilePath))
            writer.write(data)
            writer.close()
            writed = true

        }
        catch(ex : IOException){
            ex.printStackTrace()
        }
        return writed
    }

    fun writeFile(context: Context, fullFilePath: String, data: ByteArray) : Boolean{
        var writed = false
        try{
            FileOutputStream(fullFilePath).use{
                it.write(data)
                it.close()
                writed = true
            }
            MediaScannerConnection.scanFile(context,
                arrayOf(fullFilePath),
                null){_,_->}
        }
        catch(ex : IOException){
            ex.printStackTrace()
        }
        return writed
    }

    /* Func for creating File */
    @Throws(IOException::class)
    fun createFile(storageDir: File, suffix: String): File {
        val timeStamp: String = SimpleDateFormat("HHmmss_ddMMyyyy").format(Date())
        val file = File(storageDir, timeStamp + suffix)
        if (file.exists()) file.delete()
        file.createNewFile()
        return file
    }

    /* Func for creating temp File */
    @Throws(IOException::class)
    fun createTempFile(context: Context, suffix: String): File {
        val randomFilename = UUID.randomUUID().toString()
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
        val file = File.createTempFile(randomFilename, suffix, storageDir)
        return file
    }

    /* Func for deleting File */
    @Throws(IOException::class)
    fun deleteFile(path: String): Boolean {
        return path.isNotEmpty() && File(path).delete()
    }
}