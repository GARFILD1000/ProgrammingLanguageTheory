package com.example.grammar.view

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.example.grammar.R


import kotlinx.android.synthetic.main.view_progress_dialog.view.*

class ProgressDialog{
    private var builder : AlertDialog.Builder
    private var dialog: AlertDialog? = null
    private var view: View? = null
    private var currentProgress = 0
    private var maxProgress = 100
    private var cancelEnable = true
    private var infinityProgress = false
    var onCancelClickListener: (progress: Int)->Unit = {}
    private var backgroundTransparency = 0.5f

    constructor(context: Context){
        val decorView = (context as Activity).window.decorView as ViewGroup
        builder = AlertDialog.Builder(context)
        view = context.layoutInflater.inflate(R.layout.view_progress_dialog, decorView, false)
    }

    fun setInfinityProgress(isInfinity: Boolean): ProgressDialog{
        infinityProgress = isInfinity
        return this
    }

    fun setDescriptionText(text: String): ProgressDialog{
        view?.descriptionTextView?.setText(text)
        return this
    }

    fun setDescriptionText(resId: Int): ProgressDialog{
        view?.descriptionTextView?.setText(resId)
        return this
    }

    fun setCancelButtonListener(cancelButtonListener:(Int)->Unit): ProgressDialog{
        onCancelClickListener = cancelButtonListener
        return this
    }

    fun setBackgroundTransparency(transparency: Float): ProgressDialog{
        backgroundTransparency = transparency
        return this
    }

    fun setCancelEnable(enable: Boolean): ProgressDialog{
        cancelEnable = enable
        return this
    }

    fun setMaxProgress(maxProgress: Int): ProgressDialog{
        this.maxProgress = maxProgress
        view?.normalProgressBar?.max = maxProgress
        return this
    }

    fun getMaxProgress(): Int{
        return this.maxProgress
    }

    fun setCurrentProgress(progress: Int): ProgressDialog{
        this.currentProgress = progress
        if (!infinityProgress){
            view?.normalProgressBar?.progress = progress
        }
        return this
    }

    fun getCurrentProgress(): Int{
        return this.currentProgress
    }

    fun show(){
        view?.cancelButton?.setOnClickListener{
            if (cancelEnable) {
                onCancelClickListener(currentProgress)
                dialog?.dismiss()
            }
        }
        view?.normalProgressBar?.visibility = if (infinityProgress) View.GONE else View.VISIBLE
        view?.infinityProgressBar?.visibility = if (infinityProgress) View.VISIBLE else View.GONE

        view?.cancelButton?.visibility = if (cancelEnable) View.VISIBLE else View.GONE
        builder.setView(view)
        dialog = builder.create()
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.show()
        if (backgroundTransparency in 0f..1f) {
            val lp = dialog!!.window?.attributes
            lp?.dimAmount = 1f-backgroundTransparency
            dialog!!.window?.attributes = lp
            dialog!!.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    fun hide(){
        dialog?.dismiss()
    }
}