package com.example.dayeat.ui.home

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.MyMarkerView
import com.example.dayeat.adapter.PaymentAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentHomeBinding
import com.example.dayeat.model.Category
import com.example.dayeat.model.MenuName
import com.example.dayeat.model.Payment
import com.example.dayeat.model.SalesExport
import com.example.dayeat.model.SalesExport2
import com.example.dayeat.model.SalesItemExport
import com.example.dayeat.model.TimeSales
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainViewModel: HomeViewModel
    private lateinit var conect: Conect

    private val paymentAdapter: PaymentAdapter = PaymentAdapter()
    private val mSalesExport: MutableList<SalesExport> = mutableListOf()
    private val mSalesExport2: MutableList<SalesExport2> = mutableListOf()
    private val mPayment: MutableList<Payment> = mutableListOf()
    private val mCategory: MutableList<Category> = mutableListOf()
    private val mTimeSales: MutableList<TimeSales> = mutableListOf()
    private val mMenuName: MutableList<MenuName> = mutableListOf()
    private val mSalesItemExport: MutableList<SalesItemExport> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mainViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        conect = Conect()

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        lifecycleScope.launch {
            mainViewModel.queryGetPenjualan(requireContext(), getCurrentDate(), getCurrentDate())
            mainViewModel.queryGetPayment(requireContext(),getCurrentDate(), getCurrentDate())
            mainViewModel.queryGetCategory(requireContext(),getCurrentDate(), getCurrentDate())
            mainViewModel.queryGetTimeSales(requireContext(),getCurrentDate(), getCurrentDate())
            mainViewModel.queryGetMenuName(requireContext(),getCurrentDate(), getCurrentDate())
        }

        binding.rvPaymentPenjualan.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPaymentPenjualan.setHasFixedSize(false)

        binding.tvStartDateSalesHome.text = getCurrentDate()
        binding.tvEndDateSalesHome.text = getCurrentDate()

        binding.tvStartDateSalesHome.setOnClickListener{
            showDatePicker(true)
        }

        binding.tvEndDateSalesHome.setOnClickListener{
            showDatePicker(false)
        }

        binding.btnExport.setOnClickListener {
            val progressBar = ProgressDialog(context)
            progressBar.setMessage("Mengambil Data...")
            progressBar.setCanceledOnTouchOutside(false)
            progressBar.setCancelable(false)
            progressBar.show()
            lifecycleScope.launch {
                val resultData = withContext(Dispatchers.IO){
                    getExportSales(binding.tvStartDateSalesHome.text.toString(), binding.tvEndDateSalesHome.text.toString())
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
                            createXlsx(dataMap, "${binding.tvStartDateSalesHome.text} To ${binding.tvEndDateSalesHome.text}")
                        }

                        when(resultExport){
                            "Berhasil" -> {
                                progressBar.dismiss()
                                Toast.makeText(context, "Berhasil, silahkan cek di Document/Report Pos/Sales Pita...", Toast.LENGTH_SHORT).show()
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

        mainViewModel.gePenjualan.observe(viewLifecycleOwner, Observer {
            it?.forEach { penjualan ->
                Log.e("hasil penjualan", penjualan.toString())
                binding.tvQtyPenjualan.text = "Items Terjual: " + penjualan.notQty.toString()
                binding.tvTotalPenjualan.text = "Rp " + NumberFormat.getNumberInstance(Locale.getDefault()).format(penjualan.notTotal)
            }
        })

        mainViewModel.getPayment.observe(viewLifecycleOwner, Observer {
            if (it != null){
                setColumMpChartPayment(it)
            }
        })

        mainViewModel.getCategory.observe(viewLifecycleOwner, Observer {
            if (it != null){
                setColumMpChartCategory(it)
            }
        })

        mainViewModel.getTimeSale.observe(viewLifecycleOwner, Observer {
            if (it != null){
                setColumMpChartTime(it)
            }
        })

        mainViewModel.getMenuName.observe(viewLifecycleOwner, Observer {
            if (it != null){
                setColumMpChartMenuName(it)
            }
        })
        return root
    }

    private fun setColumMpChartCategory(entry: MutableList<Category>) {
        lifecycleScope.launch {
            val chart: BarChart = binding.barChartCategory
            chart.axisRight.setDrawLabels(false)

            val entries = ArrayList<BarEntry>()
            val xValues = ArrayList<String>()

            // Menambahkan data ke entries dan xValues
            entry.forEachIndexed { index, category ->
                val cleanCategory = category.category.toString().replace("'", "").replace("&", "")
                xValues.add(cleanCategory)
                entries.add(BarEntry(index.toFloat(), category.total?.toFloat() ?: 0f))
            }

            withContext(Dispatchers.Main) {
                if (entries.isNotEmpty() && xValues.isNotEmpty()) {
                    // Konfigurasi Y Axis
                    val yAxis = chart.axisLeft
                    yAxis.axisMinimum = 0f
                    yAxis.setDrawAxisLine(false) // Menghilangkan garis Y axis
                    yAxis.setDrawGridLines(false) // Menghilangkan garis grid di Y axis
                    yAxis.setDrawLabels(true) // Jika ingin label tetap ditampilkan, atau false jika ingin dihilangkan

                    // Menghilangkan garis X Axis
                    val xAxis = chart.xAxis
                    xAxis.setDrawAxisLine(false)
                    xAxis.setDrawGridLines(false) // Menghilangkan garis grid di X axis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
                    xAxis.granularity = 1f
                    xAxis.isGranularityEnabled = true

                    // Menghilangkan garis grid background
                    chart.setDrawGridBackground(false)
                    chart.setDrawBorders(false) // Jika ingin tanpa border di sekitar chart

                    // Konfigurasi Legend
                    val legend = chart.legend
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    legend.form = LegendForm.SQUARE
                    legend.formSize = 9f
                    legend.textSize = 11f
                    legend.xEntrySpace = 4f

                    // Menyiapkan DataSet dan Data untuk Chart
                    val dataSet = BarDataSet(entries, "Categories")
                    dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)

                    val barData = BarData(dataSet)
                    chart.data = barData

                    // Menghilangkan deskripsi chart
                    chart.description.isEnabled = false

                    val markerView = MyMarkerView(chart.context, R.layout.marker_view)
                    markerView.setNameCategory(xValues)
                    chart.marker = markerView

                    // Refresh Chart
                    chart.invalidate()
                } else {
                    Log.e("Hasil bar chart", "Data entries atau xValues kosong")
                }
            }
        }
    }

    private fun setColumMpChartPayment(entry: MutableList<Payment>) {
        lifecycleScope.launch {
            val chart: BarChart = binding.barChartPayment
            chart.axisRight.setDrawLabels(false)

            val entries = ArrayList<BarEntry>()
            val xValues = ArrayList<String>()

            // Menambahkan data ke entries dan xValues
            entry.forEachIndexed { index, category ->
                val cleanCategory = category.jenis.toString().replace("'", "").replace("&", "")
                xValues.add(cleanCategory)
                entries.add(BarEntry(index.toFloat(), category.total?.toFloat() ?: 0f))
            }

            withContext(Dispatchers.Main) {
                if (entries.isNotEmpty() && xValues.isNotEmpty()) {
                    // Konfigurasi Y Axis
                    val yAxis = chart.axisLeft
                    yAxis.axisMinimum = 0f
                    yAxis.setDrawAxisLine(false) // Menghilangkan garis Y axis
                    yAxis.setDrawGridLines(false) // Menghilangkan garis grid di Y axis
                    yAxis.setDrawLabels(true) // Jika ingin label tetap ditampilkan, atau false jika ingin dihilangkan

                    // Menghilangkan garis X Axis
                    val xAxis = chart.xAxis
                    xAxis.setDrawAxisLine(false)
                    xAxis.setDrawGridLines(false) // Menghilangkan garis grid di X axis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
                    xAxis.granularity = 1f
                    xAxis.isGranularityEnabled = true

                    // Menghilangkan garis grid background
                    chart.setDrawGridBackground(false)
                    chart.setDrawBorders(false) // Jika ingin tanpa border di sekitar chart

                    // Konfigurasi Legend
                    val legend = chart.legend
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    legend.form = LegendForm.SQUARE
                    legend.formSize = 9f
                    legend.textSize = 11f
                    legend.xEntrySpace = 4f

                    // Menyiapkan DataSet dan Data untuk Chart
                    val dataSet = BarDataSet(entries, "Payment")
                    dataSet.setColors(*ColorTemplate.COLORFUL_COLORS)

                    val barData = BarData(dataSet)
                    chart.data = barData

                    // Menghilangkan deskripsi chart
                    chart.description.isEnabled = false

                    val markerView = MyMarkerView(chart.context, R.layout.marker_view)
                    markerView.setNameCategory(xValues)
                    chart.marker = markerView

                    // Refresh Chart
                    chart.invalidate()
                } else {
                    Log.e("Hasil bar chart", "Data entries atau xValues kosong")
                }
            }
        }
    }

    private fun setColumMpChartTime(entry: MutableList<TimeSales>) {
        lifecycleScope.launch {
            val chart: BarChart = binding.barChartDate
            chart.axisRight.setDrawLabels(false)

            val entries = ArrayList<BarEntry>()
            val xValues = ArrayList<String>()

            // Menambahkan data ke entries dan xValues
            entry.forEachIndexed { index, category ->
                val cleanCategory = category.time.toString().replace("'", "").replace("&", "")
                xValues.add(cleanCategory)
                entries.add(BarEntry(index.toFloat(), category.total?.toFloat() ?: 0f))
            }

            withContext(Dispatchers.Main) {
                if (entries.isNotEmpty() && xValues.isNotEmpty()) {
                    // Konfigurasi Y Axis
                    val yAxis = chart.axisLeft
                    yAxis.axisMinimum = 0f
                    yAxis.setDrawAxisLine(false) // Menghilangkan garis Y axis
                    yAxis.setDrawGridLines(false) // Menghilangkan garis grid di Y axis
                    yAxis.setDrawLabels(true) // Jika ingin label tetap ditampilkan, atau false jika ingin dihilangkan

                    // Menghilangkan garis X Axis
                    val xAxis = chart.xAxis
                    xAxis.setDrawAxisLine(false)
                    xAxis.setDrawGridLines(false) // Menghilangkan garis grid di X axis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
                    xAxis.granularity = 1f
                    xAxis.isGranularityEnabled = true

                    // Menghilangkan garis grid background
                    chart.setDrawGridBackground(false)
                    chart.setDrawBorders(false) // Jika ingin tanpa border di sekitar chart

                    // Konfigurasi Legend
                    val legend = chart.legend
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    legend.form = LegendForm.SQUARE
                    legend.formSize = 9f
                    legend.textSize = 11f
                    legend.xEntrySpace = 4f

                    // Menyiapkan DataSet dan Data untuk Chart
                    val dataSet = BarDataSet(entries, "TimeSales")
                    dataSet.setColors(*ColorTemplate.LIBERTY_COLORS)

                    val barData = BarData(dataSet)
                    chart.data = barData

                    // Menghilangkan deskripsi chart
                    chart.description.isEnabled = false

                    val markerView = MyMarkerView(chart.context, R.layout.marker_view)
                    markerView.setNameCategory(xValues)
                    chart.marker = markerView

                    // Refresh Chart
                    chart.invalidate()
                } else {
                    Log.e("Hasil bar chart", "Data entries atau xValues kosong")
                }
            }
        }
    }

    private fun setColumMpChartMenuName(entry: MutableList<MenuName>) {
        lifecycleScope.launch {
            val chart: BarChart = binding.barChartMenu
            chart.axisRight.setDrawLabels(false)

            val entries = ArrayList<BarEntry>()
            val xValues = ArrayList<String>()

            // Menambahkan data ke entries dan xValues
            entry.forEachIndexed { index, category ->
                val cleanCategory = category.menuName.toString().replace("'", "").replace("&", "")
                xValues.add(cleanCategory)
                entries.add(BarEntry(index.toFloat(), category.total?.toFloat() ?: 0f))
            }

            withContext(Dispatchers.Main) {
                if (entries.isNotEmpty() && xValues.isNotEmpty()) {
                    // Konfigurasi Y Axis
                    val yAxis = chart.axisLeft
                    yAxis.axisMinimum = 0f
                    yAxis.setDrawAxisLine(false) // Menghilangkan garis Y axis
                    yAxis.setDrawGridLines(false) // Menghilangkan garis grid di Y axis
                    yAxis.setDrawLabels(true) // Jika ingin label tetap ditampilkan, atau false jika ingin dihilangkan

                    // Menghilangkan garis X Axis
                    val xAxis = chart.xAxis
                    xAxis.setDrawAxisLine(false)
                    xAxis.setDrawGridLines(false) // Menghilangkan garis grid di X axis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
                    xAxis.granularity = 1f
                    xAxis.isGranularityEnabled = true

                    // Menghilangkan garis grid background
                    chart.setDrawGridBackground(false)
                    chart.setDrawBorders(false) // Jika ingin tanpa border di sekitar chart

                    // Konfigurasi Legend
                    val legend = chart.legend
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    legend.form = LegendForm.SQUARE
                    legend.formSize = 9f
                    legend.textSize = 11f
                    legend.xEntrySpace = 4f

                    // Menyiapkan DataSet dan Data untuk Chart
                    val dataSet = BarDataSet(entries, "Menu Name")
                    dataSet.setColors(*ColorTemplate.JOYFUL_COLORS)

                    val barData = BarData(dataSet)
                    chart.data = barData

                    // Menghilangkan deskripsi chart
                    chart.description.isEnabled = false

                    val markerView = MyMarkerView(chart.context, R.layout.marker_view)
                    markerView.setNameCategory(xValues)
                    chart.marker = markerView

                    // Refresh Chart
                    chart.invalidate()
                } else {
                    Log.e("Hasil bar chart", "Data entries atau xValues kosong")
                }
            }
        }
    }

    private fun showDatePicker(start: Boolean) {
        val datePickerDialog = DatePickerDialog(
            requireContext(), {_, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)
                if (start){
                    binding.tvStartDateSalesHome.text = formattedDate
                }else{
                    binding.tvEndDateSalesHome.text = formattedDate
                }
                if (binding.tvStartDateSalesHome.text.toString().isNotEmpty() && binding.tvEndDateSalesHome.text.toString().isNotEmpty()){
                    lifecycleScope.launch {
                        mainViewModel.queryGetPenjualan(requireContext(),binding.tvStartDateSalesHome.text.toString(), binding.tvEndDateSalesHome.text.toString())
                        mainViewModel.queryGetPayment(requireContext(),binding.tvStartDateSalesHome.text.toString(), binding.tvEndDateSalesHome.text.toString())
                        mainViewModel.queryGetCategory(requireContext(),binding.tvStartDateSalesHome.text.toString(), binding.tvEndDateSalesHome.text.toString())
                        mainViewModel.queryGetTimeSales(requireContext(),binding.tvStartDateSalesHome.text.toString(), binding.tvEndDateSalesHome.text.toString())
                        mainViewModel.queryGetMenuName(requireContext(),binding.tvStartDateSalesHome.text.toString(), binding.tvEndDateSalesHome.text.toString())
                    }
                }
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun getExportSales(startDate: String, endDate: String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            try {
                val query = "EXEC USP_Sales_DashBoard ?, ?"
                val st = conn.prepareStatement(query)
                st.setString(1, startDate)
                st.setString(2, endDate)
                var nr = st.execute()
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

    private fun createXlsx(dataMap: Map<String, List<Any>>, dateExport: String):String {
        try {
            val strDate: String =
                SimpleDateFormat("dd-MM-yyyy HH-mm-ss", Locale.getDefault()).format(Date())
            val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Report POS")
            if (!root.exists()) root.mkdirs()
            val path = File(root, "Sales PITA ($dateExport) $strDate.xlsx")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //    private fun setPiePayment(pieEntry: MutableList<Payment>){
//        binding.pieChartPayment.setUsePercentValues(false)
//        binding.pieChartPayment.description.isEnabled = false
//        binding.pieChartPayment.setExtraOffsets(5f, 10f, 5f, 5f)
//
//        binding.pieChartPayment.dragDecelerationFrictionCoef = 0.95f
//
////        val s = SpannableString("Method")
////        s.setSpan(RelativeSizeSpan(1.7f), 0, 14, 0)
////        s.setSpan(ForegroundColorSpan(Color.RED), s.length - 14, s.length, 0)
////
////        binding.pieChartPayment.centerText = s
//        binding.pieChartPayment.isDrawHoleEnabled = true
//        binding.pieChartPayment.setHoleColor(Color.WHITE)
//        binding.pieChartPayment.setTransparentCircleColor(Color.WHITE)
//        binding.pieChartPayment.setTransparentCircleAlpha(110)
//
//        binding.pieChartPayment.holeRadius = 58f
//        binding.pieChartPayment.transparentCircleRadius = 61f
//
//        binding.pieChartPayment.setDrawCenterText(true)
//
//        binding.pieChartPayment.rotationAngle = 0f
//
//        binding.pieChartPayment.isRotationEnabled = true
//        binding.pieChartPayment.isHighlightPerTapEnabled = true
//
//        binding.pieChartPayment.animateY(1400, Easing.EaseInOutQuad)
//
//        binding.pieChartPayment.legend.isEnabled = false
//        binding.pieChartPayment.setEntryLabelColor(Color.BLACK)
//        binding.pieChartPayment.setEntryLabelTextSize(12f)
//
//        val entries: ArrayList<PieEntry> = ArrayList()
//        pieEntry.forEach {
//            entries.add(PieEntry(it.total!!.toFloat(), it.jenis.toString()))
//        }
//
//        val dataSet = PieDataSet(entries, "Method")
//
//        dataSet.setDrawIcons(false)
//
//        dataSet.sliceSpace = 3f
//        dataSet.iconsOffset = MPPointF(0f, 40f)
//        dataSet.selectionShift = 5f
//
//        val colors = ArrayList<Int>()
//
//        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
//
//        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
//
//        for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)
//
//        for (c in ColorTemplate.LIBERTY_COLORS) colors.add(c)
//
//        for (c in ColorTemplate.PASTEL_COLORS) colors.add(c)
//
//        colors.add(ColorTemplate.getHoloBlue())
//
//        dataSet.colors = colors
//
//        val data = PieData(dataSet)
//        data.setValueFormatter(PercentFormatter())
//        data.setValueTextSize(15f)
//        data.setValueTypeface(Typeface.DEFAULT_BOLD)
//        data.setValueTextColor(Color.BLACK)
//        binding.pieChartPayment.data = data
//
//        binding.pieChartPayment.highlightValues(null)
//
//        binding.pieChartPayment.invalidate()
//
//        binding.pieChartPayment.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
//            override fun onValueSelected(e: Entry?, h: Highlight?) {
//                if (e is PieEntry) {
//                    val info = "Method: ${e.label}\nTotal: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(e.value.toInt())}"
//                    Toast.makeText(context, info, Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onNothingSelected() {}
//        })
//    }
}