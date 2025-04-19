package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.databinding.ListPaymentPenjualanBinding
import com.example.dayeat.model.Menu
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Pay
import com.example.dayeat.model.Payment
import java.text.NumberFormat
import java.util.Locale

class PaymentAdapter():RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {

    private val mPayment: MutableList<Payment> = mutableListOf()

    fun setData(payment: MutableList<Payment>){
        mPayment.clear()
        mPayment.addAll(payment)
        notifyDataSetChanged()
    }

    inner class ViewHolder(var binding: ListPaymentPenjualanBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListPaymentPenjualanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mPayment.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mPayment[position]){
                binding.tvTextJenisPayment.text = this.jenis
                binding.tvTextTotalJenisPayment.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(this.total)
            }
        }
    }
}