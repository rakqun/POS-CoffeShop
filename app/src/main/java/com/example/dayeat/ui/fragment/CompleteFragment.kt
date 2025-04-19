package com.example.dayeat.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
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
import com.example.dayeat.adapter.ListCustomerAdapter
import com.example.dayeat.adapter.OrderItemsAdapter
import com.example.dayeat.adapter.PayAdapter
import com.example.dayeat.async.AsyncBluetoothEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrinter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentCompleteBinding
import com.example.dayeat.databinding.FragmentPayBinding
import com.example.dayeat.model.Category
import com.example.dayeat.model.Customer
import com.example.dayeat.model.MenuName
import com.example.dayeat.model.Pay
import com.example.dayeat.model.Payment
import com.example.dayeat.model.Product
import com.example.dayeat.model.SalesExport
import com.example.dayeat.model.SalesExport2
import com.example.dayeat.model.SalesItemExport
import com.example.dayeat.model.TimeSales
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.BluetoothPermissionHelper
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Setting
import com.example.dayeat.utils.Tools
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.reflect.full.memberProperties

class CompleteFragment : Fragment() {

    private var _binding: FragmentCompleteBinding? = null
    private val binding get() = _binding!!

    private var payAdapter: PayAdapter = PayAdapter()
    private var mPay: MutableList<Pay> = mutableListOf()

    private val REQUEST_ENABLE_BT = 101
    private lateinit var bluetoothHelper: BluetoothPermissionHelper

    private lateinit var mainViewModel: MainViewModel
    private lateinit var sessionLogin: SessionLogin

    private lateinit var setting: Setting
    private lateinit var connect: Conect

