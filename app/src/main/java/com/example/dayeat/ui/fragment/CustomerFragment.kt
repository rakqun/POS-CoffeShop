package com.example.dayeat.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.CustomerAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentCustomerBinding
import com.example.dayeat.model.Customer
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.SessionLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.util.Locale

class CustomerFragment : Fragment() {

    private var _binding: FragmentCustomerBinding? = null
    private val binding get() = _binding!!

    private var conect: Conect = Conect()
    private lateinit var sessionLogin: SessionLogin
    private var customerAdapter: CustomerAdapter = CustomerAdapter()
    private var mCustomer: MutableList<Customer> = mutableListOf()

    private var arrayFlag: ArrayList<String> = arrayListOf("", "DISABLE", "HIDE")

    private lateinit var progressBar: ProgressDialog

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerBinding.inflate(inflater, container, false)

        sessionLogin = SessionLogin(requireContext())
        progressBar  = ProgressDialog(context)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvListCustomer.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvListCustomer.layoutManager = GridLayoutManager(context, 3)
        }

        binding.rvListCustomer.setHasFixedSize(true)

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.queryDataCustomer(requireContext())
            }
        }

        mainViewModel.getCustomer().observe(viewLifecycleOwner, Observer { menuGroup ->
            if (menuGroup != null){
                customerAdapter.setData(menuGroup)
                showItems()
            }
        })

        binding.pullRefress.setOnRefreshListener {
            mainViewModel.queryDataCustomer(requireContext())
            binding.pullRefress.setRefreshing(false)
        }

        binding.fabAddCustomer.setOnClickListener {
            dialogCategory(0, "", "", "", "", "", 0, "")
        }

        return binding.root
    }

    private fun showItems(){
        binding.rvListCustomer.adapter = customerAdapter
        customerAdapter.setOnItemClickRemoveCallback(object : CustomerAdapter.OnItemClickRemoveCallback{
            override fun onItemClicked(customer: Customer) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(customer.cName + " Ini mau dihapus cok?")

                builder.setPositiveButton("Yoi"){p,_->
                    lifecycleScope.launch {
                        val resultDelete = withContext(Dispatchers.IO){
                            queryHapusCust(customer.custId!!.toInt())
                        }
                        when(resultDelete){
                            "Berhasil" -> {
                                p.dismiss()
                                mainViewModel.queryDataCustomer(requireContext())
                            }
                            "Gagal" -> {
                                Toast.makeText(context, "Gagal dihapus guys!!", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(context, resultDelete, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                builder.setNegativeButton("Gak"){p,_->
                    p.dismiss()
                }

                builder.create().show()
            }
        })

        customerAdapter.setOnItemClickEditCallback(object : CustomerAdapter.OnItemClickEditCallback{
            override fun onItemClicked(customer: Customer) {
                dialogCategory(customer.custId!!.toInt(),
                    customer.regNo.toString(),
                    customer.cName.toString(),
                    customer.addr.toString(),
                    customer.phone.toString(),
                    customer.email.toString(),
                    customer.cDisc!!.toInt(),
                    customer.cFlags.toString())
            }
        })
    }

    private fun dialogCategory(custId: Int, regNo: String, cName: String, alamat: String, phone:String, email: String, disc:Int, flag: String){
        val dialogAddCategory = Dialog(requireContext())
        dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogAddCategory.setContentView(R.layout.dialog_customer)
        dialogAddCategory.setCanceledOnTouchOutside(false)

        val tvTitle = dialogAddCategory.findViewById<TextView>(R.id.tvTitleDialogCustomer)
        val etNameCustomer = dialogAddCategory.findViewById<EditText>(R.id.etNameDialogCustomer)
        val etNoRegCustomer = dialogAddCategory.findViewById<EditText>(R.id.etNoRegDialogCustomer)
        val etDiscCustomer = dialogAddCategory.findViewById<EditText>(R.id.etDiscontDialogCustomer)
        val etAlamatCustomer = dialogAddCategory.findViewById<EditText>(R.id.etAlamatDialogCustomer)
        val etPhoneCustomer = dialogAddCategory.findViewById<EditText>(R.id.etPhoneDialogCustomer)
        val etEmailCustomer = dialogAddCategory.findViewById<EditText>(R.id.etEmailDialogCustomer)
        val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.imgCloseCustomer)
        val btnBatal = dialogAddCategory.findViewById<TextView>(R.id.tvBatalCustomer)
        val btnSimpan = dialogAddCategory.findViewById<TextView>(R.id.tvSimpanCustomer)
        val spinnerFlag = dialogAddCategory.findViewById<Spinner>(R.id.spinnerFlagsCustomer)
        val checkPb1 = dialogAddCategory.findViewById<CheckBox>(R.id.checkNoPb1)
        val checkChg = dialogAddCategory.findViewById<CheckBox>(R.id.checkNoServiceChg)

        var selectedFlag = ""
        val adapterFlag = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayFlag)
        adapterFlag.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFlag.adapter = adapterFlag
        spinnerFlag.setSelection(0)
        spinnerFlag.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedFlag = arrayFlag[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        if (custId > 0){
            tvTitle.text = "Edit $cName"
            etNameCustomer.setText(cName)
            etDiscCustomer.setText(disc.toString())
            etNoRegCustomer.setText(regNo.toString())
            etNoRegCustomer.isFocusableInTouchMode = false
            etAlamatCustomer.setText(alamat)
            etEmailCustomer.setText(email)
            etPhoneCustomer.setText(phone)
            checkPb1.isChecked = flag.contains("NO_PB1")
            checkChg.isChecked = flag.contains("NO_SERVICE_CHARGE")
            spinnerFlag.setSelection(adapterFlag.getPosition(if(flag.contains("HIDE")){"HIDE"}else if (flag.contains("DISABLE")){"DISABLE"}else{""}))
        }

        btnClose.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnBatal.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnSimpan.setOnClickListener{
            val getFlag = if(checkPb1.isChecked){"NO_PB1,"}else{""} + if(checkChg.isChecked){"NO_SERVICE_CHARGE,"}else{""} + selectedFlag
            if (etNameCustomer.text.toString().isEmpty()){
                Toast.makeText(context, "Name tidak boleh kosong ya..", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultSimpan = withContext(Dispatchers.IO){
                        querySaveCat(custId,
                            etNoRegCustomer.text.toString(),
                            etNameCustomer.text.toString().capitalize(Locale.ROOT),
                            etAlamatCustomer.text.toString(),
                            etPhoneCustomer.text.toString(),
                            etEmailCustomer.text.toString(),
                            etDiscCustomer.text.toString().toIntOrNull() ?: 0,
                            getFlag)
                    }
                    when (resultSimpan){
                        "Berhasil" -> {
                            dialogAddCategory.dismiss()
                            mainViewModel.queryDataCat(requireContext())
                        }
                        "Gagal" -> {
                            Toast.makeText(context, "Gagal nyimpan guys!!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, resultSimpan, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dialogAddCategory.show()
        dialogAddCategory.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialogAddCategory.window?.setGravity(Gravity.CENTER)
    }

    private fun querySaveCat(custId: Int, regNo: String, cName:String, addr: String, phone: String, email: String, disc: Int, flags: String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Cust_Update @CustID = ?, @RegNo = ?, @CName = ?, @Addr = ?, @Phone = ?, @Email = ?, @User = ?, @CDisc = ?, @CFlags = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, custId)
                pp.setString(2, regNo)
                pp.setString(3, cName)
                pp.setString(4, addr)
                pp.setString(5, phone)
                pp.setString(6, email)
                pp.setString(7, sessionLogin.getUserLogin())
                pp.setInt(8, disc)
                pp.setString(9, flags)
                pp.execute()
                "Berhasil"
            }catch (e:SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusCust(custID: Int):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Cust_Delete @CustID = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, custID)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}