package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.databinding.ListMenuItemBinding
import com.example.dayeat.model.Menu
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.UserAccount
import java.text.NumberFormat
import java.util.Locale

class UserAdapter():RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private val mUserAccount: MutableList<UserAccount> = mutableListOf()

    fun setData(item: MutableList<UserAccount>){
        mUserAccount.clear()
        mUserAccount.addAll(item)
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
        fun onItemClicked(userAccount: UserAccount)
    }

    interface OnItemClickEditCallback {
        fun onItemClicked(userAccount: UserAccount)
    }

    interface OnItemClickRemoveCallback {
        fun onItemClicked(userAccount: UserAccount)
    }

    inner class ViewHolder(var binding: ListMenuItemBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = mUserAccount.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(mUserAccount[position]){
                binding.tvNameGroupMenu.text = this.userName
                binding.tvPriceMenu.visibility = View.GONE

                binding.cardItemMenu.setOnClickListener { onItemClickCallback?.onItemClicked(this) }
                binding.imgRemove.setOnClickListener { onItemClickRemoveCallback?.onItemClicked(this) }
                binding.imgEdit.setOnClickListener { onItemClickEditCallback?.onItemClicked(this) }
            }
        }
    }
}