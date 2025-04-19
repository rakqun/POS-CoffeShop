package com.example.dayeat.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.CategoryAdapter
import com.example.dayeat.adapter.TableAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentTableBinding
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Table
import com.example.dayeat.mvvm.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class TableFragment : Fragment() {

    private var _binding : FragmentTableBinding? = null
    private val binding get() = _binding!!

    private var conect: Conect = Conect()
    private var tableAdapter: TableAdapter = TableAdapter()
    private var mTable: MutableList<Table> = mutableListOf()

    private var arrayFlag: ArrayList<String> = arrayListOf("", "Disable", "Hide")

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTableBinding.inflate(inflater, container, false)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvTable.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvTable.layoutManager = GridLayoutManager(context, 3)
        }

        binding.rvTable.setHasFixedSize(true)

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.queryDataTable(requireContext())
            }
        }

        mainViewModel.getTable().observe(viewLifecycleOwner, Observer { table ->
            if (table != null){
                mTable.clear()
                mTable.addAll(table)
                tableAdapter.setData(mTable)
                showItems()
            }
        })

        binding.pullRefresTable.setOnRefreshListener {
            mainViewModel.queryDataTable(requireContext())
            binding.pullRefresTable.setRefreshing(false)
        }

        binding.fabAddTable.setOnClickListener {
            dialogCategory(0,"", "")
        }

        return binding.root
    }

    private fun dialogCategory(tableId: Int, tableCode: String, tFlags:String){
        val dialogAddCategory = Dialog(requireContext())
        dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogAddCategory.setContentView(R.layout.dialog_table)
        dialogAddCategory.setCanceledOnTouchOutside(false)

        val tvTitleTable = dialogAddCategory.findViewById<TextView>(R.id.tvTitleDialogTable)
        val etTableName = dialogAddCategory.findViewById<EditText>(R.id.etTableNameDialog)
        val spinnerFlag = dialogAddCategory.findViewById<Spinner>(R.id.spinnerFlagsTableDialog)
        val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.imgCloseTable)
        val btnBatal = dialogAddCategory.findViewById<TextView>(R.id.tvBatalTableDialog)
        val btnSimpan = dialogAddCategory.findViewById<TextView>(R.id.tvSimpanTableDialog)

        btnBatal.text = "Batal"
        tvTitleTable.text = "Tambah Meja"

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

        if (tableId > 0){
            tvTitleTable.text = "Edit $tableCode"
            etTableName.setText(tableCode.toString())
            spinnerFlag.setSelection(adapterFlag.getPosition(tFlags))
        }

        btnClose.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnBatal.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnSimpan.setOnClickListener{
            if (etTableName.text.toString().isEmpty()){
                Toast.makeText(context, "Table tidak boleh kosong ya..", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultSimpan = withContext(Dispatchers.IO){
                        if (tableId > 0){
                            queryUpdateCat(tableId, etTableName.text.toString(), selectedFlag)
                        }else{
                            querySaveCat(etTableName.text.toString(), selectedFlag)
                        }
                    }
                    when (resultSimpan){
                        "Berhasil" -> {
                            dialogAddCategory.dismiss()
                            mainViewModel.queryDataTable(requireContext())
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

    private fun showItems(){
        binding.rvTable.adapter = tableAdapter
        tableAdapter.setOnItemClickRemoveCallback(object : TableAdapter.OnItemClickRemoveCallback{
            override fun onItemClicked(table: Table) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(table.tableCode + " Ini mau dihapus cok?")

                builder.setPositiveButton("Yoi"){p,_->
                    lifecycleScope.launch {
                        val resultDelete = withContext(Dispatchers.IO){
                            queryHapusCat(table.tableId!!.toInt())
                        }
                        when(resultDelete){
                            "Berhasil" -> {
                                p.dismiss()
                                mainViewModel.queryDataTable(requireContext())
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

        tableAdapter.setOnItemClickEditCallback(object : TableAdapter.OnItemClickEditCallback{
            override fun onItemClicked(table: Table) {
                dialogCategory(table.tableId!!.toInt(), table.tableCode.toString(), table.tFlags.toString())
            }
        })
    }

    private fun querySaveCat(tableCode:String, flag:String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Table_Update @TableCode = ?, @TGroupID = ?, @TFlags = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, tableCode)
                pp.setInt(2, 1)
                pp.setString(3, flag)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdateCat(tableId: Int,tableCode: String, flag:String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Table_Update @TableID =?, @TableCode = ?, @TGroupID = ?, @TFlags = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, tableId)
                pp.setString(2, tableCode)
                pp.setInt(3, 1)
                pp.setString(4, flag)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusCat(tableId: Int):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Table_Delete @TableId = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, tableId)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }
}