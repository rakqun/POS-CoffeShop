package com.example.dayeat.adapter

import android.content.Context
import android.widget.TextView
import com.example.dayeat.R
import com.example.dayeat.model.Category
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.util.Locale

class MyMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {


    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val mCategory: ArrayList<String> = arrayListOf()

    fun setNameCategory(category: ArrayList<String>){
        mCategory.clear()
        mCategory.addAll(category)
    }
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tvContent.text = "${mCategory[e?.x?.toInt() ?: 0]}\nValue: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(e?.y?.toInt() ?: 0)}"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
