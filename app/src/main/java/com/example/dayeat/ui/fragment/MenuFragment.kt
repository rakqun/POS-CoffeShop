package com.example.dayeat.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
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
import android.view.inputmethod.InputMethodManager
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
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentMenuBinding
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.mvvm.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private var conect: Conect = Conect()
    private var categoryAdapter: CategoryAdapter = CategoryAdapter()
    private var mMenuGroup: MutableList<MenuGroup> = mutableListOf()

    private var arrayDept: ArrayList<String> = arrayListOf("", "Food", "Beverage")
    private var arrayFlag: ArrayList<String> = arrayListOf("", "Disable", "Hide")

    private lateinit var progressBar: ProgressDialog

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)

        progressBar = ProgressDialog(context)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvMenuGroup.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvMenuGroup.layoutManager = GridLayoutManager(context, 3)
        }

        binding.rvMenuGroup.setHasFixedSize(true)

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.queryDataCat(requireContext())
            }
        }

        mainViewModel.getMenuGroup().observe(viewLifecycleOwner, Observer { menuGroup ->
            if (menuGroup != null){
                mMenuGroup.clear()
                mMenuGroup.addAll(menuGroup)
                categoryAdapter.setData(mMenuGroup)
                showItems()
            }
        })

        binding.pullRefres.setOnRefreshListener {
            mainViewModel.queryDataCat(requireContext())
            binding.pullRefres.setRefreshing(false)
        }

        binding.fabAddMenu.setOnClickListener {
            dialogCategory(0, "", "", "")
        }

        return binding.root
    }

    private fun dialogCategory(catId: Int, catName: String, dept: String, flag: String){
        val dialogAddCategory = Dialog(requireContext())
        dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogAddCategory.setContentView(R.layout.dialog_group_menu)
        dialogAddCategory.setCanceledOnTouchOutside(false)

        val tvTitle = dialogAddCategory.findViewById<TextView>(R.id.tvTitleDialog)
        val etCategoryName = dialogAddCategory.findViewById<EditText>(R.id.etNameCategoryDialog)
        val spinnerDept = dialogAddCategory.findViewById<Spinner>(R.id.spinnerDept)
        val spinnerFlag = dialogAddCategory.findViewById<Spinner>(R.id.spinnerFlags)
        val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.imgClose)
        val btnBatal = dialogAddCategory.findViewById<TextView>(R.id.tvHapusOrBatal)
        val btnSimpan = dialogAddCategory.findViewById<TextView>(R.id.tvSimpan)

        btnBatal.text = "Batal"
        tvTitle.text = "Tambah Category"

        var selectedDept = ""
        val adapterDept = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayDept)
        adapterDept.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDept.adapter = adapterDept
        spinnerDept.setSelection(0)
        spinnerDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedDept = arrayDept[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

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

        if (catId > 0){
            tvTitle.text = "Edit $catName"
            etCategoryName.setText(catName)
            spinnerDept.setSelection(adapterDept.getPosition(dept))
            spinnerFlag.setSelection(adapterFlag.getPosition(flag))
        }

        btnClose.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnBatal.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnSimpan.setOnClickListener{
            if (etCategoryName.text.toString().isEmpty()){
                Toast.makeText(context, "Category tidak boleh kosong ya..", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultSimpan = withContext(Dispatchers.IO){
                        if (catId > 0){
                            queryUpdateCat(catId, etCategoryName.text.toString(), selectedFlag, selectedDept)
                        }else{
                            querySaveCat(etCategoryName.text.toString(), selectedFlag, selectedDept)
                        }
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

    private fun showItems(){
        binding.rvMenuGroup.adapter = categoryAdapter
        categoryAdapter.setOnItemClickCallback(object : CategoryAdapter.OnItemClickCallback {
            override fun onItemClicked(menuGroup: MenuGroup) {

//                val action = MenuFragmentDirections.actionNavMenuToChildMenuFragment()
//                action.menuNameGroup = menuGroup.menuName.toString()
//                action.menuGroupId = menuGroup.menuId!!.toInt()

                val bundle = Bundle().apply {
                    putString("menuNameGroup", menuGroup.catName.toString())
                    putInt("menuGroupId", menuGroup.catId!!.toInt())
                }

                view!!.findNavController()
                    .navigate(R.id.action_nav_menu_to_childMenuActivity, bundle)
            }
        })

        categoryAdapter.setOnItemClickRemoveCallback(object : CategoryAdapter.OnItemClickRemoveCallback{
            override fun onItemClicked(menuGroup: MenuGroup) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(menuGroup.catName + " Ini mau dihapus cok?")

                builder.setPositiveButton("Yoi"){p,_->
                    lifecycleScope.launch {
                        val resultDelete = withContext(Dispatchers.IO){
                            queryHapusCat(menuGroup.catId!!.toInt())
                        }
                        when(resultDelete){
                            "Berhasil" -> {
                                p.dismiss()
                                mainViewModel.queryDataCat(requireContext())
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

        categoryAdapter.setOnItemClickEditCallback(object : CategoryAdapter.OnItemClickEditCallback{
            override fun onItemClicked(menuGroup: MenuGroup) {
                dialogCategory(menuGroup.catId!!.toInt(), menuGroup.catName.toString(), menuGroup.catDept.toString(), menuGroup.catFlag.toString())
            }
        })
    }

    private fun querySaveCat(catName: String, flag: String, dept:String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Cat_Update @CatName = ?, @CatFlags = ?, @Dept = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, catName)
                pp.setString(2, flag)
                pp.setString(3, dept)
                pp.execute()
                "Berhasil"
            }catch (e:SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdateCat(catId: Int, catName: String, flag: String, dept:String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Cat_Update @CatName = ?, @CatFlags = ?, @Dept = ?, @CatId = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, catName)
                pp.setString(2, flag)
                pp.setString(3, dept)
                pp.setInt(4, catId)
                pp.execute()
                "Berhasil"
            }catch (e:SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusCat(catId: Int):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Cat_Delete @CatId = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, catId)
                pp.execute()
                "Berhasil"
            }catch (e:SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryDataCat():String{
        val conn = conect.connection(context)
        mMenuGroup = mutableListOf()
        if (conn!= null){
            return try {
                val query = "SELECT * FROM J_Cat"
                val pp = conn.createStatement()
                val rs = pp.executeQuery(query)
                mMenuGroup.clear()
                while (rs.next()){
                    val getCatName = rs.getString("CatName")
                    if (getCatName != ""){
                        val menuGroup = MenuGroup()
                        menuGroup.catId = rs.getInt("CatId")
                        menuGroup.catName = getCatName
                        menuGroup.catDept = rs.getString("Dept")
                        menuGroup.catFlag = rs.getString("CatFlags")
                        mMenuGroup.add(menuGroup)
                    }
                }
                "Berhasil"
            }catch (e:SQLException){
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