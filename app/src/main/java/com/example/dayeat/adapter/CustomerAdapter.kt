package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.Customer
import com.example.dayeat.model.Menu
import com.example.dayeat.model.MenuGroup
import java.text.NumberFormat
import java.util.Locale

class CustomerAdapter():RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    private val mCustomer: MutableList<Customer> = mutableListOf()

    fun setData(item: MutableList<Customer>){
        mCustomer.clear()
        mCustomer.addAll(item)
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
        fun onItemClicked(customer: Customer)
    }

    interface OnItemClickEditCallback {
        fun onItemClicked(customer: Customer)
    }

    interface OnItemClickRemoveCallback {
        fun onItemClicked(customer: Customer)
    }

    inner class ViewHolder(var binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mCustomer.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mCustomer[position]){
                binding.tvNameGroupMenu.text = this.cName
                binding.tvPriceMenu.visibility = View.GONE

                binding.imgRemove.setOnClickListener { onItemClickRemoveCallback?.onItemClicked(this) }
                binding.imgEdit.setOnClickListener { onItemClickEditCallback?.onItemClicked(this) }
            }
        }
    }
}