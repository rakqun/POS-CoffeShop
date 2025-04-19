package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListPayBinding
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Pay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PayAdapter:RecyclerView.Adapter<PayAdapter.ViewHolder>() {

    private val mPay: MutableList<Pay> = mutableListOf()

    fun method(kondisi: String):String{return kondisi}

    fun setData(pay: MutableList<Pay>){
        mPay.clear()
        mPay.addAll(pay)
        notifyDataSetChanged()
    }

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(pay: Pay)
    }

    inner class ViewHolder(var binding: ListPayBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListPayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mPay.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mPay[position]){

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(
                    this.date.toString()
                )

                binding.tvPaymentMethod.visibility = View.GONE

                if (method == "method"){
                    binding.tvPaymentMethod.visibility = View.GONE
                    binding.tvPaymentMethod.text = this.method
                }

                binding.tvDatePay.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(dateFormat)
                binding.tvTablePay.text = this.salesNo + " - " + this.noMeja
                binding.tvQtyPay.text = this.qty.toString()
                binding.tvNetTotalPay.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(this.total!!.toInt())

                binding.cdPay.setOnClickListener { onItemClickCallback!!.onItemClicked(this) }
            }
        }
    }

}