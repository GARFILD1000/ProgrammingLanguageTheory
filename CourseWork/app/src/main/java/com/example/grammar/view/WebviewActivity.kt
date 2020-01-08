package com.example.grammar.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.grammar.R
import kotlinx.android.synthetic.main.activity_webview.*

class WebViewActivity: AppCompatActivity() {
    companion object{
        const val KEY_RESOURCE_ID = "resid"
        const val KEY_ARGS = "args"
        const val ARGS_SEPARATOR = "$"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val args = intent.getStringExtra(KEY_ARGS)?.split(ARGS_SEPARATOR)?.toTypedArray()
        val resId = intent.getIntExtra(KEY_RESOURCE_ID, R.string.app_info)

        val text = if (args == null)resources.getString(resId) else resources.getString(resId, *args)
        webView.loadDataWithBaseURL("http://live.airy.tv/", text, "text/html", "UTF-8", null)
    }
}