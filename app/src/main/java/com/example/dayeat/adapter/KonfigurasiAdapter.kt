package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.Konfigurasi

class KonfigurasiAdapter:RecyclerView.Adapter<KonfigurasiAdapter.ViewAdapter>() {

    private val mKonfigurasi: MutableList<Konfigurasi> = mutableListOf()

    fun setData(item: MutableList<Konfigurasi>){
        mKonfigurasi.clear()
        mKonfigurasi.addAll(item)
        notifyDataSetChanged()
    }

    private var onItemClickEditCallback: OnItemClickEditCallback? = null
    private var onItemClickRemoveCallback: OnItemClickRemoveCallback? = null

    interface OnItemClickEditCallback {
        fun onItemClicked(menuGroup: Konfigurasi)
    }

    interface OnItemClickRemoveCallback {
        fun onItemClicked(menuGroup: Konfigurasi)
    }

    fun setOnItemClickEditCallback(onItemClickEditCallback: OnItemClickEditCallback) {
        this.onItemClickEditCallback = onItemClickEditCallback
    }

    fun setOnItemClickRemoveCallback(onItemClickRemoveCallback: OnItemClickRemoveCallback) {
        this.onItemClickRemoveCallback = onItemClickRemoveCallback
    }

    inner class ViewAdapter(val binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewAdapter {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewAdapter(view)
    }

    override fun getItemCount(): Int = mKonfigurasi.size

    override fun onBindViewHolder(holder: ViewAdapter, position: Int) {
        with(holder){
            with(mKonfigurasi[position]){
                binding.tvPriceMenu.visibility = View.GONE
                binding.tvNameGroupMenu.text = this.code
                binding.imgRemove.setOnClickListener { onItemClickRemoveCallback?.onItemClicked(this) }
                binding.imgEdit.setOnClickListener { onItemClickEditCallback?.onItemClicked(this) }
            }
        }
    }
}