    private val mSalesExport: MutableList<SalesExport> = mutableListOf()
    private val mSalesExport2: MutableList<SalesExport2> = mutableListOf()
    private val mPayment: MutableList<Payment> = mutableListOf()
    private val mCategory: MutableList<Category> = mutableListOf()
    private val mTimeSales: MutableList<TimeSales> = mutableListOf()
    private val mMenuName: MutableList<MenuName> = mutableListOf()
    private val mSalesItemExport: MutableList<SalesItemExport> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCompleteBinding.inflate(inflater, container, false)

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
                mainViewModel.queryComplet(requireContext())
            }
        }

        mainViewModel.getPay().observe(viewLifecycleOwner, Observer { menuGroup ->
            if (menuGroup != null){
                mPay.clear()
                mPay.addAll(menuGroup)
                binding.cvTotalComplet.visibility = View.VISIBLE
                if(menuGroup.isEmpty()){
                    binding.cvTotalComplet.visibility = View.GONE
                }
                binding.tvTotalComplet.text = "Total   " + "${mPay.sumOf { it.qty!!.toInt() }}   " + "${NumberFormat.getNumberInstance(Locale.getDefault()).format(mPay.sumOf { it.total!!.toInt() })}"
                Log.e("Complete", mPay.toString())
                payAdapter.setData(mPay)
                showItems()
            }
        })

        binding.swipePay.setOnRefreshListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    mainViewModel.queryComplet(requireContext())
                }
                binding.swipePay.isRefreshing = false
            }
        }

        binding.cvTotalComplet.setOnClickListener {
            val build = AlertDialog.Builder(context)
            build.setMessage("Mau Export?")
            build.setPositiveButton("Ya"){_,_->
                val progressBar = ProgressDialog(context)
                progressBar.setMessage("Mengambil Data...")
                progressBar.setCanceledOnTouchOutside(false)
                progressBar.setCancelable(false)
                progressBar.show()
                lifecycleScope.launch {
                    val resultData = withContext(Dispatchers.IO){
                        getExportSales()
                    }
                    when(resultData) {
                        "Berhasil" -> {
                            progressBar.setMessage("Sedang export...")
                            val dataMap = mapOf(
                                "Complet & Void Order " to mSalesExport,
                                "Not Complet & Void Order" to mSalesExport2,
                                "Detail Menu In Bill" to mSalesItemExport,
                                "Method" to mPayment,
                                "Category Sales" to mCategory,
                                "Menu Sales" to mMenuName,
                                "Time Sales" to mTimeSales,
                            )

                            val resultExport = withContext(Dispatchers.IO){
                                createXlsx(dataMap)
                            }

                            when(resultExport){
                                "Berhasil" -> {
                                    progressBar.dismiss()
                                    Toast.makeText(context, "Berhasil, silahkan cek di Document/Report Pos/Complete Report...", Toast.LENGTH_SHORT).show()
                                }
                                "Gagal" -> {
                                    progressBar.dismiss()
                                    Toast.makeText(context, "Gagal Export data...", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    progressBar.dismiss()
                                    Toast.makeText(context, resultExport, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        "Gagal" -> {
                            progressBar.dismiss()
                            Toast.makeText(context, "Gagal Mengambil data, periksa kembali server anda...", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            progressBar.dismiss()
                            Toast.makeText(context, resultData, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            build.setNegativeButton("Ga"){p,_->
                p.dismiss()
            }
            build.create().show()
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
                etPax.setText(pay.pax.toString())

                btnCancel.visibility = View.VISIBLE

                if (sessionLogin.getMobileRole() == "KASIR"){
                    btnCancel.visibility = View.GONE
                }

                btnPrint.visibility    = View.VISIBLE
                btnAddOrder.visibility = View.GONE
                btnSplit.visibility = View.GONE
                btnOrder.visibility = View.GONE

                rvListOrder.layoutManager = LinearLayoutManager(context)
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

                btnPrint.setOnClickListener {
                    lifecycleScope.launch {
//                        if (bluetoothHelper.checkAndRequestPermissions()) {
//                            bluetoothHelper.checkBluetoothEnabled()
//                        }else{
                            printBluetooth(pay.noMeja.toString(),pay.salesNo.toString(),mProduct, pay.method.toString(), pay.amountPaid!!.toInt(), pay.promo.toString(), pay.disc!!.toInt(), true, pay.cName.toString(), pay.completDate.toString(), pay.completTime.toString())
//                        }
                    }
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
                                                mainViewModel.queryComplet(context!!)
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
            ).execute(Tools.getAsyncEscPosPrinter(requireContext(),
                setting.getPrinterSave()!!, sessionLogin.getUserLogin(), meja, salesNo, listPesanan, method, cash, promoCode, disc, reprint, "=======REPRINT COMPLETE=========", customer, tanggalCetak, waktuCetak,
                sessionLogin.getHeader1(), sessionLogin.getHeader2(), sessionLogin.getHeader3(), sessionLogin.getHeader4(), sessionLogin.getHeader5(),
                sessionLogin.getFooter1(), sessionLogin.getFooter2(), sessionLogin.getHeader3(), sessionLogin.getFooter4(), sessionLogin.getFooter5())).toString()
            result.await()
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    override fun onResume() {
//        super.onResume()
//        mainViewModel.queryPay(requireContext())
//    }

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

    private fun getExportSales():String{
        val conn = connect.connection(context)
        if (conn!= null){
            try {
                val query = "EXEC USP_Sales_ReportComplet"
                val st = conn.createStatement()
                var nr = st.execute(query)
                var resultSetIndex = 0
                mPayment.clear()
                mCategory.clear()
                mTimeSales.clear()
                mMenuName.clear()
                do {
                    if (nr) {
                        val resultSet = st.resultSet
                        resultSetIndex++
                        when (resultSetIndex) {
                            1 -> {
                                while (resultSet.next()){
                                    val salesExport = SalesExport().apply {
                                        a_billNo = resultSet.getString("No Bill")
                                        b_noMeja = resultSet.getString("No Meja")
                                        c_customer = resultSet.getString("Customer")
                                        this.d_startDate = resultSet.getString("Start Order")
                                        e_startBy = resultSet.getString("Start By")
                                        this.f_closeDate = resultSet.getString("Close Date")
                                        g_closeBy = resultSet.getString("Close By")
                                        f_qty = resultSet.getInt("Qty")
                                        h_grossTotal = resultSet.getInt("GrossTotal")
                                        i_dpp = resultSet.getInt("DPP")
                                        j_disc = resultSet.getInt("Disc")
                                        k_serviceChg = resultSet.getInt("ServiceChg")
                                        l_tax = resultSet.getInt("PB1")
                                        m_netTotal = resultSet.getInt("NetTotal")
                                        n_method = resultSet.getString("Method")
                                        o_amountPaid = resultSet.getInt("Amount Paid")
                                        p_kembalian = resultSet.getInt("Kembalian")
                                        q_voidBy = resultSet.getString("Void By")
                                        r_voidDate = resultSet.getString("Void Date")
                                        s_voidReason = resultSet.getString("Void Reason")
                                    }
                                    mSalesExport.add(salesExport)
                                }
                            }
                            2 -> {
                                while (resultSet.next()){
                                    val salesExport = SalesExport2().apply {
                                        a_billNo = resultSet.getString("No Bill")
                                        b_noMeja = resultSet.getString("No Meja")
                                        c_customer = resultSet.getString("Customer")
                                        this.d_startDate = resultSet.getString("Start Order")
                                        e_startBy = resultSet.getString("Start By")
                                        this.f_closeDate = resultSet.getString("Close Date")
                                        g_closeBy = resultSet.getString("Close By")
                                        f_qty = resultSet.getInt("Qty")
                                        h_grossTotal = resultSet.getInt("GrossTotal")
                                        i_dpp = resultSet.getInt("DPP")
                                        j_disc = resultSet.getInt("Disc")
                                        k_serviceChg = resultSet.getInt("ServiceChg")
                                        l_tax = resultSet.getInt("PB1")
                                        m_netTotal = resultSet.getInt("NetTotal")
                                        n_method = resultSet.getString("Method")
                                        o_amountPaid = resultSet.getInt("Amount Paid")
                                        p_kembalian = resultSet.getInt("Kembalian")
                                        q_voidBy = resultSet.getString("Void By")
                                        r_voidDate = resultSet.getString("Void Date")
                                        s_voidReason = resultSet.getString("Void Reason")

                                    }
                                    mSalesExport2.add(salesExport)
                                }
                            }
                            3 -> {
                                while (resultSet.next()){
                                    val salesItemExport = SalesItemExport().apply {
                                        a_SalesNo = resultSet.getString("SalesNo")
                                        b_MenuName = resultSet.getString("MenuName")
                                        c_Request = resultSet.getString("Request")
                                        d_Qty = resultSet.getInt("Qty")
                                        e_Total = resultSet.getInt("NetTotal")
                                    }
                                    mSalesItemExport.add(salesItemExport)
                                }
                            }
                            4 -> {
                                while (resultSet.next()){
                                    val payment = Payment()
                                    payment.jenis = resultSet.getString("Method")
                                    payment.total = resultSet.getInt("Total")
                                    mPayment.add(payment)
                                }
                            }
                            5 -> {
                                while (resultSet.next()){
                                    val category = Category()
                                    category.category = resultSet.getString("Category")
                                    category.total = resultSet.getInt("Total")
                                    mCategory.add(category)
                                }
                            }
                            6 -> {
                                while (resultSet.next()){
                                    val menu = MenuName()
                                    menu.menuName = resultSet.getString("MenuName")
                                    menu.total = resultSet.getInt("Total")
                                    mMenuName.add(menu)
                                }
                            }
                            7 -> {
                                while (resultSet.next()){
                                    val timeSales = TimeSales()
                                    timeSales.time = resultSet.getString("Time")
                                    timeSales.total = resultSet.getInt("NetTotal")
                                    mTimeSales.add(timeSales)
                                }
                            }
                            else -> {

                            }
                        }
                    }
                    nr = st.moreResults
                } while (nr)
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun createXlsx(dataMap: Map<String, List<Any>>):String {
        try {
            val strDate: String =
                SimpleDateFormat("dd-MM-yyyy HH-mm-ss", Locale.getDefault()).format(Date())
            val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Report POS")
            if (!root.exists()) root.mkdirs()
            val path = File(root, "Complete Report $strDate.xlsx")
            val workbook = XSSFWorkbook()
            val outputStream = FileOutputStream(path)
            val headerStyle = workbook.createCellStyle()
            headerStyle.alignment = HorizontalAlignment.CENTER
            headerStyle.fillForegroundColor = IndexedColors.BLUE_GREY.getIndex()
            headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            headerStyle.borderTop = BorderStyle.MEDIUM
            headerStyle.borderBottom = BorderStyle.MEDIUM
            headerStyle.borderRight = BorderStyle.MEDIUM
            headerStyle.borderLeft = BorderStyle.MEDIUM
            val font = workbook.createFont()
            font.fontHeightInPoints = 12.toShort()
            font.color = IndexedColors.WHITE.getIndex()
            font.bold = true
            headerStyle.setFont(font)
            dataMap.forEach { (sheetName, data) ->
                val sheet = workbook.createSheet(sheetName)

                if (data.isNotEmpty()) {
                    // Ambil properti dari class menggunakan memberProperties
                    val properties = data.first()::class.memberProperties

                    // Buat header otomatis
                    val headerRow: Row = sheet.createRow(0)
                    properties.forEachIndexed { index, property ->
                        headerRow.createCell(index).apply {
                            setCellValue(property.name)
                            cellStyle = headerStyle
                        }
                    }

                    // Tambahkan data
                    data.forEachIndexed { rowIndex, item ->
                        val row: Row = sheet.createRow(rowIndex + 1)
                        properties.forEachIndexed { colIndex, property ->
                            val value = property.getter.call(item)?.toString() ?: "" // Gunakan getter untuk membaca nilai
                            row.createCell(colIndex).setCellValue(value)
                        }
                    }
                }

            }
            workbook.write(outputStream)
            outputStream.close()
            return "Berhasil"
        } catch (e: IOException) {
            e.printStackTrace()
            return e.toString()
        }
    }
}