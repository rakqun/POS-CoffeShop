package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.MenuGroup

class CategoryAdapter():RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private val mMenuGroup: MutableList<MenuGroup> = mutableListOf()

    fun setData(item: MutableList<MenuGroup>){
        mMenuGroup.clear()
        mMenuGroup.addAll(item)
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
        fun onItemClicked(menuGroup: MenuGroup)
    }

    interface OnItemClickEditCallback {
        fun onItemClicked(menuGroup: MenuGroup)
    }

    interface OnItemClickRemoveCallback {
        fun onItemClicked(menuGroup: MenuGroup)
    }

    inner class ViewHolder(var binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mMenuGroup.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mMenuGroup[position]){
                binding.tvPriceMenu.visibility = View.GONE

                binding.tvNameGroupMenu.text = this.catName

                binding.cardItemMenu.setOnClickListener { onItemClickCallback?.onItemClicked(this) }
                binding.imgRemove.setOnClickListener { onItemClickRemoveCallback?.onItemClicked(this) }
                binding.imgEdit.setOnClickListener { onItemClickEditCallback?.onItemClicked(this) }
            }
        }
    }
}