package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListSplitPesananBinding
import com.example.dayeat.model.Pay
import com.example.dayeat.model.Product
import com.example.dayeat.model.SalesItem
import java.text.NumberFormat
import java.util.Locale

class ListSplitAdapter:RecyclerView.Adapter<ListSplitAdapter.ViewHolder>() {

    private val mProduct: MutableList<SalesItem> = mutableListOf()

    fun setData(product: MutableList<SalesItem>){
        mProduct.clear()
        mProduct.addAll(product)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null
    private var onItemClickCallbackQty: OnItemClickCallbackQty? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    fun setOnItemClickCallbackQty(onItemClickCallbackQty: OnItemClickCallbackQty) {
        this.onItemClickCallbackQty = onItemClickCallbackQty
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: SalesItem, position: Int)
    }

    interface OnItemClickCallbackQty {
        fun onItemClicked(items: SalesItem)
    }

    inner class ViewHolder(val binding: ListSplitPesananBinding):RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListSplitPesananBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mProduct.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mProduct[position]){
                binding.tvNameSplit.text = this.nama
                binding.tvQuantySplit.text = this.quanty.toString()
                binding.imgCheckSplit.visibility = View.GONE

                if (this.move == true){
                    binding.imgCheckSplit.visibility = View.VISIBLE
                }

                binding.tvQuantySplit.setOnClickListener{onItemClickCallbackQty?.onItemClicked(this)}
                binding.tvNameSplit.setOnClickListener{onItemClickCallback?.onItemClicked(this, position)}
            }
        }
    }
}