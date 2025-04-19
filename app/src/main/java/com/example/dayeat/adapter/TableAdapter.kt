package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.Menu
import com.example.dayeat.model.Table
import com.google.android.material.tabs.TabLayout.Tab
import java.text.NumberFormat
import java.util.Locale

class TableAdapter():RecyclerView.Adapter<TableAdapter.ViewHolder>() {

    private val mTable: MutableList<Table> = mutableListOf()

    fun setData(item: MutableList<Table>){
        mTable.clear()
        mTable.addAll(item)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null
    private var onItemClickEditCallback: OnItemClickEditCallback? = null
    private var onItemClickRemoveCallback: OnItemClickRemoveCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    fun setOnItemClickEditCallback(onItemClickEditCallback: OnItemClickEditCallback) {
        this.onItemClickEditCallback = onItemClickEditCallback
    }

    fun setOnItemClickRemoveCallback(onItemClickRemoveCallback: OnItemClickRemoveCallback) {
        this.onItemClickRemoveCallback = onItemClickRemoveCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(table: Table)
    }

    interface OnItemClickEditCallback {
        fun onItemClicked(table: Table)
    }

    interface OnItemClickRemoveCallback {
        fun onItemClicked(table: Table)
    }

    inner class ViewHolder(var binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mTable.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mTable[position]){
                binding.tvPriceMenu.visibility = View.GONE
                binding.tvNameGroupMenu.text = this.tableCode
                binding.imgRemove.setOnClickListener { onItemClickRemoveCallback?.onItemClicked(this) }
                binding.imgEdit.setOnClickListener { onItemClickEditCallback?.onItemClicked(this) }
            }
        }
    }
}