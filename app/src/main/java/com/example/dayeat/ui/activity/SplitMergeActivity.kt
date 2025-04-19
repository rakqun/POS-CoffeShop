package com.example.dayeat.ui.activity

import android.app.Dialog
import android.app.ProgressDialog
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dayeat.R
import com.example.dayeat.adapter.ListBillAdapter
import com.example.dayeat.adapter.ListSplitAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.ActivitySplitMergeBinding
import com.example.dayeat.db.DbContract
import com.example.dayeat.model.BillSales
import com.example.dayeat.model.ItemMove
import com.example.dayeat.model.Product
import com.example.dayeat.model.SalesItem
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.SQLException
import java.text.NumberFormat
import java.util.Locale

class SplitMergeActivity : AppCompatActivity() {

    private lateinit var _binding: ActivitySplitMergeBinding
    private val binding get() = _binding

    private lateinit var connect: Conect
    private lateinit var sessionLogin: SessionLogin
    private lateinit var setting: Setting

    private lateinit var mainViewModel: MainViewModel

    private var listSplitAdapter: ListSplitAdapter = ListSplitAdapter()
    private lateinit var listBillAdapter : ListBillAdapter
    private val mProduct : MutableList<SalesItem> = mutableListOf()
    private val mBillSales : MutableList<BillSales> = mutableListOf()

    private var getSalesId: Int? = null
    private var getTable: Int? = null
    private var salesNo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySplitMergeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connect = Conect()
        sessionLogin = SessionLogin(this)
        setting = Setting(this)

        getSalesId = intent?.getIntExtra("SalesId", 0)
        getTable = intent?.getIntExtra("Table",0)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.rvMenuPesanan.layoutManager = LinearLayoutManager(this)
        binding.rvMenuPesanan.setHasFixedSize(false)

