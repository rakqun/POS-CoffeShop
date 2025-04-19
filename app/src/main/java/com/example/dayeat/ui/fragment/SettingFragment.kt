package com.example.dayeat.ui.fragment

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.dayeat.R
import com.example.dayeat.adapter.KonfigurasiAdapter
import com.example.dayeat.adapter.MenuAdapter
import com.example.dayeat.async.AsyncBluetoothEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrint
import com.example.dayeat.async.AsyncEscPosPrinter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentSettingBinding
import com.example.dayeat.db.DbContract
import com.example.dayeat.model.Konfigurasi
import com.example.dayeat.model.Menu
import com.example.dayeat.model.Product
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
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class SettingFragment : Fragment() {

    private var _binding : FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private var selectedDevice : BluetoothConnection? = null
    private var selectedDevice2: BluetoothDevice? = null


    private lateinit var setting: Setting

    private lateinit var sessionLogin: SessionLogin
    private lateinit var mainViewModel: MainViewModel
    private val calendar = Calendar.getInstance()
    private var mKonfigurasi: MutableList<Konfigurasi> = mutableListOf()
    private var konfigurasiAdapter: KonfigurasiAdapter = KonfigurasiAdapter()
    private var conect: Conect = Conect()

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothDevicesList = mutableListOf<BluetoothDevice>()

    private val REQUEST_ENABLE_BT = 101
    private lateinit var bluetoothHelper: BluetoothPermissionHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        setting = Setting(requireContext())
        sessionLogin = SessionLogin(requireContext())
        bluetoothHelper = BluetoothPermissionHelper(requireActivity(), REQUEST_ENABLE_BT)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.queryDataMethod(requireContext())

        if (setting.getNamePrinterSave() != ""){
            binding.tvSelectBluetoothSetting.text = setting.getNamePrinterSave()
        }

        binding.tvSelectBluetoothSetting.setOnClickListener {
            showBluetoothDeviceSelectionDialog()
//            checkBluetoothPermissions()
        }

        binding.btnPrint.setOnClickListener{
            Log.e("hasil printer save", setting.getPrinter())
            if (setting.getPrinter() == "" || setting.getPrinter() == "Default Printer"){
                Toast.makeText(context, "Tidak ada printer yang di pilih...", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    printBluetooth(setting.getPrinterSave()!!)
                }
            }
        }

        binding.tvPaymentMethodSetting.setOnClickListener {
            lifecycleScope.launch {
                val progressBar = ProgressDialog(context)
                progressBar.setMessage("Mohon tunggu..")
                progressBar.setCanceledOnTouchOutside(false)
                progressBar.setCancelable(false)
                progressBar.show()

                val dialogListMethod = Dialog(requireContext())
                dialogListMethod.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogListMethod.setContentView(R.layout.dialog_list_method)

                val btnCloseDialogMethod = dialogListMethod.findViewById<ImageView>(R.id.btnCloseDialogMethod)
                val btnAddMethod = dialogListMethod.findViewById<RelativeLayout>(R.id.btnAddMethod)
                val btnCloseMethod = dialogListMethod.findViewById<RelativeLayout>(R.id.btnCloseMethod)
                val rvListMethod = dialogListMethod.findViewById<RecyclerView>(R.id.rvListMethod)

                rvListMethod.layoutManager = LinearLayoutManager(context)
                rvListMethod.setHasFixedSize(true)

                btnCloseDialogMethod.setOnClickListener {
                    dialogListMethod.dismiss()
                }

                btnCloseMethod.setOnClickListener{
                    dialogListMethod.dismiss()
                }

                btnAddMethod.setOnClickListener{
                    dialogMethod(0,"")
                }

                mainViewModel.getMethod().observe(viewLifecycleOwner, Observer { getMenuList ->
                    lifecycleScope.launch {
                        if (getMenuList != null){
                            withContext(Dispatchers.IO){
                                mKonfigurasi.clear()
                                val filter = getMenuList.filter { it.tName == "Receipt.PaymentMethod" }
                                mKonfigurasi.addAll(filter)
                            }
                            konfigurasiAdapter.setData(mKonfigurasi)
                            rvListMethod.adapter = konfigurasiAdapter
                            konfigurasiAdapter.setOnItemClickRemoveCallback(object : KonfigurasiAdapter.OnItemClickRemoveCallback {
                                override fun onItemClicked(menuGroup: Konfigurasi) {
                                    val builder = AlertDialog.Builder(context)
                                    builder.setTitle(menuGroup.tName + " Ini mau dihapus cok?")

                                    builder.setPositiveButton("Yoi") { p, _ ->
                                        lifecycleScope.launch {
                                            val resultDelete = withContext(Dispatchers.IO) {
                                                queryHapusMethod(menuGroup.recordId!!.toInt())
                                            }
                                            when (resultDelete) {
                                                "Berhasil" -> {
                                                    p.dismiss()
                                                    mainViewModel.queryDataMethod(requireContext())
                                                }

                                                "Gagal" -> {
                                                    Toast.makeText(
                                                        context,
                                                        "Gagal dihapus guys!!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                else -> {
                                                    Toast.makeText(
                                                        context,
                                                        resultDelete,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }

                                    builder.setNegativeButton("Gak") { p, _ ->
                                        p.dismiss()
                                    }

                                    builder.create().show()
                                }
                            })
                            konfigurasiAdapter.setOnItemClickEditCallback(object :KonfigurasiAdapter.OnItemClickEditCallback{
                                override fun onItemClicked(menuGroup: Konfigurasi) {
                                    dialogMethod(menuGroup.recordId!!.toInt(), menuGroup.code.toString())
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

        binding.tvPromoCodeSetting.setOnClickListener {
            lifecycleScope.launch {
                val progressBar = ProgressDialog(context)
                progressBar.setMessage("Mohon tunggu..")
                progressBar.setCanceledOnTouchOutside(false)
                progressBar.setCancelable(false)
                progressBar.show()

                val dialogListMethod = Dialog(requireContext())
                dialogListMethod.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogListMethod.setContentView(R.layout.dialog_list_method)

                val btnCloseDialogMethod = dialogListMethod.findViewById<ImageView>(R.id.btnCloseDialogMethod)
                val btnAddMethod = dialogListMethod.findViewById<RelativeLayout>(R.id.btnAddMethod)
                val btnCloseMethod = dialogListMethod.findViewById<RelativeLayout>(R.id.btnCloseMethod)
                val rvListMethod = dialogListMethod.findViewById<RecyclerView>(R.id.rvListMethod)
                val tvTitleDialog = dialogListMethod.findViewById<TextView>(R.id.tvTitleDialogMethod)

                tvTitleDialog.text = "PROMO CODE"

                rvListMethod.layoutManager = LinearLayoutManager(context)
                rvListMethod.setHasFixedSize(true)

                btnCloseDialogMethod.setOnClickListener {
                    dialogListMethod.dismiss()
                }

                btnCloseMethod.setOnClickListener{
                    dialogListMethod.dismiss()
                }

                btnAddMethod.setOnClickListener{
                    dialogPromoCode(0,"", 0, 0, 0,0, "", "")
                }

                mainViewModel.getMethod().observe(viewLifecycleOwner, Observer { getMenuList ->
                    lifecycleScope.launch {
                        if (getMenuList != null){
                            withContext(Dispatchers.IO){
                                mKonfigurasi.clear()
                                val filter = getMenuList.filter { it.tName == "Sales.Promo" }
                                mKonfigurasi.addAll(filter)
                            }
                            konfigurasiAdapter.setData(mKonfigurasi)
                            rvListMethod.adapter = konfigurasiAdapter
                            konfigurasiAdapter.setOnItemClickRemoveCallback(object : KonfigurasiAdapter.OnItemClickRemoveCallback {
                                override fun onItemClicked(menuGroup: Konfigurasi) {
                                    val builder = AlertDialog.Builder(context)
                                    builder.setTitle(menuGroup.tName + " Ini mau dihapus cok?")

                                    builder.setPositiveButton("Yoi") { p, _ ->
                                        lifecycleScope.launch {
                                            val resultDelete = withContext(Dispatchers.IO) {
                                                queryHapusMethod(menuGroup.recordId!!.toInt())
                                            }
                                            when (resultDelete) {
                                                "Berhasil" -> {
                                                    p.dismiss()
                                                    mainViewModel.queryDataMethod(requireContext())
                                                }

                                                "Gagal" -> {
                                                    Toast.makeText(
                                                        context,
                                                        "Gagal dihapus guys!!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                else -> {
                                                    Toast.makeText(
                                                        context,
                                                        resultDelete,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }

                                    builder.setNegativeButton("Gak") { p, _ ->
                                        p.dismiss()
                                    }

                                    builder.create().show()
                                }
                            })
                            konfigurasiAdapter.setOnItemClickEditCallback(object :KonfigurasiAdapter.OnItemClickEditCallback{
                                override fun onItemClicked(menuGroup: Konfigurasi) {
                                    dialogPromoCode(menuGroup.recordId!!.toInt(), menuGroup.code.toString(), menuGroup.customV1!!.toInt(), menuGroup.customV4!!.toInt(), menuGroup.customV2!!.toInt(), menuGroup.customV3!!.toInt(), menuGroup.customD1.toString(), menuGroup.customD2.toString())
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

        binding.tvCustomPrintBillSetting.setOnClickListener {

            val progressBar = ProgressDialog(context)
            progressBar.setMessage("Mohon tunggu..")
            progressBar.setCanceledOnTouchOutside(false)
            progressBar.setCancelable(false)

            val dialogPrintBill = Dialog(requireContext())
            dialogPrintBill.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogPrintBill.setContentView(R.layout.dialog_text_print)
            dialogPrintBill.setCanceledOnTouchOutside(false)
            dialogPrintBill.setCancelable(false)

            val header1 = dialogPrintBill.findViewById<EditText>(R.id.etHeader1)
            val header2 = dialogPrintBill.findViewById<EditText>(R.id.etHeader2)
            val header3 = dialogPrintBill.findViewById<EditText>(R.id.etHeader3)
            val header4 = dialogPrintBill.findViewById<EditText>(R.id.etHeader4)
            val header5 = dialogPrintBill.findViewById<EditText>(R.id.etHeader5)

            val footer1 = dialogPrintBill.findViewById<EditText>(R.id.etFooter)
            val footer2 = dialogPrintBill.findViewById<EditText>(R.id.etFooter2)
            val footer3 = dialogPrintBill.findViewById<EditText>(R.id.etFooter3)
            val footer4 = dialogPrintBill.findViewById<EditText>(R.id.etFooter4)
            val footer5 = dialogPrintBill.findViewById<EditText>(R.id.etFooter5)

            val btnSave  = dialogPrintBill.findViewById<Button>(R.id.btnSaveTextPrint)
            val btnPrint = dialogPrintBill.findViewById<Button>(R.id.btnPrintTextPrint)
            val btnClose= dialogPrintBill.findViewById<ImageView>(R.id.imgCloseTextPrint)

            btnClose.setOnClickListener {
                dialogPrintBill.dismiss()
            }

            header1.setText(sessionLogin.getHeader1())
            header2.setText(sessionLogin.getHeader2())
            header3.setText(sessionLogin.getHeader3())
            header4.setText(sessionLogin.getHeader4())
            header5.setText(sessionLogin.getHeader5())

            footer1.setText(sessionLogin.getFooter1())
            footer2.setText(sessionLogin.getFooter2())
            footer3.setText(sessionLogin.getFooter3())
            footer4.setText(sessionLogin.getFooter4())
            footer5.setText(sessionLogin.getFooter5())

            btnSave.setOnClickListener {
                lifecycleScope.launch {
                    progressBar.show()
                    val resultSave = withContext(Dispatchers.IO){
                        querySaveTextPrint(header1.text.toString(), header2.text.toString(), header3.text.toString(), header4.text.toString(), header5.text.toString(), footer1.text.toString(), footer2.text.toString(), footer3.text.toString(), footer4.text.toString(), footer5.text.toString())
                    }
                    when(resultSave){
                        "Berhasil" -> {
                            sessionLogin.createCustomBill(header1.text.toString(), header2.text.toString(), header3.text.toString(), header4.text.toString(), header5.text.toString(), footer1.text.toString(), footer2.text.toString(), footer3.text.toString(), footer4.text.toString(), footer5.text.toString())
                            Toast.makeText(context, "Berhasil menyimpan!!", Toast.LENGTH_SHORT).show()
                        }
                        "Gagal" -> {
                            Toast.makeText(context, "Gagal menyimpan!!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, resultSave, Toast.LENGTH_SHORT).show()
                        }
                    }
                    progressBar.dismiss()
                }
            }

            btnPrint.setOnClickListener {
                lifecycleScope.launch {
                    if (bluetoothHelper.checkAndRequestPermissions()) {
                        bluetoothHelper.checkBluetoothEnabled()
                    }else{
                        printBluetooth()
                    }
                }
            }

            dialogPrintBill.show()
            dialogPrintBill.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialogPrintBill.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogPrintBill.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialogPrintBill.window?.setGravity(Gravity.CENTER)
        }

        binding.stBluetoothOnOffSetting.setOnCheckedChangeListener { _, checked ->
            setting.saveBluetooth(checked)
        }

        binding.stShowTax.setOnCheckedChangeListener { _, checked ->
            setting.saveShowTax(checked)
        }

        binding.stShowChg.setOnCheckedChangeListener { _, checked ->
            setting.saveShowChg(checked)
        }

        binding.btnSimpanSetting.setOnClickListener {
            lifecycleScope.launch {
                val resultSave = withContext(Dispatchers.IO){
                    querySaveTaxChg(binding.etTaxSetting.text.toString().toInt(), binding.etChgSetting.text.toString().toInt())
                }
                when(resultSave){
                    "Berhasil" -> {
                        sessionLogin.createTaxChg(binding.etTaxSetting.text.toString().toInt(), binding.etChgSetting.text.toString().toInt())
                        Toast.makeText(context, "Berhasil menyimpan!!", Toast.LENGTH_SHORT).show()
                    }
                    "Gagal" -> {
                        Toast.makeText(context, "Gagal menyimpan!!", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, resultSave, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.etTaxSetting.setText(sessionLogin.getTax().toString())
        binding.etChgSetting.setText(sessionLogin.getChg().toString())

        binding.stBluetoothOnOffSetting.isChecked = setting.getBluetoothSwitch()
        binding.stShowChg.isChecked = setting.getShowChg()
        binding.stShowTax.isChecked = setting.getShowTax()

//        if (!setting.getBluetoothSwitch()){
//            binding.tvSelectBluetoothSetting.visibility = View.GONE
//            binding.btnPrint.visibility = View.GONE
//            binding.tvBluetoothSetting.visibility = View.GONE
//        }

        return binding.root
    }

    private fun querySaveTextPrint(header1: String, header2: String, header3: String, header4: String, header5: String, footer1: String, footer2: String, footer3: String, footer4: String, footer5: String): String {
        val conn = conect.connection(context)
        if (conn != null) {
            return try {
                val query = "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Header1', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Header2', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Header3', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Header4', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Header5', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Footer1', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Footer2', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Footer3', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Footer4', ?\n" +
                        "EXEC dbo.USP_SH_Settings_SetAsString 'TextBill.Footer5', ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, header1)
                pp.setString(2, header2)
                pp.setString(3, header3)
                pp.setString(4, header4)
                pp.setString(5, header5)
                pp.setString(6, footer1)
                pp.setString(7, footer2)
                pp.setString(8, footer3)
                pp.setString(9, footer4)
                pp.setString(10, footer5)
                pp.execute()
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun querySaveTaxChg(tax: Int, chg: Int): String {
        val conn = conect.connection(context)
        if (conn != null) {
            return try {
                val query = "EXEC dbo.USP_SH_Settings_SetAsMoney 'Sales.ServiceCharge', ? \n" +
                        "EXEC dbo.USP_SH_Settings_SetAsMoney 'Sales.PB1', ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, chg)
                pp.setInt(2, tax)
                pp.execute()
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun dialogPromoCode(recordId: Int, methodName: String, discPersen: Int, discJumlah: Int, minSpan: Int, maxDisc: Int, startDate: String, expire: String){
        val dialogQty = Dialog(requireContext())
        dialogQty.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogQty.setContentView(R.layout.dialog_promo_code)

        val etNameCode = dialogQty.findViewById<EditText>(R.id.etNameCodePromo)
        val etStartDateCode = dialogQty.findViewById<EditText>(R.id.tvStartDatePromoCode)
        val etExpiredDateCode = dialogQty.findViewById<EditText>(R.id.tvExpiredDateJumlahPromoCode)
        val etDiscPersen = dialogQty.findViewById<EditText>(R.id.tvQtyPersenPromoCode)
        val etDiscJumlah = dialogQty.findViewById<EditText>(R.id.tvQtyJumlahPromoCode)
        val etMinSpan = dialogQty.findViewById<EditText>(R.id.etMinSpanPromoCode)
        val etMaxDisc = dialogQty.findViewById<EditText>(R.id.etMaxDiscPromoCode)
        val btnSimpanPromo = dialogQty.findViewById<RelativeLayout>(R.id.btnSimpanPromoCode)
        val btnClosePromo = dialogQty.findViewById<ImageView>(R.id.imgCloseQtyPromoCode)

        if (methodName != ""){
            etNameCode.setText(methodName)
            etDiscPersen.setText(discPersen.toString())
            etDiscJumlah.setText(discJumlah.toString())
            etMinSpan.setText(minSpan.toString())
            etMaxDisc.setText(maxDisc.toString())
            etStartDateCode.setText(startDate)
            etExpiredDateCode.setText(expire)
        }

        btnClosePromo.setOnClickListener {
            dialogQty.dismiss()
        }

        etStartDateCode.setOnClickListener{
            val datePickerDialog = DatePickerDialog(
                requireContext(), { _, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, monthOfYear, dayOfMonth)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedDate.time)
                    etStartDateCode.setText(formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        etExpiredDateCode.setOnClickListener{
            val datePickerDialog = DatePickerDialog(
                requireContext(), { _, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, monthOfYear, dayOfMonth)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedDate.time)
                    etExpiredDateCode.setText(formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        btnSimpanPromo.setOnClickListener{
            if (etNameCode.text.toString() == "" || (etDiscPersen.text.toString() == "" && etDiscJumlah.text.toString() == "") || etStartDateCode.text.toString() == "" || etExpiredDateCode.text.toString() == ""){
                Toast.makeText(requireContext(), if(etNameCode.text.toString() == ""){"PROMO CODE Tidak Boleh kosong,"}else{""} +
                        if(etDiscPersen.text.toString() == ""){"DISC % Tidak Boleh kosong,"}else{""} +
                        if(etDiscJumlah.text.toString() == ""){"DISC Jumlah Tidak Boleh kosong,"}else{""} +
                        if(etStartDateCode.text.toString() == ""){"Start Date Tidak Boleh kosong,"}else{""} +
                        if(etExpiredDateCode.text.toString() == ""){"Expire Date Tidak Boleh kosong,"}else{""} + "!!", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultUpdateMethod = withContext(Dispatchers.IO){
                        queryUpdateMethod(recordId,"Sales.Promo", etNameCode.text.toString(), "", etDiscPersen.text.toString().toIntOrNull()?:0, etMinSpan.text.toString().toIntOrNull()?:0, etMaxDisc.text.toString().toIntOrNull()?:0, etDiscJumlah.text.toString().toIntOrNull()?:0, "", "", etStartDateCode.text.toString(), etExpiredDateCode.text.toString())
                    }
                    when (resultUpdateMethod){
                        "Berhasil" -> {
                            dialogQty.dismiss()
                            mainViewModel.queryDataMethod(requireContext())
                        }
                        "Gagal" -> {
                            Toast.makeText(context, "Gagal menyimpan!!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, resultUpdateMethod, Toast.LENGTH_SHORT).show()
                        }
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

    private fun dialogMethod(recordId: Int, methodName: String){
        val dialogQty = Dialog(requireContext())
        dialogQty.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogQty.setContentView(R.layout.dialog_qty)

        val tvTitleDialog = dialogQty.findViewById<TextView>(R.id.tvTitleQty)
        val etQty = dialogQty.findViewById<EditText>(R.id.tvQtyJumlah)
        val btnSimpan = dialogQty.findViewById<RelativeLayout>(R.id.btnSimpanQty)
        val btnCloseQty = dialogQty.findViewById<ImageView>(R.id.imgCloseQty)

        tvTitleDialog.text = "M E T H O D"
        etQty.inputType = InputType.TYPE_CLASS_TEXT
        etQty.setText(methodName)

        btnCloseQty.setOnClickListener {
            dialogQty.dismiss()
        }

        btnSimpan.setOnClickListener{
            if (etQty.text.toString() == ""){
                Toast.makeText(requireContext(), "Tidak boleh kosong!!", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultUpdateMethod = withContext(Dispatchers.IO){
                        queryUpdateMethod(recordId,"Receipt.PaymentMethod", etQty.text.toString(), "", 0, 0, 0, 0, "", "", "", "")
                    }
                    when (resultUpdateMethod){
                        "Berhasil" -> {
                            dialogQty.dismiss()
                            mainViewModel.queryDataMethod(requireContext())
                        }
                        "Gagal" -> {
                            Toast.makeText(context, "Gagal menyimpan!!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, resultUpdateMethod, Toast.LENGTH_SHORT).show()
                        }
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

    private fun queryUpdateMethod(recordId: Int, tName: String, code: String, description: String, customV1: Int, customV2: Int,customV3: Int,customV4: Int, customStr1: String, customStr2: String, customD1: String, customD2: String): String {
        val conn = conect.connection(context)
        if (conn != null) {
            try {
                val query = "EXEC USP_J_Lookup_UpdateMobile @RecordId = ?, @TName = ?, @Code = ?, @Description = ?, @CustomV1 = ?, @CustomV2 = ?, @CustomV3 = ?, @CustomV4 = ?, @CustomStr1 = ?, @CustomStr2 = ?, @CustomD1 = ?, @CustomD2 = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, recordId)
                pp.setString(2, tName)
                pp.setString(3, code)
                pp.setString(4, description)
                pp.setInt(5, customV1)
                pp.setInt(6, customV2)
                pp.setInt(7, customV3)
                pp.setInt(8, customV4)
                pp.setString(9, customStr1)
                pp.setString(10, customStr2)
                pp.setString(11, customD1)
                pp.setString(12, customD2)
                pp.execute()
                return "Berhasil"
            } catch (e: SQLException) {
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusMethod(recordId: Int): String {
        val conn = conect.connection(context)
        if (conn != null) {
            return try {
                val query = "EXEC USP_J_Lookup_Delete @RecordID = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, recordId)
                pp.execute()
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun checkBluetoothPermissions() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent,201)
        }else{
            if (bluetoothDevicesList != null) {
                val items = arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                items[0] = "Default printer"
                for ((i, device) in bluetoothDevicesList.withIndex()) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    items[i + 1] = device.device.name
                }

                val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                alertDialog.setTitle("Bluetooth printer selection")
                alertDialog.setItems(items) { _, i1 ->
                    val index = i1 - 1
                    selectedDevice = if (index == -1) {
                        null
                    } else {
                        bluetoothDevicesList[index]
                    }

                    val spanString = SpannableString(items[i1])
                    spanString.setSpan(
                        ForegroundColorSpan(Color.BLACK),
                        0,
                        spanString.length,
                        0
                    )
                    binding.tvSelectBluetoothSetting.text = spanString
                    Log.e("Hasil pilih printer", selectedDevice!!.device.toString())

                    if (selectedDevice != null) {
                        setting.savePrinter(selectedDevice!!.device.toString(), spanString.toString())
                    } else {
                        setting.savePrinter("Default Printer", spanString.toString())
                    }
                }
                val alert = alertDialog.create()
                alert.setCanceledOnTouchOutside(true)
                alert.show()
            }
        }
    }

    private fun showBluetoothDeviceSelectionDialog() {
        // Ambil daftar perangkat yang dipasangkan

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    1001
                )
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val deviceNames = mutableListOf<String>()

        pairedDevices?.forEach { device ->
            bluetoothDevicesList.add(device)
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            deviceNames.add("$deviceName - $deviceAddress")
        }

        if (deviceNames.isEmpty()) {
            deviceNames.add("No Bluetooth devices found")
        }

        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Bluetooth Printer Selection")
        alertDialog.setItems(deviceNames.toTypedArray()) { _, i ->
            if (deviceNames[i] == "No Bluetooth devices found") {
                return@setItems
            }

            selectedDevice2 = bluetoothDevicesList.getOrNull(i)

            val spanString = SpannableString(deviceNames[i])
            spanString.setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                spanString.length,
                0
            )

            binding.tvSelectBluetoothSetting.text = spanString
            Log.e("Hasil pilih printer", selectedDevice2!!.address.toString())

            if (selectedDevice2 != null) {
                setting.savePrinter(selectedDevice2!!.address.toString(), spanString.toString())
            } else {
                setting.savePrinter("Default Printer", spanString.toString())
            }
        }
        alertDialog.show()
    }

    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    1001
                )
            }
        }
    }

    private suspend fun printBluetooth(printerConnection: BluetoothConnection):String {
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
            ).execute(getAsyncEscPosPrinter(printerConnection)).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinter(printerConnection: BluetoothConnection): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
            try {
                printer.addTextToPrint("Connection Sukses....")
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

    private suspend fun printBluetooth():String {
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
            ).execute(getAsyncEscPosPrinter()).toString()
            result.await()
        }
    }

    private suspend fun getAsyncEscPosPrinter(): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(setting.getPrinterSave(), 203, 48f, 32)
            try {
                printer.addTextToPrint("\n" +
                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, requireActivity().resources.getDrawableForDensity(R.drawable.logo_ii, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                        "[L]\n" +
                        "[C]${sessionLogin.getHeader1()}\n" +
                        "[C]${sessionLogin.getHeader2()}\n" +
                        "[C]${sessionLogin.getHeader3()}\n" +
                        "[C]${sessionLogin.getHeader4()}\n" +
                        "[C]${sessionLogin.getHeader5()}\n" +
                        "[L]No BILL      [R]: [R]2025/0001\n" +
                        "[L]No Meja      [R]: [R]A01\n" +
                        "[L]Customer     [R]: [R]Ex.Name\n"+
                        "[L]Tanggal Cetak[R]: [R]${getCurrentDate("yyyy-MM-dd")}\n" +
                        "[L]Waktu Cetak  [R]: [R]${getCurrentDate("HH:mm")}\n" +
                        "[L]Kasir        [R]: [R]${sessionLogin.getUserLogin()}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        "[L]Ex. Menu\n" +
                        "[L]1[L]x[L]50.000 [R]50.000\n" +
                        "[L]================================\n" +
                        "[L]<b>PROMO [R]Ex. Promo</b>\n" +
                        "[L]<b>DISC  [R]Rp 10.000</b>\n" +
                        "[L]<b>TOTAL [R]Rp 40.000</b>\n" +
                        "[L]ITEMS    [R]1\n" +
                        "[L]TUNAI    [R]Rp 50.000\n" +
                        "[L]KEMBALIAN[R]Rp 10.000\n" +
                        "[L]\n" +
                        "[C]<b>${sessionLogin.getFooter1()}</b>\n"+
                        "[C]<b>${sessionLogin.getFooter2()}</b>\n"+
                        "[C]<b>${sessionLogin.getFooter3()}</b>\n"+
                        "[C]<b>${sessionLogin.getFooter4()}</b>\n"+
                        "[C]<b>${sessionLogin.getFooter5()}</b>\n"+
                        "[L]\n")
            }catch (e: EscPosEncodingException){
                e.toString()
                Log.e("cek printer", e.toString())
            }
            printer
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}