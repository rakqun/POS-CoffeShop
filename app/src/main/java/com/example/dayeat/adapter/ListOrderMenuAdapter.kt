package com.example.dayeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.R
import com.example.dayeat.model.OrderItems
import java.text.NumberFormat
import java.util.Locale

class ListOrderMenuAdapter(private val listItem: MutableList<OrderItems>): RecyclerView.Adapter<ListOrderMenuAdapter.ViewHolder>(),
    Filterable {

    private var onItemClickCallback: OnItemClickCallback? = null

    private val searchItem = ArrayList<OrderItems>(listItem)

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: OrderItems)
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvNameOrderMenu)
        private val tvStock = itemView.findViewById<TextView>(R.id.tvHargaOrderMenu)
        private val llBgItems = itemView.findViewById<ConstraintLayout>(R.id.llBgItemList)
        fun bind(items: OrderItems){
            tvName.text = items.nama
            tvStock.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(items.harga)

            llBgItems.background = itemView.resources.getDrawable(R.drawable.bg_order_item)

            if (items.todayDisable == 1){
                llBgItems.background = itemView.resources.getDrawable(R.drawable.bg_order_item_disable)
            }else{
                itemView.setOnClickListener{onItemClickCallback?.onItemClicked(items)}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_order_items, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listItem[position])
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList = ArrayList<OrderItems>()

                if (p0!!.isBlank() or p0.isEmpty()){
                    filteredList.addAll(searchItem)
                }else{
                    val filterPatern = p0.toString().toLowerCase(Locale.ROOT).trim()

                    searchItem.forEach{
                        if (it.nama!!.toString().toLowerCase(Locale.ROOT).contains(filterPatern)){
                            filteredList.add(it)
                        }
                    }
                }

                val result = FilterResults()
                result.values = filteredList
                return result
            }

            override fun publishResults(p0: CharSequence, p1: FilterResults) {
                listItem.clear()
                listItem.addAll(p1.values as MutableList<OrderItems>)
                notifyDataSetChanged()
            }
        }
    }
}