        mainViewModel.getProduct().observe(this@SplitMergeActivity) { p ->
            listSplitAdapter.setData(p)
            binding.rvMenuPesanan.adapter = listSplitAdapter
            listSplitAdapter.setOnItemClickCallbackQty(object : ListSplitAdapter.OnItemClickCallbackQty{
                override fun onItemClicked(items: SalesItem) {
                    val dialogQty = Dialog(this@SplitMergeActivity)
                    dialogQty.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialogQty.setContentView(R.layout.dialog_qty)

                    val tvTitleQty = dialogQty.findViewById<TextView>(R.id.tvTitleQty)
                    val etQty = dialogQty.findViewById<EditText>(R.id.tvQtyJumlah)
                    val btnSimpan = dialogQty.findViewById<RelativeLayout>(R.id.btnSimpanQty)
                    val btnCloseQty = dialogQty.findViewById<ImageView>(R.id.imgCloseQty)
                    val etQtySplit = dialogQty.findViewById<EditText>(R.id.tvQtySplit)

                    etQtySplit.visibility = View.VISIBLE
                    tvTitleQty.text = "SPLIT Qty"
                    etQtySplit.setText("0")
                    etQty.setText(items.quanty.toString())

                    val textWatcher = object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val split = etQty.text.toString().toIntOrNull() ?: 0

                            if (split <= items.quanty!!.toInt()){
                                etQtySplit.setText("${items.quanty!!.toInt() - split}")
                            }else{
                                Toast.makeText(this@SplitMergeActivity, "Qty tidak boleh lebih dari sekarang!!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    }

                    etQty.addTextChangedListener(textWatcher)

                    btnCloseQty.setOnClickListener {
                        dialogQty.dismiss()
                    }

                    btnSimpan.setOnClickListener{
                        if (etQty.text.toString().toInt() <= 0 || etQty.text.toString() == "" || etQty.text.toString().toInt() > items.quanty!!.toInt()){
                            Toast.makeText(this@SplitMergeActivity, "Tidak boleh sama dengan 0 atau kosong dan melebihi qty sekarang", Toast.LENGTH_SHORT).show()
                        }else{
                            lifecycleScope.launch {
                                val progressBar = ProgressDialog(this@SplitMergeActivity)
                                progressBar.setMessage("Mohon tunggu..")
                                progressBar.setCancelable(false)
                                progressBar.setCanceledOnTouchOutside(false)
                                progressBar.show()
                                val resultQtySplit = withContext(Dispatchers.IO){
                                    sendItemsSplit(items.detailId!!.toInt(), etQty.text.toString().toInt())
                                }
                                when(resultQtySplit){
                                    "Berhasil" -> {
                                        dialogQty.dismiss()
                                        showItems()
                                    }
                                    "Gagal" -> {
                                        Toast.makeText(this@SplitMergeActivity, "Gagal split qty", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        Toast.makeText(this@SplitMergeActivity, resultQtySplit, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                progressBar.dismiss()
                            }
                        }
                    }

                    dialogQty.show()
                    dialogQty.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    dialogQty.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialogQty.window?.attributes?.windowAnimations = R.style.DialogAnimation
                    dialogQty.window?.setGravity(Gravity.CENTER)
                }
            })
            listSplitAdapter.setOnItemClickCallback(object : ListSplitAdapter.OnItemClickCallback{
                override fun onItemClicked(items: SalesItem, position: Int) {
                    if (items.move == true){
                        mProduct[position].move = false
                        items.move = false
                    }else{
                        mProduct[position].move = true
                        items.move = true
                    }
                    listSplitAdapter.notifyDataSetChanged()
                }
            })
        }

        binding.fbBackSplit.setOnClickListener {
            finish()
        }

        binding.btnMerge.setOnClickListener {
            lifecycleScope.launch {
                var selectedMove = 0
                withContext(Dispatchers.IO) {
                    mProduct.forEach {
                        if (it.move == true){
                            selectedMove++
                        }
                    }
                }

                Log.e("selected move", selectedMove.toString())

                if (selectedMove == 0){
                    Toast.makeText(this@SplitMergeActivity, "Wajib pilih item!!!", Toast.LENGTH_SHORT).show()
                }else{
                    val progressBar = ProgressDialog(this@SplitMergeActivity)
                    progressBar.setMessage("Mohon tunggu..")
                    progressBar.setCancelable(false)
                    progressBar.setCanceledOnTouchOutside(false)
                    progressBar.show()
                    val resultBill = withContext(Dispatchers.IO){
                        getBillSales()
                    }
                    when(resultBill){
                        "Berhasil" -> {
                            val dialogBillSales = Dialog(this@SplitMergeActivity)
                            dialogBillSales.requestWindowFeature(Window.FEATURE_NO_TITLE)
                            dialogBillSales.setContentView(R.layout.dialog_list_sales)

                            val rvListBill = dialogBillSales.findViewById<RecyclerView>(R.id.rvListSalesBill)
                            val btnClose = dialogBillSales.findViewById<ImageView>(R.id.btnCloseDialogSales)
                            val btnNewBill = dialogBillSales.findViewById<Button>(R.id.btnNewBill)

                            btnClose.setOnClickListener {
                                dialogBillSales.dismiss()
                            }

                            btnNewBill.setOnClickListener {
                                lifecycleScope.launch {
                                    val mItemMove = mutableListOf<ItemMove>()
                                    withContext(Dispatchers.IO){
                                        mProduct.forEach {
                                            if(it.move == true){
                                                val itemMove = ItemMove()
                                                itemMove.detailID = it.detailId!!.toInt()
                                                itemMove.salesID = getSalesId!!.toInt()
                                                itemMove.user = sessionLogin.getUserLogin()
                                                mItemMove.add(itemMove)
                                            }
                                        }
                                    }
                                    val resultMove = withContext(Dispatchers.IO){
                                        sendItemsMove(Json.encodeToString(mItemMove), getTable!!.toInt())
                                    }
                                    when(resultMove){
                                        "Berhasil" -> {
                                            dialogBillSales.dismiss()
                                            Toast.makeText(this@SplitMergeActivity, "Berhasil di pindah ke $salesNo", Toast.LENGTH_SHORT).show()
                                            showItems()
                                        }
                                        "Gagal" -> {
                                            Toast.makeText(this@SplitMergeActivity, "Gagal split/merge!!", Toast.LENGTH_SHORT).show()
                                        }
                                        else -> {
                                            Toast.makeText(this@SplitMergeActivity, resultMove, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }

                            rvListBill.layoutManager = LinearLayoutManager(this@SplitMergeActivity)
                            rvListBill.setHasFixedSize(false)

                            listBillAdapter = ListBillAdapter(mBillSales)
                            rvListBill.adapter = listBillAdapter
                            listBillAdapter.setOnItemClickCallback(object : ListBillAdapter.OnItemClickCallback{
                                override fun onItemClicked(items: BillSales) {
                                    lifecycleScope.launch {
                                        val mItemMove = mutableListOf<ItemMove>()
                                        withContext(Dispatchers.IO){
                                            mProduct.forEach {
                                                if(it.move == true){
                                                    val itemMove = ItemMove()
                                                    itemMove.detailID = it.detailId!!.toInt()
                                                    itemMove.salesID = items.salesId!!.toInt()
                                                    itemMove.user = sessionLogin.getUserLogin()
                                                    mItemMove.add(itemMove)
                                                }
                                            }
                                        }
                                        val resultMove = withContext(Dispatchers.IO){
                                            sendItemsMove(Json.encodeToString(mItemMove), 0)
                                        }
                                        when(resultMove){
                                            "Berhasil" -> {
                                                dialogBillSales.dismiss()
                                                showItems()
                                            }
                                            "Gagal" -> {
                                                Toast.makeText(this@SplitMergeActivity, "Gagal split/merge!!", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                Toast.makeText(this@SplitMergeActivity, resultMove, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            })

                            dialogBillSales.show()
                            dialogBillSales.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            dialogBillSales.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialogBillSales.window?.attributes?.windowAnimations = R.style.DialogAnimation
                            dialogBillSales.window?.setGravity(Gravity.CENTER)
                        }
                        "Gagal" -> {
                            Toast.makeText(this@SplitMergeActivity, "Gagal split qty", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this@SplitMergeActivity, resultBill, Toast.LENGTH_SHORT).show()
                        }
                    }
                    progressBar.dismiss()
                }
            }
        }

        showItems()
    }

    private fun showItems(){
        lifecycleScope.launch {
            val progressBar = ProgressDialog(this@SplitMergeActivity)
            progressBar.setMessage("Mohon tunggu..")
            progressBar.setCancelable(false)
            progressBar.setCanceledOnTouchOutside(false)
            progressBar.show()
            val resultProduct = withContext(Dispatchers.IO){
                getProduct(getSalesId!!.toInt())
            }

            when(resultProduct){
                "Berhasil" -> {
                    mainViewModel.mProduct.value = mProduct
                }
                "Gagal" -> {
                    Toast.makeText(this@SplitMergeActivity, "Gagal memuat, silahkan coba lagi!!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this@SplitMergeActivity, resultProduct, Toast.LENGTH_SHORT).show()
                }

            }
            progressBar.dismiss()
        }
    }

    private fun getBillSales():String{
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "SELECT S.SalesID, S.SalesNo, T.TableCode FROM J_Sales S \n" +
                        "JOIN J_Table T ON T.TableID = S.TableID \n" +
                        "WHERE S.ClosedDateTime IS NULL AND S.VoidDateTime IS NULL"
                val preparedStatement = conn.createStatement()
                val rs = preparedStatement.executeQuery(query)
                while (rs.next()){
                    val billSales = BillSales()
                    billSales.salesId = rs.getInt("SalesID")
                    billSales.salesNo = rs.getString("SalesNO")
                    billSales.noMeja = rs.getString("TableCode")
                    mBillSales.add(billSales)
                }
                return "Berhasil"
            }catch (e: SQLException){
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun sendItemsSplit(detailId: Int, qty: Int):String{
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "EXEC USP_J_SalesItem_Split @DetailID = ?, @Qty = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, detailId)
                preparedStatement.setInt(2, qty)
                preparedStatement.execute()
                return "Berhasil"
            }catch (e: SQLException){
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun sendItemsMove(json: String, tableId: Int):String{
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "EXEC USP_J_SalesItem_MoveMobile @JSON = ?, @TableID = ?, @By = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, json)
                preparedStatement.setInt(2, tableId)
                preparedStatement.setString(3, sessionLogin.getUserLogin())
                val result = preparedStatement.executeQuery()
                while (result.next()){
                    salesNo = result.getString("SalesNo")
                }
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun getProduct(salesId: Int): String{
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "" +
                        "SELECT SI.*, M.MenuName FROM J_SalesItem SI \n" +
                        "JOIN J_Menu M ON M.MenuID = SI.MenuID \n" +
                        "WHERE SI.SalesId = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, salesId)
                val rs = st.executeQuery()
                mProduct.clear()
                while (rs.next()){
                    val product = SalesItem()
                    product.detailId = rs.getInt("DetailID")
                    product.menuId   = rs.getString("MenuId")
                    product.quanty   = rs.getInt("Qty")
                    product.harga   = rs.getInt("Price")
                    product.tax     = rs.getInt("Tax")
                    product.chg     = rs.getInt("ServiceChg")
                    product.total   = rs.getInt("NetTotal")
                    product.nama    = rs.getString("MenuName")
                    product.catatan = rs.getString("Request")
                    product.move    = false
                    mProduct.add(product)
                }
                return "Berhasil"
            }catch (e: SQLException){
                return e.toString()
            }
        }
        return "Gagal"
    }

}