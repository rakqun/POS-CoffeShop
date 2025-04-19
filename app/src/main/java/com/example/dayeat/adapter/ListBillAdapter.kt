package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuSalesBinding
import com.example.dayeat.model.BillSales
import com.example.dayeat.model.SalesItem

class ListBillAdapter(private var mBillSales: MutableList<BillSales>):RecyclerView.Adapter<ListBillAdapter.ViewHolder>() {

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: BillSales)
    }

    inner class ViewHolder(val binding: ListMenuSalesBinding):RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMenuSalesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mBillSales.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mBillSales[position]){
                binding.tvNoBillSales.text = this.salesNo
                binding.tvNoMejaSales.text = this.noMeja
                binding.llBillSales.setOnClickListener { onItemClickCallback!!.onItemClicked(this) }
            }
        }
    }
}