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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.MenuAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentChildMenuBinding
import com.example.dayeat.model.Menu
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException


class ChildMenuFragment : Fragment() {

    private var _binding: FragmentChildMenuBinding? = null
    private val binding get() = _binding!!

    private var conect: Conect = Conect()
    private var menuAdapter: MenuAdapter? = null
    private var mMenu: MutableList<Menu>? = null

    private var code = 0

    private lateinit var progressBar: ProgressDialog

    private var arrayFlag: ArrayList<String> = arrayListOf("", "Disable", "Hide")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChildMenuBinding.inflate(inflater, container, false)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvMenuGroup.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvMenuGroup.layoutManager = GridLayoutManager(context, 3)
        }

        binding.rvMenuGroup.setHasFixedSize(true)

//        showItems(dataId)

        binding.pullRefres.setOnRefreshListener {
//            showItems(dataId)
            binding.pullRefres.setRefreshing(false)
        }

        binding.fabAddMenu.setOnClickListener {
            Log.e("hasil", "tab")
//            dialogCategory(0, "$dataName$code", "", 0, "", 0, "")
        }

        return binding.root
    }

    private fun dialogCategory(menuId: Int, menuCode: String, menuName: String, catId: Int, flag: String, price: Int, prefs: String){
        val dialogAddCategory = Dialog(requireContext())
        dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogAddCategory.setContentView(R.layout.dialog_menu)
        dialogAddCategory.setCanceledOnTouchOutside(false)

        val tvTitle = dialogAddCategory.findViewById<TextView>(R.id.tvTitleDialogMenu)
        val etMenuName = dialogAddCategory.findViewById<EditText>(R.id.etNameMenuDialog)
        val etPriceName = dialogAddCategory.findViewById<EditText>(R.id.etHargaMenuDialog)
        val etPref = dialogAddCategory.findViewById<EditText>(R.id.etPrefMenuDialog)
        val chipGroupPref = dialogAddCategory.findViewById<ChipGroup>(R.id.chipGroupPref)
        val spinnerFlag = dialogAddCategory.findViewById<Spinner>(R.id.spinnerFlagsMenu)
        val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.imgCloseMenu)
        val btnBatal = dialogAddCategory.findViewById<TextView>(R.id.tvBatalMenu)
        val btnSimpan = dialogAddCategory.findViewById<TextView>(R.id.tvSimpanMenu)

        btnBatal.text = "Batal"
        tvTitle.text = "Tambah Menu"

        etPref.imeOptions = EditorInfo.IME_ACTION_DONE
        etPref.setOnEditorActionListener(TextView.OnEditorActionListener{_,actionId,_ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                val chip = Chip(context)
                chip.text = etPref.text.toString()

                chip.isCloseIconVisible = true

                chip.setOnCloseIconClickListener{
                    chipGroupPref.removeView(chip)
                }

                chipGroupPref.addView(chip)
                return@OnEditorActionListener true
            }
            false
        })

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
            tvTitle.text = "Edit $menuName"
            etMenuName.setText(menuName)
            etPriceName.setText(price)
            spinnerFlag.setSelection(adapterFlag.getPosition(flag))
        }

        btnClose.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnBatal.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnSimpan.setOnClickListener{
            lifecycleScope.launch {

                var prefChip = ""

                for (i in 0 until chipGroupPref.childCount) {
                    val chip = chipGroupPref.getChildAt(i)
                    prefChip += "$chip,"
                }

                val resultSimpan = withContext(Dispatchers.IO){
                    if (menuId > 0){
                        queryUpdateMenu(menuId, menuCode, etMenuName.text.toString(), catId, selectedFlag, etPriceName.text.toString().toInt(), prefChip)
                    }else{
                        querySaveMenu(menuCode, etMenuName.text.toString(), catId, selectedFlag, etPriceName.text.toString().toInt(), prefChip)
                    }
                }
                when (resultSimpan){
                    "Berhasil" -> {
                        dialogAddCategory.dismiss()
                        showItems(catId)
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

        dialogAddCategory.show()
        dialogAddCategory.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialogAddCategory.window?.setGravity(Gravity.CENTER)
    }

    private fun showItems(dataInt: Int){
        lifecycleScope.launch {

            progressBar.setMessage("Tunggu woi...!!")
            progressBar.setCanceledOnTouchOutside(false)
            progressBar.show()

            val resultItems = withContext(Dispatchers.IO){
                queryDataMenu(dataInt)
            }

            when (resultItems){
                "Berhasil" -> {

                    code = "000".toInt() + mMenu!!.size

                    menuAdapter = MenuAdapter()
                    binding.rvMenuGroup.adapter = menuAdapter
                    menuAdapter!!.setOnItemClickRemoveCallback(object : MenuAdapter.OnItemClickRemoveCallback{
                        override fun onItemClicked(menuGroup: Menu) {
                            val builder = AlertDialog.Builder(context)
                            builder.setTitle(menuGroup.menuName + " Ini mau dihapus cok?")

                            builder.setPositiveButton("Yoi"){p,_->
                                lifecycleScope.launch {
                                    val resultDelete = withContext(Dispatchers.IO){
                                        queryHapusMenu(menuGroup.catId!!.toInt())
                                    }
                                    when(resultDelete){
                                        "Berhasil" -> {
                                            p.dismiss()
                                            showItems(dataInt)
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

                    menuAdapter!!.setOnItemClickEditCallback(object : MenuAdapter.OnItemClickEditCallback{
                        override fun onItemClicked(menuGroup: Menu) {
                            dialogCategory(menuGroup.menuId!!.toInt(), menuGroup.menuCode.toString(), menuGroup.menuName.toString(), menuGroup.catId!!.toInt(), menuGroup.flag.toString(), menuGroup.price!!.toInt(), menuGroup.prefs.toString())
                        }
                    })
                }
                "Gagal" -> {
                    Snackbar.make(binding.root, "Ngelek cok, ulang lagi!!", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    Snackbar.make(binding.root, resultItems, Snackbar.LENGTH_LONG).show()
                }
            }
            progressBar.dismiss()
        }
    }

    private fun querySaveMenu(menuCode: String, menuName: String, catId: Int, flag: String, price: Int, prefs: String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Menu_Update @MenuCode = ?, @MenuName = ?, @CatId = ?, @MFlags = ?, @Price = ?, @Prefs = ?, @By = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, menuCode)
                pp.setString(2, menuName)
                pp.setInt(3, catId)
                pp.setString(4, flag)
                pp.setInt(5, price)
                pp.setString(6, prefs)
                pp.setString(7, "Admin")
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdateMenu(menuId: Int, menuCode: String, menuName: String, catId: Int, flag: String, price: Int, prefs: String):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Menu_Update @MenuCode = ?, @MenuName = ?, @CatId = ?, @MFlags = ?, @Price = ?, @Prefs = ?, @By = ?, @MenuID = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, menuCode)
                pp.setString(2, menuName)
                pp.setInt(3, catId)
                pp.setString(4, flag)
                pp.setInt(5, price)
                pp.setString(6, prefs)
                pp.setString(7, "Admin")
                pp.setInt(8, menuId)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusMenu(menuId: Int):String{
        val conn = conect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_Menu_Delete @MenuID = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, menuId)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryDataMenu(catId: Int):String{
        val conn = conect.connection(context)
        mMenu = mutableListOf()
        if (conn!= null){
            return try {
                val query = "SELECT * FROM J_Menu WHERE CatId = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, catId)
                val rs = pp.executeQuery()
                mMenu!!.clear()
                while (rs.next()){
                    val menu = Menu()
                    menu.menuId = rs.getInt("MenuId")
                    menu.menuName = rs.getString("MenuName")
                    menu.menuCode = rs.getString("MenuCode")
                    menu.catId = rs.getInt("CatId")
                    menu.price = rs.getInt("Price")
                    menu.flag = rs.getString("MFlags")
                    menu.prefs = rs.getString("Prefs")
                    mMenu?.add(menu)
                }
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