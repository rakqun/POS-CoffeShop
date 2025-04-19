package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListPayBinding
import com.example.dayeat.model.CLosing
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Pay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ClosingAdapter:RecyclerView.Adapter<ClosingAdapter.ViewHolder>() {

    private val mClosing: MutableList<CLosing> = mutableListOf()

    fun method(kondisi: String):String{return kondisi}

    fun setData(closing: MutableList<CLosing>){
        mClosing.clear()
        mClosing.addAll(closing)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(closing: CLosing)
    }

    inner class ViewHolder(var binding: ListPayBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListPayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mClosing.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mClosing[position]){

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(
                    this.closingDate.toString()
                )

                binding.tvPaymentMethod.visibility = View.GONE

                binding.tvDatePay.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(dateFormat)
                binding.tvTablePay.text = this.closingBy
                binding.tvQtyPay.text = this.closingQty.toString()
                binding.tvNetTotalPay.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(this.closingTotal!!.toInt())

                binding.cdPay.setOnClickListener { onItemClickCallback!!.onItemClicked(this) }
            }
        }
    }

}