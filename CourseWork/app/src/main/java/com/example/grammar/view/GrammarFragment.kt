package com.example.grammar.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.grammar.R
import com.example.grammar.databinding.FragmentGrammarBinding
import com.example.grammar.model.GeneratorParams
import com.example.grammar.model.GeneratorResults
import com.example.grammar.model.Symbol
import com.example.grammar.repository.LocalStorageService
import com.example.grammar.util.RegularRulesGenerator
import kotlinx.android.synthetic.main.fragment_grammar.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class GrammarFragment: Fragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + supervisorJob
    val supervisorJob = SupervisorJob()
    lateinit var binding: FragmentGrammarBinding
    val LOG_TAG = "GrammarFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_grammar, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aboutButton.setOnClickListener {
            val resId = R.string.app_info
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(WebViewActivity.KEY_RESOURCE_ID, resId)
            startActivity(intent)
        }

        createButton.setOnClickListener {
            createChains()
        }

        saveButton.setOnClickListener {
            saveParams()
        }

        loadButton.setOnClickListener {
            (context as? MainActivity)?.goLoadParams()
        }

        resultsButton.setOnClickListener {
            (context as? MainActivity)?.goToChainsList()
        }
    }

    override fun onResume() {
        checkNeedLoadParams()
        super.onResume()
    }

    private fun showToast(string: String) {
        Toast.makeText(context, string, Toast.LENGTH_LONG).show()
    }

    private fun checkNeedLoadParams(){
        val fileToLoad = (context as? MainActivity)?.fileToLoad?: return
        val needLoadParams = (context as? MainActivity)?.needLoadParams?: return
        if (needLoadParams) {
            val params = LocalStorageService.loadParams(fileToLoad.absolutePath)
            (context as? MainActivity)?.fileToLoad = null
            (context as? MainActivity)?.needLoadParams = false
            showToast("Параметры загружены")
            setParams(params)
        }
    }

    private fun setParams(params: GeneratorParams) {
        minLengthEditText.setText(params.minLength.toString())
        maxLengthEditText.setText(params.maxLength.toString())
        multiplicityEditText.setText(params.multiplicity.toString())
        startChainEditText.setText(params.startSubchain)
        endChainEditText.setText(params.endSubchain)
        terminalEditText.setText(params.terminals.joinToString(""))
        if (params.leftLinearOutput) {
            leftLinearOutputButton.isChecked = true
        } else {
            rightLinearOutputButton.isChecked = true
        }
    }

    private fun prepareParams(): GeneratorParams?{
        val minLength = minLengthEditText.text.toString().toIntOrNull()
        val maxLength = maxLengthEditText.text.toString().toIntOrNull()
        val multiplicity = multiplicityEditText.text.toString().toIntOrNull()
        minLength?: showToast("Неправильно задана минимальная длина")
        maxLength?: showToast("Неправильно задана максимальная длина")
        multiplicity?: showToast("Неправильно задана чётность цепочек")

        if (minLength == null || maxLength == null || multiplicity == null) {
            return null
        }

        val params = GeneratorParams()
        val terminals = LinkedList<Symbol>()
        terminals.addAll(
            terminalEditText.text.toString()
                .toCharArray()
                .map{ Symbol(it.toString(), true)}
        )
        params.terminals = terminals
        params.startSubchain = startChainEditText.text.toString()
        params.endSubchain = endChainEditText.text.toString()
        params.leftLinearOutput = leftLinearOutputButton.isChecked
        params.minLength = minLength
        params.maxLength = maxLength
        params.multiplicity = multiplicity
        return params
    }

    private fun saveParams(){
        prepareParams()?.let{generatorParams ->
            val result = LocalStorageService.saveParams(generatorParams)
            if (result == null) {
                showToast("Ошибка сохранения")
            } else {
                showToast("Файл ${result.name} сохранён")
            }
        }
    }

    private fun createChains(){
        val params = prepareParams()
        params?: return
        val dialog = ProgressDialog(context!!).apply {
            setCancelEnable(true)
            onCancelClickListener = {
                supervisorJob.cancelChildren()
                this.hide()
            }
            setInfinityProgress(true)
        }
        dialog.show()
        dialog.setDescriptionText("Генерирование праил грамматики")
        val generator = RegularRulesGenerator(params.terminals, params.leftLinearOutput, params.multiplicity)
        generator.addLastBlock(RegularRulesGenerator.Block(params.startSubchain, false))
        generator.addLastBlock(RegularRulesGenerator.Block("", true))
        generator.addLastBlock(RegularRulesGenerator.Block(params.endSubchain, false))
        var generatorResults = GeneratorResults()
        launch {
            withContext(Dispatchers.Default + supervisorJob) {
                val grammar = generator.generateGrammar()
                Log.d(LOG_TAG, "Generated grammar:\n$grammar")
                dialog.setDescriptionText("Генерирование цепочек")
                val chains = grammar.createChains(params.minLength, params.maxLength)
                generatorResults.grammar = grammar
                generatorResults.chains = chains
            }
            (context as MainActivity).generatorParams = params
            (context as MainActivity).generatorResults = generatorResults
            (context as MainActivity).goToChainsList()
            dialog.hide()
        }
    }

    override fun onPause() {
        super.onPause()
    }
}