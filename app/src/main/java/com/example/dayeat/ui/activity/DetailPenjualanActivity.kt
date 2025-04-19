package com.example.dayeat.ui.activity

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.R
import com.example.dayeat.adapter.OrderItemsAdapter
import com.example.dayeat.adapter.PayAdapter
import com.example.dayeat.databinding.ActivityDetailPenjualanBinding
import com.example.dayeat.model.Pay
import com.example.dayeat.model.Product
import com.example.dayeat.mvvm.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.text.NumberFormat
import java.util.Locale

class DetailPenjualanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailPenjualanBinding

    private var payAdapter: PayAdapter = PayAdapter()
    private var mPay: MutableList<Pay> = mutableListOf()

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPenjualanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val getStart = intent.getStringExtra("start")
        val getEnd = intent.getStringExtra("end")

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvPenjualan.layoutManager = GridLayoutManager(this, 1)
        } else {
            binding.rvPenjualan.layoutManager = GridLayoutManager(this, 3)
        }

        binding.rvPenjualan.setHasFixedSize(true)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.querySales(this@DetailPenjualanActivity, getStart.toString(), getEnd.toString())
            }
        }

        mainViewModel.getPay().observe(this, Observer { pay ->
            if (pay != null){
                mPay.clear()
                mPay.addAll(pay)
                payAdapter.setData(mPay)
                payAdapter.method("method")
                showItems()
            }
        })

        binding.swipeDetailPenjualan.setOnRefreshListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    mainViewModel.querySales(this@DetailPenjualanActivity, getStart.toString(), getEnd.toString())
                }
                binding.swipeDetailPenjualan.isRefreshing = false
            }
        }

        binding.fbBack.setOnClickListener {
            finish()
        }
    }

    private fun showItems(){
        binding.rvPenjualan.adapter = payAdapter
        payAdapter.setOnItemClickCallback(object : PayAdapter.OnItemClickCallback {
            override fun onItemClicked(pay: Pay) {
                val dialogAddCategory = Dialog(this@DetailPenjualanActivity)
                dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogAddCategory.setContentView(R.layout.dialog_list_pesanan)

                val rvListOrder = dialogAddCategory.findViewById<RecyclerView>(R.id.rvMenuPesanan)
                val tvQty = dialogAddCategory.findViewById<TextView>(R.id.tvQuantyBawah)
                val tvTotal = dialogAddCategory.findViewById<TextView>(R.id.tvTotalBawah)
                val btnCancel = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnVoidListPesanan)
                val btnOrder = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnInput)
                val tvBtnOrder = dialogAddCategory.findViewById<TextView>(R.id.tvBtnOrder)
                val tvBtnVoid = dialogAddCategory.findViewById<TextView>(R.id.tvBtnVoid)
                val tvMeja = dialogAddCategory.findViewById<TextView>(R.id.tvMejaDialog)
                val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.btnCloseDialogPesanan)
                val btnAddOrder = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnAddItems)
                val btnPrint = dialogAddCategory.findViewById<ImageView>(R.id.btnPrintPesanan)

                btnPrint.visibility = View.GONE
                btnAddOrder.visibility = View.GONE
                btnOrder.visibility = View.GONE
                btnCancel.visibility = View.GONE

                rvListOrder.layoutManager = LinearLayoutManager(this@DetailPenjualanActivity)
                rvListOrder.setHasFixedSize(true)

                tvMeja.text = pay.noMeja.toString()
                tvBtnOrder.text = "PAY"
                tvBtnVoid.text  = "VOID"

                val mProduct : MutableList<Product> = mutableListOf()

                lifecycleScope.launch {
                    withContext(Dispatchers.IO){
                        try {
                            val jsonArray = JSONArray(pay.detail)
                            for (i in 0 until jsonArray.length()){
                                val getObject = jsonArray.getJSONObject(i)
                                val product = Product()
                                product.menuId  = getObject.getString("MenuID")
                                product.quanty  = getObject.getInt("Qty")
                                product.harga   = getObject.getInt("Price")
                                product.total   = getObject.getInt("NetTotal")
                                product.nama    = getObject.getString("MenuName")
                                product.catatan = getObject.getString("Request")
                                mProduct.add(product)
                            }
                        }catch (e: JSONException){
                            e.printStackTrace()
                        }
                    }
                    var orderProductAdapter : OrderItemsAdapter? = null
                    orderProductAdapter = OrderItemsAdapter(mProduct, false)
                    rvListOrder.adapter = orderProductAdapter
                    tvQty.text = mProduct.sumOf { it.quanty!!.toInt() }.toString()
                    tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.total!!.toInt() })
                }

                btnClose.setOnClickListener {
                    dialogAddCategory.dismiss()
                }

                dialogAddCategory.show()
                dialogAddCategory.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
                dialogAddCategory.window?.setGravity(Gravity.CENTER)
            }
        })
    }
}