package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.Customer
import com.example.dayeat.model.Konfigurasi

class ListCustomerAdapter:RecyclerView.Adapter<ListCustomerAdapter.ViewAdapter>() {

    private val mCustomer: MutableList<Customer> = mutableListOf()

    fun setData(item: MutableList<Customer>){
        mCustomer.clear()
        mCustomer.addAll(item)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null

    interface OnItemClickCallback {
        fun onItemClicked(customer: Customer)
    }

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    inner class ViewAdapter(val binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewAdapter {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewAdapter(view)
    }

    override fun getItemCount(): Int = mCustomer.size

    override fun onBindViewHolder(holder: ViewAdapter, position: Int) {
        with(holder){
            with(mCustomer[position]){
                if (this.cFlags!!.contains("HIDE")){
                    binding.cardItemMenu.visibility = View.GONE
                }else{
                    binding.cardItemMenu.visibility = View.VISIBLE
                    binding.tvPriceMenu.visibility = View.GONE
                    binding.tvNameGroupMenu.text = this.cName
                    binding.imgRemove.visibility = View.GONE
                    binding.imgEdit.visibility = View.GONE
                    binding.cardItemMenu.setOnClickListener{onItemClickCallback!!.onItemClicked(this)}
                }
            }
        }
    }
}