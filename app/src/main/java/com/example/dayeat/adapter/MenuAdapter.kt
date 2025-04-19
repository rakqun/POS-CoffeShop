package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.Menu
import com.example.dayeat.model.MenuGroup
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter():RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    private val mMenu: MutableList<Menu> = mutableListOf()

    fun setData(item: MutableList<Menu>){
        mMenu.clear()
        mMenu.addAll(item)
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
        fun onItemClicked(menuGroup: Menu)
    }

    interface OnItemClickEditCallback {
        fun onItemClicked(menuGroup: Menu)
    }

    interface OnItemClickRemoveCallback {
        fun onItemClicked(menuGroup: Menu)
    }

    inner class ViewHolder(var binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mMenu.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mMenu[position]){
                binding.tvPriceMenu.visibility = View.VISIBLE

                binding.tvNameGroupMenu.text = this.menuName
                if (this.price != null){
                    binding.tvPriceMenu.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(if(this.netPrice!! > 0){this.netPrice}else{this.price})
                }else{
                    binding.tvPriceMenu.visibility = View.GONE
                }

                binding.cardItemMenu.setOnClickListener { onItemClickCallback?.onItemClicked(this) }
                binding.imgRemove.setOnClickListener { onItemClickRemoveCallback?.onItemClicked(this) }
                binding.imgEdit.setOnClickListener { onItemClickEditCallback?.onItemClicked(this) }
            }
        }
    }
}