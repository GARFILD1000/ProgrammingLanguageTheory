package com.example.grammar.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grammar.adapter.ChainsAdapter
import com.example.grammar.R
import com.example.grammar.adapter.FilesAdapter
import com.example.grammar.databinding.FragmentChainsBinding
import com.example.grammar.databinding.FragmentFilesBinding
import com.example.grammar.repository.LocalStorageService
import kotlinx.android.synthetic.main.fragment_chains.*
import kotlinx.android.synthetic.main.fragment_files.*
import java.io.File

class FilesFragment : Fragment() {
    lateinit var binding: FragmentFilesBinding
    lateinit var filesListAdapter: FilesAdapter
    lateinit var filesLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_files, container, false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filesListAdapter = FilesAdapter()
        filesListAdapter.onItemClick = {
            filesListAdapter.selectItem(it)
        }
        filesListAdapter.onDeleteItem = {
            filesListAdapter.getItem(it)?.let { file ->
                file.delete()
                filesListAdapter.removeItem(it)
            }
        }
        filesLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        filesList.adapter = filesListAdapter
        filesList.layoutManager = filesLayoutManager
        filesList.setHasFixedSize(true)
        val directory = (context as MainActivity).directoryToShow
        directory?.listFiles()?.let { files ->
            showFiles(files)
        }

        doneButton.setOnClickListener {
            filesListAdapter.getItem(filesListAdapter.selectedItem)?.let{file ->
                (context as? MainActivity)?.fileToLoad = file
            }
            activity?.onBackPressed()
        }

        closeButton.setOnClickListener {
            (context as? MainActivity)?.fileToLoad = null
            activity?.onBackPressed()
        }
    }

    fun showFiles(files: Array<File>) {
        filesListAdapter.setItems(files.toList())
    }
}