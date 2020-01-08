package com.example.grammar.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.adapter.ChainsAdapter
import com.example.grammar.R
import com.example.grammar.databinding.FragmentChainsBinding
import com.example.grammar.model.GeneratorResults
import com.example.grammar.model.Grammar
import com.example.grammar.repository.LocalStorageService
import kotlinx.android.synthetic.main.fragment_chains.*
import kotlinx.android.synthetic.main.fragment_chains.aboutButton
import kotlinx.android.synthetic.main.fragment_chains.loadButton
import kotlinx.android.synthetic.main.fragment_chains.saveButton
import kotlinx.android.synthetic.main.fragment_grammar.*

class ChainsFragment: Fragment() {
    lateinit var binding: FragmentChainsBinding
    lateinit var chainsListAdapter: ChainsAdapter
    var currentGrammar: Grammar? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_chains, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    private fun showToast(string: String) {
        Toast.makeText(context, string, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        checkNeedLoadResults()
        super.onResume()
    }

    private fun checkNeedLoadResults(){
        val fileToLoad = (context as? MainActivity)?.fileToLoad?: return
        val needLoadResults = (context as? MainActivity)?.needLoadResults?: return
        if (needLoadResults) {
            val results = LocalStorageService.loadResults(fileToLoad.absolutePath)
            (context as? MainActivity)?.fileToLoad = null
            (context as? MainActivity)?.needLoadResults = false
            results?.let{
                setResults(it)
                showToast("Результаты загружены")
            }
        }
    }

    private fun setResults(results: GeneratorResults){
        chainsListAdapter.setItems(results.chains)
        chainsCount.setText(results.chains.size.toString())
        currentGrammar = results.grammar
    }

    private fun saveResults(results: GeneratorResults){
        val result = LocalStorageService.saveResults(results)
        if (result == null) {
            showToast("Ошибка сохранения")
        } else {
            showToast("Файл ${result.name} сохранён")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chainsListAdapter = ChainsAdapter()
        chainsList.adapter = chainsListAdapter
        chainsList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        chainsList.setHasFixedSize(true)

        (context as MainActivity).generatorResults?.let {results ->
                setResults(results)
        }

        chainsListAdapter.onChainClick = { chain ->
            (context as MainActivity).chainToShow = chain
            (context as MainActivity).goToGraph()
        }

        aboutButton.setOnClickListener {
            val resId = R.string.app_info
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(WebViewActivity.KEY_RESOURCE_ID, resId)
            startActivity(intent)
        }

        loadButton.setOnClickListener {
            (context as? MainActivity)?.goLoadResults()
        }

        saveButton.setOnClickListener {
            saveResults(
                GeneratorResults().apply{
                    chains = chainsListAdapter.getItems()
                }
            )
        }
    }
}