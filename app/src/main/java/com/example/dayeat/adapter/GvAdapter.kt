package com.example.dayeat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.dayeat.R

class GvAdapter(private val listInt: MutableList<String>, private val context: Context, private val kondisi: Boolean):BaseAdapter() {

    private var layoutInflater: LayoutInflater? = null

    override fun getCount(): Int {
        return listInt.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getView(position: Int, v: View?, parent: ViewGroup?): View {
        var view = v
        if (layoutInflater == null) {
            layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }
        if (view == null) {
            view = layoutInflater!!.inflate(R.layout.list_grid, null)
        }

        val text = view?.findViewById<TextView>(R.id.tvTextGrid)
        text?.text = listInt[position]

        if (kondisi){
            text?.textSize = 18f
        }

        return view!!
    }
}