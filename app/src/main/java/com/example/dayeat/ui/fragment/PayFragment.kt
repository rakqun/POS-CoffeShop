package com.example.dayeat.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.dayeat.R
import com.example.dayeat.adapter.GvAdapter
import com.example.dayeat.adapter.KonfigurasiAdapter
import com.example.dayeat.adapter.ListCustomerAdapter
import com.example.dayeat.adapter.OrderItemsAdapter
import com.example.dayeat.adapter.PayAdapter
import com.example.dayeat.async.AsyncBluetoothEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrinter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentPayBinding
import com.example.dayeat.db.DbContract
import com.example.dayeat.db.DbQuery
import com.example.dayeat.model.Customer
import com.example.dayeat.model.Konfigurasi
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Pay
import com.example.dayeat.model.Product
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.BluetoothPermissionHelper
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Setting
import com.example.dayeat.utils.Tools
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PayFragment : Fragment() {

    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    private var payAdapter: PayAdapter = PayAdapter()
    private var listCustomerAdapter: ListCustomerAdapter = ListCustomerAdapter()
    private var mPay: MutableList<Pay> = mutableListOf()
    private var mCustomer: MutableList<Customer> = mutableListOf()
    private lateinit var listTableId: MutableList<String>
    private lateinit var listTable: MutableList<String>

    private lateinit var mainViewModel: MainViewModel
    private lateinit var sessionLogin: SessionLogin

    private var arrayMetod: ArrayList<String> = arrayListOf()
    private var arrayPromo: ArrayList<String> = arrayListOf()

    private lateinit var setting: Setting
    private lateinit var connect: Conect

    private val REQUEST_ENABLE_BT = 101
    private lateinit var bluetoothHelper: BluetoothPermissionHelper

    private var disc1: Int? = 0
    private var disc2: Int? = 0
    private var netTotal: Int? = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPayBinding.inflate(inflater, container, false)

        connect = Conect()
        setting = Setting(requireContext())
        sessionLogin = SessionLogin(requireContext())

        bluetoothHelper = BluetoothPermissionHelper(requireActivity(), REQUEST_ENABLE_BT)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvPay.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvPay.layoutManager = GridLayoutManager(context, 2)
        }

        binding.rvPay.setHasFixedSize(true)

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.queryPay(requireContext())
                mainViewModel.queryDataCustomer(requireContext())
                loadGroup()
            }
        }

        mainViewModel.getPay().observe(viewLifecycleOwner, Observer { menuGroup ->
            if (menuGroup != null){
                mPay.clear()
                mPay.addAll(menuGroup)
                Log.e("Pay", mPay.toString())
                payAdapter.setData(mPay)
                showItems()
            }
        })

        binding.swipePay.setOnRefreshListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    mainViewModel.queryPay(requireContext())
                }
                binding.swipePay.isRefreshing = false
            }
        }

        return binding.root
    }

    private fun showItems(){
        binding.rvPay.adapter = payAdapter
        payAdapter.setOnItemClickCallback(object : PayAdapter.OnItemClickCallback {
            override fun onItemClicked(pay: Pay) {
                val dialogAddCategory = Dialog(requireContext())
                dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogAddCategory.setContentView(R.layout.dialog_list_pesanan)

                val rvListOrder = dialogAddCategory.findViewById<RecyclerView>(R.id.rvMenuPesanan)
                val tvQty = dialogAddCategory.findViewById<TextView>(R.id.tvQuantyBawah)
                val tvTotal = dialogAddCategory.findViewById<TextView>(R.id.tvTotalBawah)
                val btnCancel = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnVoidListPesanan)
                val btnOrder = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnInput)
                val tvBtnOrder = dialogAddCategory.findViewById<TextView>(R.id.tvBtnOrder)
                val tvBtnVoid = dialogAddCategory.findViewById<TextView>(R.id.tvBtnVoidListPesanan)
                val tvMeja = dialogAddCategory.findViewById<TextView>(R.id.tvMejaDialog)
                val rlMeja = dialogAddCategory.findViewById<RelativeLayout>(R.id.rlMeja)
                val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.btnCloseDialogPesanan)
                val btnAddOrder = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnAddItems)
                val btnSplit = dialogAddCategory.findViewById<RelativeLayout>(R.id.btnSplitMerge)
                val btnPrint = dialogAddCategory.findViewById<ImageView>(R.id.btnPrintPesanan)
                val etPax = dialogAddCategory.findViewById<EditText>(R.id.etPaxDialogPesanan)
                val tvTaxDialogPesanan = dialogAddCategory.findViewById<TextView>(R.id.tvTaxDialogPesanan)
                val tvChgDialogPesanan = dialogAddCategory.findViewById<TextView>(R.id.tvChgDialogPesanan)
                val etCustomerDialogPesanan = dialogAddCategory.findViewById<EditText>(R.id.etCustomeperDialogPesanan)

                etCustomerDialogPesanan.setText(pay.cName.toString())

                if(pay.cName!!.isEmpty()){
                    etCustomerDialogPesanan.visibility = View.GONE
                }

                etPax.setText(pay.pax.toString())
                etPax.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        lifecycleScope.launch {
                            val progressBar = ProgressDialog(context).apply {
                                setMessage("Mohon tunggu..")
                                setCanceledOnTouchOutside(false)
                                setCancelable(false)
                                show()
                            }

                            val resultPax = withContext(Dispatchers.IO) {
                                queryUpdatePax(pay.salesId!!.toInt(), etPax.text.toString().toInt())
                            }

                            progressBar.dismiss()
                            val message = when (resultPax) {
                                "Berhasil" -> "Berhasil mengubah pax!!"
                                "Gagal" -> "Gagal mengubah pax!!"
                                else -> resultPax
                            }

                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        true
                    } else {
                        false
                    }
                }

                etCustomerDialogPesanan.setOnClickListener {
                    lifecycleScope.launch {
                        val progressBar = ProgressDialog(context)
                        progressBar.setMessage("Mohon tunggu..")
                        progressBar.setCanceledOnTouchOutside(false)
                        progressBar.setCancelable(false)
                        progressBar.show()

                        val dialogListMethod = Dialog(requireContext())
                        dialogListMethod.requestWindowFeature(Window.FEATURE_NO_TITLE)
                        dialogListMethod.setContentView(R.layout.dialog_list_method)

                        val tvTitleCustomer = dialogListMethod.findViewById<TextView>(R.id.tvTitleDialogMethod)
                        val btnCloseDialogMethod = dialogListMethod.findViewById<ImageView>(R.id.btnCloseDialogMethod)
                        val btnAddMethod = dialogListMethod.findViewById<RelativeLayout>(R.id.btnAddMethod)
                        val btnCloseMethod = dialogListMethod.findViewById<RelativeLayout>(R.id.btnCloseMethod)
                        val rvListMethod = dialogListMethod.findViewById<RecyclerView>(R.id.rvListMethod)

                        btnAddMethod.visibility = View.GONE
                        tvTitleCustomer.text = "C U S T O M E R"

                        rvListMethod.layoutManager = LinearLayoutManager(context)
                        rvListMethod.setHasFixedSize(true)

                        btnCloseDialogMethod.setOnClickListener {
                            dialogListMethod.dismiss()
                        }

                        btnCloseMethod.setOnClickListener{
                            dialogListMethod.dismiss()
                        }

                        mainViewModel.getCustomer().observe(viewLifecycleOwner, Observer { getMenuList ->
                            lifecycleScope.launch {
                                if (getMenuList != null){
                                    mCustomer.addAll(getMenuList)
                                    listCustomerAdapter.setData(mCustomer)
                                    rvListMethod.adapter = listCustomerAdapter
                                    listCustomerAdapter.setOnItemClickCallback(object : ListCustomerAdapter.OnItemClickCallback{
                                        override fun onItemClicked(customer: Customer) {
                                            etCustomerDialogPesanan.setText(customer.cName.toString())
                                            lifecycleScope.launch {
                                                progressBar.show()

                                                val resultPax = withContext(Dispatchers.IO) {
                                                    queryUpdateCustomer(pay.salesId!!.toInt(), customer.custId!!.toInt())
                                                }

                                                val message = when (resultPax) {
                                                    "Berhasil" -> "Berhasil mengubah customer!!"
                                                    "Gagal" -> "Gagal mengubah customer!!"
                                                    else -> resultPax
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                progressBar.dismiss()
                                                dialogListMethod.dismiss()
                                            }
                                        }
                                    })
                                }
                            }
                        })

                        dialogListMethod.show()
                        dialogListMethod.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        dialogListMethod.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialogListMethod.window?.attributes?.windowAnimations = R.style.DialogAnimation
                        dialogListMethod.window?.setGravity(Gravity.CENTER)
                        progressBar.dismiss()
                    }
                }

                btnPrint.visibility = View.VISIBLE
                btnAddOrder.visibility = View.VISIBLE
                btnSplit.visibility = View.VISIBLE

                rvListOrder.layoutManager = LinearLayoutManager(context)
                rvListOrder.setHasFixedSize(true)

                tvMeja.text = pay.noMeja.toString()
                tvBtnOrder.text = "PAY"
                tvBtnVoid.text  = "VOID"

                val mProduct : MutableList<Product> = mutableListOf()

                rlMeja.setOnClickListener{
                    val dialogCetak = Dialog(requireContext())
                    dialogCetak.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialogCetak.setContentView(R.layout.dialog_list_table)

                    val listQty = dialogCetak.findViewById<GridView>(R.id.list_view_meja)
                    val btnCloseMeja = dialogCetak.findViewById<ImageView>(R.id.btnCloseDialogMeja)

                    listQty.numColumns = 5
                    val adapter = GvAdapter(listTable, requireContext(), true)
                    listQty.adapter = adapter

                    listQty.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
                        lifecycleScope.launch {
                            val resultTable = withContext(Dispatchers.IO){
                                queryTransferTable(pay.salesId!!.toInt(), listTable[position])
                            }
                            when(resultTable){
                                "Berhasil" -> {
                                    tvMeja.text = listTable[position]
                                    mainViewModel.queryPay(requireContext())
                                }
                                "Gagal" -> {
                                    Toast.makeText(context, "Gagal memindahkan meja!!", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(context, resultTable, Toast.LENGTH_SHORT).show()
                                }
                            }
                            dialogCetak.dismiss()
                        }
                    }

                    btnCloseMeja.setOnClickListener {
                        dialogCetak.dismiss()
                    }

                    dialogCetak.show()
                    dialogCetak.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    dialogCetak.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialogCetak.window?.attributes?.windowAnimations = R.style.DialogAnimation
                    dialogCetak.window?.setGravity(Gravity.CENTER)
                }

                btnAddOrder.setOnClickListener{
                    dialogAddCategory.dismiss()
                    val bundle = Bundle().apply {
                        putString("table", pay.noMeja!!.toString())
                        putInt("salesId", pay.salesId!!.toInt())
                    }
                    view!!.findNavController().navigate(R.id.action_nav_pay_to_addOrderActivity, bundle)
                }

                lifecycleScope.launch {
                    withContext(Dispatchers.IO){
                        try {
                            val jsonArray = JSONArray(pay.detail)
                            for (i in 0 until jsonArray.length()){
                                val getObject = jsonArray.getJSONObject(i)
                                val product = Product()
                                product.detailId = getObject.getInt("DetailID")
                                product.menuId  = getObject.getString("MenuID")
                                product.quanty  = getObject.getInt("Qty")
                                product.harga   = getObject.getInt("Price")
                                product.tax     = getObject.getInt("Tax")
                                product.chg     = getObject.getInt("ServiceChg")
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
                    if (setting.getShowTax()){
                        tvTaxDialogPesanan.visibility = View.VISIBLE
                        tvTaxDialogPesanan.text = "Tax: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.tax!!.toInt() })}"
                    }else{
                        tvTaxDialogPesanan.visibility = View.GONE
                    }
                    if (setting.getShowChg()){
                        tvChgDialogPesanan.visibility = View.VISIBLE
                        tvChgDialogPesanan.text = "Service Chg: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.chg!!.toInt() })}"
                    }else{
                        tvChgDialogPesanan.visibility = View.GONE
                    }
                    tvTotal.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(mProduct.sumOf { it.total!!.toInt() })
                }

                btnSplit.setOnClickListener {
                    dialogAddCategory.dismiss()
                    val bundle = Bundle().apply {
                        putInt("SalesId", pay.salesId!!.toInt())
                        putInt("Table", pay.tableId!!.toInt())
                    }
                    view!!.findNavController().navigate(R.id.action_nav_pay_to_splitMergeActivity, bundle)
                }

                btnPrint.setOnClickListener {
                    val dialogPrint = AlertDialog.Builder(context)
                    dialogPrint.setTitle("Mau Print Apa?")
                    val itemsDialog = arrayOf("Taking Order", "Tempt Bill")
                    dialogPrint.setItems(itemsDialog) { dialogInterface, position ->
                        when(position){
                            0 -> {
                                lifecycleScope.launch {
//                                    if (bluetoothHelper.checkAndRequestPermissions()) {
//                                        bluetoothHelper.checkBluetoothEnabled()
//                                    }else{
                                        printBluetoothOrder(pay.noMeja.toString(), mProduct)
//                                    }

                                }
                            }
                            1 -> {
                                lifecycleScope.launch {
//                                    if (bluetoothHelper.checkAndRequestPermissions()) {
//                                        bluetoothHelper.checkBluetoothEnabled()
//                                    }else{
                                        printBluetoothTempt(pay.noMeja.toString(), pay.salesNo.toString(), mProduct, pay.promo.toString(), pay.disc!!.toInt(), etCustomerDialogPesanan.text.toString())
//                                    }
                                }
                            }
                        }
                    }
                    dialogPrint.show()
                }

                btnOrder.setOnClickListener{
                    dialogAddCategory.dismiss()
                    if (setting.getPrinter() == "" || setting.getPrinter() == "Default Printer"){
                        Toast.makeText(context, "Silahkan pilih printer di setting..", Toast.LENGTH_SHORT).show()
                    }else {
//                            Toast.makeText(context, "masuk", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            val dialogPayment = Dialog(requireContext())
                            dialogPayment.requestWindowFeature(Window.FEATURE_NO_TITLE)
                            dialogPayment.setContentView(R.layout.dialog_payment)
                            dialogPayment.setCanceledOnTouchOutside(false)

                            val qtyPayment = dialogPayment.findViewById<TextView>(R.id.tvQtyPayment)
                            val totalPayment = dialogPayment.findViewById<TextView>(R.id.tvTotalPayment)
                            val spinnerPayment = dialogPayment.findViewById<AppCompatSpinner>(R.id.spinnerPayment)
                            val spinnerPromo = dialogPayment.findViewById<AppCompatSpinner>(R.id.spinnerPromoCode)
                            val llCash = dialogPayment.findViewById<LinearLayout>(R.id.llCashPayment)
                            val rlSelectPayment = dialogPayment.findViewById<RelativeLayout>(R.id.rlSelectPayment)
                            val rlSelectPromo = dialogPayment.findViewById<RelativeLayout>(R.id.rlSelectPromoCode)
                            val etCast = dialogPayment.findViewById<EditText>(R.id.etCashPayment)
                            val tvKembalian = dialogPayment.findViewById<TextView>(R.id.tvKembalian)
                            val tvDiscPromo = dialogPayment.findViewById<TextView>(R.id.tvDiscPayment)
                            val btnCetakPayment = dialogPayment.findViewById<RelativeLayout>(R.id.btnCetakPayment)
                            val btnCetakUlang = dialogPayment.findViewById<RelativeLayout>(R.id.btnCetakUlangPayment)
                            val btnClosePayment = dialogPayment.findViewById<ImageView>(R.id.btnCloseDialogPayment)

                            val totalPay = mProduct.sumOf { it.total!!.toInt() }

                            qtyPayment.text = mProduct.sumOf { it.quanty!!.toInt() }.toString()
                            totalPayment.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(totalPay)

                            var selectMetod = "TUNAI"
                            val adapterTipe = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayMetod)
                            adapterTipe.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerPayment.adapter = adapterTipe
                            spinnerPayment.setSelection(0)
                            spinnerPayment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                    selectMetod = arrayMetod[position]
                                    when (selectMetod){
                                        "TUNAI" -> {
                                            llCash.visibility = View.VISIBLE
                                        }
                                        "QRIS" -> {
                                            llCash.visibility = View.GONE
                                        }
                                        else -> {
                                            llCash.visibility = View.GONE
                                        }
                                    }
                                }
                                override fun onNothingSelected(p: AdapterView<*>?) {}
                            }

                            var selectPromo = pay.promo.toString()
                            var discTerpasang = pay.disc!!.toInt()
                            val adapterPromo = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayPromo)
                            adapterPromo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerPromo.adapter = adapterPromo
                            spinnerPromo.setSelection(adapterPromo.getPosition(pay.promo.toString()))
                            spinnerPromo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                    lifecycleScope.launch {
                                        val progressBar = ProgressDialog(context)
                                        progressBar.setMessage("Mohon tunggu...")
                                        progressBar.setCanceledOnTouchOutside(false)
                                        progressBar.show()
                                        val resultJoinPromo = withContext(Dispatchers.IO){
                                            queryJoinPromo(pay.salesId!!.toInt(), arrayPromo[position])
                                        }
                                        when(resultJoinPromo){
                                            "Berhasil" -> {
                                                selectPromo = arrayPromo[position]
                                                discTerpasang = disc1!! + disc2!!

                                                if (selectPromo == ""){
//                                                    Toast.makeText(context, "Promo dibatalkan..", Toast.LENGTH_SHORT).show()
                                                    tvDiscPromo.visibility = View.GONE
                                                }else{
                                                    Toast.makeText(context, "Promo berhasil dipasang..", Toast.LENGTH_SHORT).show()
                                                    tvDiscPromo.visibility = View.VISIBLE
                                                    tvDiscPromo.text = "Disc : " + NumberFormat.getNumberInstance(Locale.getDefault()).format(discTerpasang)
                                                }
                                                mainViewModel.queryPay(requireContext())
                                                totalPayment.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(netTotal)
                                            }
                                            "Gagal" -> {
                                                Toast.makeText(context, "Gagal memasang promo...", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                Toast.makeText(context, resultJoinPromo, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        progressBar.dismiss()
                                    }
                                }
                                override fun onNothingSelected(p: AdapterView<*>?) {}
                            }

                            if (selectMetod == "TUNAI"){
                                llCash.visibility = View.VISIBLE
                            }

                            etCast.setText(totalPay.toString())

                            val textWatcher = object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    val cashChange = etCast.text.toString().toIntOrNull() ?: 0
                                    val resultChange = cashChange - totalPay
                                    tvKembalian.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(resultChange)
                                }
                                override fun afterTextChanged(s: Editable?) {}
                            }

                            etCast.addTextChangedListener(textWatcher)

                            var salesIdResult = 0

                            var remarks = ""

                            btnCetakUlang.setOnClickListener {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO){
                                        printBluetooth(pay.noMeja.toString(),pay.salesNo.toString(), mProduct, selectMetod, etCast.text.toString().toInt(), selectPromo, discTerpasang,true, etCustomerDialogPesanan.text.toString(), getCurrentDate("yyyy-MM-dd"), getCurrentDate("HH:mm"))
                                    }
                                }
                            }

                            btnCetakPayment.setOnClickListener{
                                if (selectMetod == "TUNAI" && tvKembalian.text.toString().replace(".","").toInt() < 0 ){
                                    Toast.makeText(context, "Kembalian tidak boleh minus!!", Toast.LENGTH_SHORT).show()
                                }else{
                                    btnCetakPayment.visibility = View.GONE
                                    val cashChange = etCast.text.toString().toIntOrNull() ?: 0
                                    val progressBarPay = ProgressDialog(context)
                                    progressBarPay.setMessage("Sabar ya cok...")
                                    progressBarPay.setCanceledOnTouchOutside(false)
                                    progressBarPay.show()
                                    lifecycleScope.launch {
                                        val resultSend = withContext(Dispatchers.IO){
                                            queryPayment(pay.salesId!!.toInt(), selectMetod, cashChange)
                                        }
                                        when(resultSend){
                                            "Berhasil" -> {
                                                withContext(Dispatchers.IO){
                                                    printBluetooth(pay.noMeja.toString(),pay.salesNo.toString(), mProduct, selectMetod, etCast.text.toString().toInt(), selectPromo, discTerpasang, false, etCustomerDialogPesanan.text.toString(), getCurrentDate("yyyy-MM-dd"), getCurrentDate("HH:mm"))
                                                }
                                                dialogAddCategory.dismiss()
//                                                dialogPayment.dismiss()
                                                mainViewModel.queryPay(requireContext())
                                                llCash.visibility = View.GONE
                                                rlSelectPayment.visibility = View.GONE
                                                rlSelectPromo.visibility = View.GONE
                                                btnCetakUlang.visibility = View.VISIBLE
                                                btnCetakPayment.visibility = View.GONE
//                                                btnCetakPayment.visibility = View.VISIBLE
                                            }
                                            "Gagal" -> {
                                                Toast.makeText(context, "Gagal membayar haha...", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                Toast.makeText(context, resultSend, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        progressBarPay.dismiss()
                                    }
                                }
                            }

                            btnClosePayment.setOnClickListener{
                                dialogPayment.dismiss()
                            }

                            dialogPayment.show()
                            dialogPayment.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            dialogPayment.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialogPayment.window?.attributes?.windowAnimations = R.style.DialogAnimation
                            dialogPayment.window?.setGravity(Gravity.CENTER)
                        }
                    }
//                    if (bluetoothHelper.checkAndRequestPermissions()) {
//                        bluetoothHelper.checkBluetoothEnabled()
//                    }else{
//
//                    }
                }

                btnClose.setOnClickListener {
                    dialogAddCategory.dismiss()
                }

                btnCancel.setOnClickListener{
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Yakin membatalkan transaksi?")
                    builder.setPositiveButton("Ya"){_,_->
                        lifecycleScope.launch {
                            val dialogQty = Dialog(requireContext())
                            dialogQty.requestWindowFeature(Window.FEATURE_NO_TITLE)
                            dialogQty.setContentView(R.layout.dialog_qty)

                            val titleQty = dialogQty.findViewById<TextView>(R.id.tvTitleQty)
                            val textBtn = dialogQty.findViewById<TextView>(R.id.tvCetak)
                            val etQty = dialogQty.findViewById<EditText>(R.id.tvQtyJumlah)
                            val btnSimpan = dialogQty.findViewById<RelativeLayout>(R.id.btnSimpanQty)
                            val btnClose = dialogQty.findViewById<ImageView>(R.id.imgCloseQty)

                            titleQty.text = "KETERANGAN VOID"
                            textBtn.text = "KIRIM"

                            etQty.inputType = InputType.TYPE_CLASS_TEXT

                            btnClose.setOnClickListener {
                                dialogQty.dismiss()
                            }

                            btnSimpan.setOnClickListener{
                                if (etQty.text.toString() == "" || etQty.text.toString().length <= 4){
                                    Toast.makeText(context!!, "Harus ada alasan dan minimal 5 kata cok...", Toast.LENGTH_SHORT).show()
                                }else{
                                    lifecycleScope.launch {
                                        val progressBar = ProgressDialog(context)
                                        progressBar.setMessage("Tunggu ya cok..")
                                        progressBar.setCanceledOnTouchOutside(false)
                                        progressBar.show()
                                        val resultVoid = withContext(Dispatchers.IO){
                                            querySalesVoid(pay.salesId!!.toInt(), etQty.text.toString())
                                        }

                                        when (resultVoid){
                                            "Berhasil" -> {
                                                mainViewModel.queryPay(requireContext())
                                                dialogQty.dismiss()
                                                dialogAddCategory.dismiss()
                                            }
                                            "Gagal" -> {
                                                Toast.makeText(context, "Gagal dihapus cok...", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                Toast.makeText(context, resultVoid, Toast.LENGTH_SHORT).show()
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
                    }

                    builder.setNegativeButton("Tidak"){p,_->
                        p.dismiss()
                    }
                    builder.create().show()

                }

                dialogAddCategory.show()
                dialogAddCategory.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
                dialogAddCategory.window?.setGravity(Gravity.CENTER)
            }
        })
    }

    private fun queryPayment(salesId: Int, method:String, forceTotal: Int):String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_SavePayment @SalesId = ?, @ForceMethod = ?, @ForceTotal = ?, @User = ?"
                val ps = conn.prepareStatement(query)
                ps.setInt(1, salesId)
                ps.setString(2, method)
                ps.setInt(3, forceTotal)
                ps.setString(4, sessionLogin.getUserLogin())
                ps.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdatePax(salesId: Int, pax:Int):String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "UPDATE J_Sales SET Pax = ? WHERE SalesID = ?"
                val ps = conn.prepareStatement(query)
                ps.setInt(1, pax)
                ps.setInt(2, salesId)
                ps.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdateCustomer(salesId: Int, customer:Int):String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "UPDATE J_Sales SET CustomerID = ? WHERE SalesID = ?\n" +
                        "EXEC USP_J_Sales_CalcTotal @SalesID = ?"
                val ps = conn.prepareStatement(query)
                ps.setInt(1, customer)
                ps.setInt(2, salesId)
                ps.setInt(3, salesId)
                ps.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryJoinPromo(salesId: Int, promoCode:String):String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_JoinPromoCode @SalesId = ?, @PromoCode = ?"
                val ps = conn.prepareStatement(query)
                ps.setInt(1, salesId)
                ps.setString(2, promoCode)
                val result = ps.executeQuery()
                while (result.next()){
                    disc1 = result.getInt("Disc1")
                    disc2 = result.getInt("Disc2")
                    netTotal = result.getInt("NetTotal")
                }
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun querySalesVoid(salesId: Int, voidReason:String):String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_Void @SalesId = ?, @VoidReason = ?, @User = ?"
                val ps = conn.prepareStatement(query)
                ps.setInt(1, salesId)
                ps.setString(2, voidReason)
                ps.setString(3, sessionLogin.getUserLogin())
                ps.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryTransferTable(salesId: Int, newTable:String):String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Sales_TransferTable @SalesId = ?, @NewTableCode = ?, @User = ?"
                val ps = conn.prepareStatement(query)
                ps.setInt(1, salesId)
                ps.setString(2, newTable)
                ps.setString(3, sessionLogin.getUserLogin())
                ps.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun loadGroup(): String{
        listTable = mutableListOf()
        listTableId = mutableListOf()
        val conn = connect.connection(context)
        if (conn != null){
            try {
                val queryTable = "EXEC USP_J_Mobile_QueryStatic @Action = 'Table'"
                val stTable = conn.createStatement()
                val rsTable = stTable.executeQuery(queryTable)
                listTable.clear()
                listTableId.clear()
                while (rsTable.next()){
                    listTable.add(rsTable.getString("TableCode"))
                    listTableId.add(rsTable.getString("TableId"))
                }

                val queryPayment = "SELECT * FROM J_Lookup WHERE TName = 'Receipt.PaymentMethod'"
                val stMethod = conn.createStatement()
                val rsMethod = stMethod.executeQuery(queryPayment)
                arrayMetod.clear()
                while (rsMethod.next()){
                    arrayMetod.add(rsMethod.getString("Code"))
                }

                val queryPromo = "SELECT * FROM J_Lookup WHERE TName = 'Sales.Promo' AND CAST(CustomD2 AS DATE) < GETDATE()"
                val stPromo = conn.createStatement()
                val rsPromo = stPromo.executeQuery(queryPromo)
                arrayPromo.clear()
                arrayPromo.add("")
                while (rsPromo.next()){
                    arrayPromo.add(rsPromo.getString("Code"))
                }
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private suspend fun printBluetooth(meja: String,salesNo: String, listPesanan: MutableList<Product>, method: String, cash: Int, promoCode: String, disc: Int, reprint: Boolean, customer: String, tanggalCetak: String, waktuCetak: String):String {
        return withContext(Dispatchers.Main){
            val result = CompletableDeferred<String>()
            AsyncBluetoothEscPosPrint(
                requireContext(),
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(asyncEscPosPrinter: AsyncEscPosPrinter, codeException: Int) {
                        result.complete("Gagal")
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                        result.complete("Berhasil")
                    }
                }
            ).execute(
                Tools.getAsyncEscPosPrinter(requireContext(),
                setting.getPrinterSave()!!, sessionLogin.getUserLogin(), meja, salesNo, listPesanan, method, cash, promoCode, disc, reprint, "============REPRINT=============", customer, tanggalCetak, waktuCetak,
                    sessionLogin.getHeader1(), sessionLogin.getHeader2(), sessionLogin.getHeader3(), sessionLogin.getHeader4(), sessionLogin.getHeader5(),
                    sessionLogin.getFooter1(), sessionLogin.getFooter2(), sessionLogin.getHeader3(), sessionLogin.getFooter4(), sessionLogin.getFooter5())).toString()
            result.await()
        }
    }

    private suspend fun printBluetoothTempt(meja: String,salesNo: String, listPesanan: MutableList<Product>, promoCode: String, disc: Int,customer: String):String {
        return withContext(Dispatchers.Main){
            val result = CompletableDeferred<String>()
            AsyncBluetoothEscPosPrint(
                requireContext(),
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(asyncEscPosPrinter: AsyncEscPosPrinter, codeException: Int) {
                        result.complete("Gagal")
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                        result.complete("Berhasil")
                    }
                }
            ).execute(getAsyncEscPosPrinterTempt(meja, salesNo, listPesanan, promoCode, disc, customer)).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinterTempt(meja: String, salesNo: String, listPesanan: MutableList<Product>, promoCode: String, disc: Int, customer: String): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(setting.getPrinterSave(), 203, 48f, 32)
            try {
                printer.addTextToPrint("\n" +
                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logo_ii, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                        "[L]\n" +
                        "[L]No BILL      [R]: [R]$salesNo\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        if (customer.isNotEmpty()){
                            "[L]Customer    [R]: [R]$customer"
                        }else{
                            ""
                        } +
                        "[L]Tanggal Cetak[R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu Cetak  [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
                        "[L]Kasir        [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        "[L]============NOT PAID============\n" +
                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
                                "[L]${it.quanty}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.harga)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.total)}"} +
                        "\n[L]================================\n" +
                        if (promoCode != ""){
                            "[L]<b>PROMO [R]$promoCode</b>\n" +
                                    "[L]<b>Disc  [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { disc!!.toInt() })}</b>\n"
                        }else{
                            ""
                        } +
                        "[L]<b>TOTAL [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}</b>\n" +
                        "[L]ITEMS    [R]${listPesanan.sumOf { it.quanty!!.toInt() }}\n" +
                        "[L]\n")
            }catch (e: EscPosEncodingException){
                e.toString()
                Log.e("cek printer", e.toString())
            }
            printer
        }
    }

    private suspend fun printBluetoothOrder(meja: String, listPesanan: MutableList<Product>):String {
        return withContext(Dispatchers.Main){
            val result = CompletableDeferred<String>()
            AsyncBluetoothEscPosPrint(
                requireContext(),
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

    private fun getCurrentDate(format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.queryPay(requireContext())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BT && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bluetoothHelper.checkBluetoothEnabled()
            Toast.makeText(context, "Bluetooth wajib dihidupkan...", Toast.LENGTH_SHORT).show()
        }
    }

//    private suspend fun getAsyncEscPosPrinter(meja: String, salesNo: String, listPesanan: MutableList<Product>, method: String, cash:Int,promoCode: String, disc: Int, reprint: Boolean, customer: String): AsyncEscPosPrinter {
//        return coroutineScope {
//            val printer = AsyncEscPosPrinter(setting.getPrinterSave(), 203, 48f, 32)
//            try {
//                printer.addTextToPrint("\n" +
//                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logo_ii, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
//                        "[L]\n" +
//                        "[L]No BILL      [R]: [R]$salesNo\n" +
//                        "[L]No Meja      [R]: [R]$meja\n" +
//                        if (customer.isNotEmpty()){
//                            "[L]Customer    [R]: [R]$customer"
//                        }else{
//                            ""
//                        } +
//                        "[L]Tanggal Cetak[R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
//                        "[L]Waktu Cetak  [R]: [R]${getCurrentDate("HH:mm:ss")}\n" +
//                        "[L]Kasir        [R]: [R]${sessionLogin.getUserLogin()}\n" +
//                        "[L]\n" +
//                        "[L]================================\n" +
//                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
//                                "[L]${it.quanty}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.harga)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.total)}"} +
//                        "\n[L]================================\n" +
//                        if (promoCode != ""){
//                            "[L]<b>PROMO [R]$promoCode</b>\n" +
//                                    "[L]<b>Disc  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(disc)}</b>\n"
//                        }else{
//                            ""
//                        } +
//                        "[L]<b>TOTAL [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}</b>\n" +
////                        if(listPesanan.sumOf { it.disc!!.toInt()} > 0){ "[L]DISC         [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak[0].disc)}\n"
////                            "[L]NET TOTAL         : [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n"
////                        } else { "" }+
//                        "[L]ITEMS    [R]${listPesanan.sumOf { it.quanty!!.toInt() }}\n" +
//                        if (method == "TUNAI"){
//                            "[L]$method  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(cash)}\n" +
//                                    "[L]KEMBALIAN[R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(cash - listPesanan.sumOf { it.total!!.toInt() })}\n"
//                        }else{
//                            "[L]$method  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}\n" +
//                                    "\n"
//                        } +
//                        "[L]\n" +
//                        if (reprint){
//                            "\n[L]============REPRINT=============\n"
//                        }else{
//                            "\n[L]================================\n"
//                        } +
//                        "[C]<b><font size='normal'>Terimakasih Telah Melakukan\n"+
//                        "[C]Pembayaran</font></b>\n" +
//                        "[L]\n")
//            }catch (e: EscPosEncodingException){
//                e.toString()
//                Log.e("cek printer", e.toString())
//            }
//            printer
//        }
//    }

}