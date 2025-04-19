package com.example.dayeat.ui.activity

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.example.dayeat.R
import com.example.dayeat.adapter.GvAdapter
import com.example.dayeat.adapter.OrderItemsAdapter
import com.example.dayeat.adapter.PagerAdapter
import com.example.dayeat.async.AsyncBluetoothEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrinter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.ActivityAddOrderBinding
import com.example.dayeat.db.DbContract
import com.example.dayeat.db.DbQuery
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Product
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Setting
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddOrderBinding

    private lateinit var connect: Conect
    private lateinit var listGroupMenu: MutableList<MenuGroup>
    private lateinit var dbQuery: DbQuery
    private lateinit var pagerAdapter: PagerAdapter
    private lateinit var mainViewModel: MainViewModel
    private var mProduct : MutableList<Product> = mutableListOf()
    private lateinit var sessionLogin: SessionLogin

    private lateinit var setting:Setting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connect = Conect()
        dbQuery = DbQuery.getInstance(this)
        setting = Setting(this)
        sessionLogin = SessionLogin(this)
        pagerAdapter = PagerAdapter(this@AddOrderActivity)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        mainViewModel.getOrders().observe(this, Observer { orders ->
            if (orders.size <= 0){
                binding.cardShop.visibility = View.GONE
                binding.tvItemsShop.visibility = View.GONE
            }else{
                binding.cardShop.visibility = View.VISIBLE
                binding.tvItemsShop.visibility = View.VISIBLE
                binding.tvItemsShop.text = orders.sumOf { it.quanty!!.toInt() }.toString()
            }
            mProduct.clear()
            mProduct.addAll(orders)
        })

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO){
                dbQuery.getItemsProduc()
            }
            mainViewModel.orderList.postValue(result)
        }

        binding.fbBack.setOnClickListener {
            finish()
        }

        val getSalesId = intent.getIntExtra("salesId", 0)
        val getTable = intent.getStringExtra("table")

        binding.cardShop.setOnClickListener {
            val dialogAddCategory = Dialog(this)
            dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogAddCategory.setContentView(R.layout.dialog_list_pesanan)

            val rvListOrder = dialogAddCategory.findViewById<RecyclerView>(R.id.rvMenuPesanan)
            val tvQty = dialogAddCategory.findViewById<TextView>(R.id.tvQuantyBawah)
            val tvTotal = dialogAddCategory.findViewById<TextView>(R.id.tvTotalBawah)
            val btnCancel = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnVoidListPesanan)
            val btnOrder = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnInput)
            val rlMeja = dialogAddCategory.findViewById<RelativeLayout>(R.id.rlMeja)
            val tvMeja = dialogAddCategory.findViewById<TextView>(R.id.tvMejaDialog)
            val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.btnCloseDialogPesanan)

            tvMeja.text = getTable

            rvListOrder.layoutManager = LinearLayoutManager(this)
            rvListOrder.setHasFixedSize(true)

            btnOrder.setOnClickListener{
                val selectMeja = tvMeja.text.toString()
                if (setting.getPrinter() == ""){
                    Toast.makeText(this, "Silahkan pilih printer di setting..", Toast.LENGTH_SHORT).show()
                }else{
                    if (selectMeja == "PILIH MEJA" || selectMeja == "MEJA" || selectMeja == ""){
                        Toast.makeText(this, "Pilih meja terlebih dahulu", Toast.LENGTH_SHORT).show()
                    }else{
                        val progressOrder = ProgressDialog(this)
                        progressOrder.setMessage("Sabar ya cok")
                        progressOrder.setCanceledOnTouchOutside(false)
                        progressOrder.show()
                        lifecycleScope.launch {
                            val resultJson = withContext(Dispatchers.IO){
                                dbQuery.getItemsProduc()
                            }
                            val jsonString = Json.encodeToString(resultJson)

                            val resultSend = withContext(Dispatchers.IO){
                                sendItemsPesanan(jsonString, getSalesId)
                            }

                            when (resultSend){
                                "Berhasil" -> {
                                    withContext(Dispatchers.IO){
                                        printBluetooth(selectMeja, resultJson)
                                        dbQuery.delete()
                                    }
                                    mainViewModel.orderList.postValue(dbQuery.getItemsProduc())
                                    progressOrder.dismiss()
                                    dialogAddCategory.dismiss()
                                }
                                "Gagal" -> {
                                    progressOrder.dismiss()
                                    Toast.makeText(this@AddOrderActivity, "Pesanan gagal dibuat", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    progressOrder.dismiss()
                                    Toast.makeText(this@AddOrderActivity, resultSend, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }

            btnClose.setOnClickListener {
                dialogAddCategory.dismiss()
            }

            btnCancel.setOnClickListener{
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Data mau dibersihkan loh?")
                builder.setPositiveButton("Ya"){_,_->
                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO){
                            dbQuery.delete()
                        }
                        dialogAddCategory.dismiss()
                        if (result > 0){
                            mainViewModel.orderList.postValue(dbQuery.getItemsProduc())
                        }
                    }
                }

                builder.setNegativeButton("Tidak"){p,_->
                    p.dismiss()
                }
                builder.create().show()

            }

            var orderProductAdapter : OrderItemsAdapter? = null
            Log.e("hasil product", mProduct.toString())
            orderProductAdapter = OrderItemsAdapter(mProduct, true)
            rvListOrder.adapter = orderProductAdapter
            tvQty.text = mProduct.sumOf { it.quanty!!.toInt() }.toString()
            tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.total!!.toInt() })
            orderProductAdapter.setOnItemClickCallbackQty(object : OrderItemsAdapter.OnItemClickCallbackQty{
                override fun onItemClicked(items: Product) {
                    val dialogQty = Dialog(this@AddOrderActivity)
                    dialogQty.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialogQty.setContentView(R.layout.dialog_qty)

                    val etQty = dialogQty.findViewById<EditText>(R.id.tvQtyJumlah)
                    val btnSimpan = dialogQty.findViewById<RelativeLayout>(R.id.btnSimpanQty)

                    etQty.setText(items.quanty.toString())

                    btnSimpan.setOnClickListener{
                        if (etQty.text.toString() <= "0" || etQty.text.toString() == ""){
                            Toast.makeText(this@AddOrderActivity, "Tidak boleh sama dengan 0 atau kosong", Toast.LENGTH_SHORT).show()
                        }else{
                            lifecycleScope.launch {
                                items.quanty = etQty.text.toString().toInt()
                                items.total = items.harga!! * items.quanty!!

                                val cv = ContentValues()
                                cv.put(DbContract.baseColumns.QUANTY_PRODUCT, items.quanty)
                                cv.put(DbContract.baseColumns.TOTAL_PRODUCT, items.total)
                                val resultRemove = withContext(Dispatchers.IO){
                                    dbQuery.update(items.uid.toString(), cv)
                                }
                                if (resultRemove > 0){
                                    tvQty.text = mProduct.sumOf { it.quanty!!.toInt() }.toString()
                                    tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.total!!.toInt() })
                                    mainViewModel.orderList.postValue(dbQuery.getItemsProduc())
                                    orderProductAdapter.notifyDataSetChanged()
                                    dialogQty.dismiss()
                                }
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

            orderProductAdapter.setOnItemClickButtonCallback(object : OrderItemsAdapter.OnItemClickButtonCallback{
                override fun onAddButtonClicked(item: Product) {
                    lifecycleScope.launch {

                        item.quanty = item.quanty!! + 1
                        item.total = item.harga!! * item.quanty!!

                        Log.e("hasil quanty", item.quanty.toString())
                        Log.e("hasil total", item.total.toString())

                        val cv = ContentValues()
                        cv.put(DbContract.baseColumns.QUANTY_PRODUCT, item.quanty)
                        cv.put(DbContract.baseColumns.TOTAL_PRODUCT, item.total)

                        val resultAdd = withContext(Dispatchers.IO){
                            dbQuery.update(item.uid.toString(), cv)
                        }
                        if (resultAdd > 0){
                            tvQty.text = mProduct.sumOf { it.quanty!!.toInt() }.toString()
                            tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.total!!.toInt() })
                            mainViewModel.orderList.postValue(dbQuery.getItemsProduc())
                            orderProductAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })

            orderProductAdapter.setOnItemClickRemoveCallback(object : OrderItemsAdapter.OnItemClickRemoveCallback{
                override fun onRemoveButtonlicked(items: Product, position: Int) {
                    if (items.quanty!! > 0 ){
                        lifecycleScope.launch {

                            items.quanty = items.quanty!! - 1
                            items.total = items.harga!! * items.quanty!!

                            val cv = ContentValues()
                            cv.put(DbContract.baseColumns.QUANTY_PRODUCT, items.quanty)
                            cv.put(DbContract.baseColumns.TOTAL_PRODUCT, items.total)
                            val resultRemove = withContext(Dispatchers.IO){
                                dbQuery.update(items.uid.toString(), cv)
                            }

                            if (items.quanty == 0){
                                withContext(Dispatchers.IO){
                                    dbQuery.deleteById(items.uid.toString())
                                }
                                mProduct.removeAt(position)
//                                if (mProduct.size == 0){
//                                    binding.includeLayout.root.visibility = View.VISIBLE
//                                    binding.clPesanan.visibility = View.GONE
//                                }
                                mainViewModel.orderList.postValue(dbQuery.getItemsProduc())
                                orderProductAdapter.notifyItemRemoved(position)
                            }
                            if (resultRemove > 0){
                                tvQty.text = mProduct.sumOf { it.quanty!!.toInt() }.toString()
                                tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.total!!.toInt() })
                                mainViewModel.orderList.postValue(dbQuery.getItemsProduc())
                                orderProductAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            })

            orderProductAdapter.setOnItemClickCallback(object : OrderItemsAdapter.OnItemClickCallback{
                override fun onItemClicked(items: Product) {
                    lifecycleScope.launch {
                        val dialogCatatan = Dialog(this@AddOrderActivity)
                        dialogCatatan.requestWindowFeature(Window.FEATURE_NO_TITLE)
                        dialogCatatan.setContentView(R.layout.dialog_catatan)
                        val etCatatan = dialogCatatan.findViewById<TextView>(R.id.edit_catatan)
                        val listCatatan = dialogCatatan.findViewById<GridView>(R.id.list_view)
                        val chipGroup = dialogCatatan.findViewById<ChipGroup>(R.id.chipGroup)
                        val tvJudul = dialogCatatan.findViewById<TextView>(R.id.tvJudulCatatan)
                        val tvKirim = dialogCatatan.findViewById<TextView>(R.id.tvUpdateCatatan)
                        val tvBatal = dialogCatatan.findViewById<TextView>(R.id.tvBatalCatatan)
                        val btnClose = dialogCatatan.findViewById<ImageView>(R.id.btnCloseCatatan)

                        chipGroup.visibility = View.VISIBLE
                        tvJudul.text = items.nama

                        val pref = withContext(Dispatchers.IO){
                            loadPrefMenu(items.menuId.toString())
                        }

                        items.catatan!!.split(",")!!.forEach {
                            if (it.trim() != ""){
                                val chip = Chip(this@AddOrderActivity)

                                chip.text = it.trim()

                                chip.isCloseIconVisible = true

                                chip.setOnCloseIconClickListener{
                                    chipGroup.removeView(chip)
                                }
                                chipGroup.addView(chip)
                            }
                        }

                        btnClose.setOnClickListener{
                            dialogCatatan.dismiss()
                        }

                        val adapter = ArrayAdapter(this@AddOrderActivity, android.R.layout.simple_list_item_1, pref.split(",").toList())
                        listCatatan.adapter = adapter

                        var arrayCatatan = ""
                        listCatatan.onItemClickListener =
                            AdapterView.OnItemClickListener { _, _, position, _ ->

                                val chip = Chip(this@AddOrderActivity)

                                val getItemsPosition = adapter.getItem(position)
                                chip.text = getItemsPosition

                                chip.isCloseIconVisible = true

                                chip.setOnCloseIconClickListener {
                                    chipGroup.removeView(chip)
                                }

                                chipGroup.addView(chip)
                                if (etCatatan.text.isNotEmpty()) {
                                    arrayCatatan += ", $getItemsPosition"
                                } else {
                                    arrayCatatan = getItemsPosition.toString()
                                }
                            }

                        tvKirim.setOnClickListener{
                            lifecycleScope.launch {
                                var chipArray = ""
                                withContext(Dispatchers.IO){
                                    for (i in 0 until chipGroup.childCount) {
                                        val dataChips = (chipGroup.getChildAt(i) as Chip).text.toString()
                                        chipArray += if (chipArray == ""){
                                            dataChips
                                        }else{
                                            ", $dataChips"
                                        }
                                    }
                                }
                                chipArray += if (chipArray == ""){
                                    etCatatan.text
                                }else{
                                    if (etCatatan.text.isNotEmpty()){
                                        ", ${etCatatan.text}"
                                    } else {
                                        ""
                                    }
                                }
                                val cv = ContentValues()
                                cv.put(DbContract.baseColumns.NOTE_PRODUCT, chipArray.toUpperCase())
                                val addNote = withContext(Dispatchers.IO){
                                    dbQuery.update(items.uid.toString(), cv)
                                }
                                if (addNote > 0){
                                    dialogCatatan.dismiss()
                                    items.catatan = chipArray.toUpperCase()
                                    orderProductAdapter.notifyDataSetChanged()
                                }
                            }
                        }

                        tvBatal.setOnClickListener {
                            dialogCatatan.dismiss()
                        }

                        dialogCatatan.show()
                        dialogCatatan.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        dialogCatatan.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialogCatatan.window?.attributes?.windowAnimations = R.style.DialogAnimation
                        dialogCatatan.window?.setGravity(Gravity.CENTER)
                    }
                }
            })

            dialogAddCategory.show()
            dialogAddCategory.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialogAddCategory.window?.setGravity(Gravity.CENTER)
        }

        setUpPager()
    }

    private fun sendItemsPesanan(json: String,  salesid: Int):String{
        val conn = connect.connection(this)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_CreateFromJSON @JSON = ?, @By = ?, @SalesID = ?"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, json)
                preparedStatement.setString(2, sessionLogin.getUserLogin())
                preparedStatement.setInt(3, salesid)
                preparedStatement.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun loadGroup(): String{
        listGroupMenu = mutableListOf<MenuGroup>()
        val conn = connect.connection(this)
        if (conn != null){
            try {
                val query = "SELECT CatID, CatIndex, CatName, CatFlags FROM J_Cat ORDER BY CatID"
                val st = conn.createStatement()
                val rs = st.executeQuery(query)
                listGroupMenu.clear()
                while (rs.next()){
                    val catId = rs.getInt("CatID")
                    val catName = rs.getString("CatName")
                    listGroupMenu.add(MenuGroup(catId,catName))
                }

                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun loadPrefMenu(menuCode: String):String{
        val conn = connect.connection(this)
        var result = ""
        if (conn != null){
            try {
                val query = "EXEC USP_J_Mobile_QueryStatic @Action = 'Menu.Pref', @MenuCode = ? "
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, menuCode)
                val rs = preparedStatement.executeQuery()
                while (rs.next()){
                    result = rs.getString("Prefs")
                }
                return result
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
        return result
    }

    private suspend fun printBluetooth(meja: String, listPesanan: MutableList<Product>):String {
        return withContext(Dispatchers.Main){
            val result = CompletableDeferred<String>()
            AsyncBluetoothEscPosPrint(this@AddOrderActivity,
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(asyncEscPosPrinter: AsyncEscPosPrinter, codeException: Int) {
                        result.complete("Gagal")
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                        result.complete("Berhasil")
                    }
                }
            ).execute(getAsyncEscPosPrinter(meja, listPesanan)).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinter(meja: String, listPesanan: MutableList<Product>): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(setting.getPrinterSave(), 203, 48f, 32)
            try {
                printer.addTextToPrint("" +
                        "[C]<b><font size='normal'>     ORDERS</font></b>\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        "[L]Tanggal      [R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu        [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]BY           [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
                                "[L]${it.quanty}"} +
                        "\n[L]================================\n" +
                        "[L]ITEMS        [R]: [R]${listPesanan.sumOf { it.quanty!!.toInt() }}")
            }catch (e: EscPosEncodingException){
                e.toString()
                Log.e("cek printer", e.toString())
            }
            printer
        }
    }

    private fun setUpPager(){
        lifecycleScope.launch{
            val progressDialog = ProgressDialog(this@AddOrderActivity)
            progressDialog.setMessage("Sebentar woi..")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
            val result = withContext(Dispatchers.IO) {
                loadGroup()
            }
            when (result){
                "Berhasil" -> {
                    withContext(Dispatchers.Main){
                        progressDialog.dismiss()
                        Log.e("size", listGroupMenu.size.toString())
                        pagerAdapter.setData(listGroupMenu)
                        binding.viewPagerOrder.adapter = pagerAdapter
                        TabLayoutMediator(binding.tabsLayoutOrder, binding.viewPagerOrder){tab, position ->
                            this@AddOrderActivity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            tab.text = listGroupMenu[position].catName.toString()
                        }.attach()
                    }
                }

                "Gagal" -> {
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, "Tidak terhubung ke server", Snackbar.LENGTH_SHORT).show()
                }

                else -> {
                    progressDialog.dismiss()
                    Snackbar.make(binding.root, result, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCurrentDate(format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

}