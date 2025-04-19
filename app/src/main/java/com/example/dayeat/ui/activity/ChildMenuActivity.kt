package com.example.dayeat.ui.activity

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.res.Configuration
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
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.MenuAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.ActivityChildMenuBinding
import com.example.dayeat.model.Menu
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.SessionLogin
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import java.lang.Math.round
import java.sql.SQLException
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class ChildMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildMenuBinding

    private var conect: Conect = Conect()
    private var menuAdapter: MenuAdapter = MenuAdapter()
    private var mMenu: MutableList<Menu> = mutableListOf()
    private lateinit var sessionLogin: SessionLogin

    private var code = 0

    private lateinit var progressBar: ProgressDialog

    private lateinit var mainViewModel: MainViewModel

    private var arrayFlag: ArrayList<String> = arrayListOf("", "Disable", "Hide")

    private var dataId = 0

    private var getTax:Float = 0.0F
    private var getChg:Float = 0.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = ProgressDialog(this)
        sessionLogin = SessionLogin(this)

        getTax = calculatePercentage(sessionLogin.getTax(), 100)
        getChg = calculatePercentage(sessionLogin.getChg(), 100)

        Log.e("Get Tax", getTax.toString())

        val dataName = intent.getStringExtra("menuNameGroup")
        dataId = intent.getIntExtra("menuGroupId", 0)

        binding.tvTitleChildMenu.text = dataName

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvMenuGroup.layoutManager = GridLayoutManager(this, 1)
        } else {
            binding.rvMenuGroup.layoutManager = GridLayoutManager(this, 3)
        }

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                Log.e("hasil cat id", dataId.toString())
                mainViewModel.queryDataMenu(this@ChildMenuActivity, dataId)
            }
        }

        mainViewModel.getMenu().observe(this, Observer { getMenuList ->
            if (getMenuList != null){
                Log.e("hasil list menu", getMenuList.toString())
                mMenu.clear()
                mMenu.addAll(getMenuList)
                menuAdapter.setData(mMenu)
                showItems()
            }
        })

        binding.fbBack.setOnClickListener {
            finish()
        }

        binding.rvMenuGroup.setHasFixedSize(true)

        binding.pullRefres.setOnRefreshListener {
            mainViewModel.queryDataMenu(this@ChildMenuActivity, dataId)
            binding.pullRefres.setRefreshing(false)
        }

        binding.fabAddMenu.setOnClickListener {
            dialogCategory(0, "$dataName$code", "", dataId, "", 0, "", false, false, false)
        }
    }

    private fun calculatePercentage(value: Int, total: Int): Float {
        return (value.toFloat() / total)
    }

    private fun dialogCategory(menuId: Int, menuCode: String, menuName: String, catId: Int, flag: String, price: Int, prefs: String, getIncludeTax: Boolean, getIncludeChg: Boolean, getTaxAndChg: Boolean) {
        val dialogAddCategory = Dialog(this)
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

        val cekIncludeTax = dialogAddCategory.findViewById<CheckBox>(R.id.checkIncludeTax)
        val cekIncludeChg = dialogAddCategory.findViewById<CheckBox>(R.id.checkIncludeChg)
        val cekTaxAndChg = dialogAddCategory.findViewById<CheckBox>(R.id.checkTaxAndService)
        val clTextTaxAndService = dialogAddCategory.findViewById<ConstraintLayout>(R.id.clTextTaxAndService)
        val textTax = dialogAddCategory.findViewById<TextView>(R.id.tvTextTax)
        val textChg = dialogAddCategory.findViewById<TextView>(R.id.tvTextChg)
        val textNilaiTax = dialogAddCategory.findViewById<TextView>(R.id.tvTextNilaiTax)
        val textNilaiChg = dialogAddCategory.findViewById<TextView>(R.id.tvTextNilaiChg)
        val textNilaiNet = dialogAddCategory.findViewById<TextView>(R.id.tvTextNilaiNetTaxAndChg)

        textTax.text = "Tax $getTax"
        textChg.text = "Chg $getChg"

        if (getTaxAndChg){
            cekTaxAndChg.isChecked = true
            cekIncludeTax.isEnabled = false
            cekIncludeChg.isEnabled = false
            clTextTaxAndService.visibility = View.VISIBLE
        }

        if (getIncludeTax){
            cekTaxAndChg.isEnabled = false
            cekIncludeTax.isChecked = true
            clTextTaxAndService.visibility = View.VISIBLE
        }

        if (getIncludeChg){
            cekTaxAndChg.isEnabled = false
            cekIncludeChg.isChecked = true
            clTextTaxAndService.visibility = View.VISIBLE
        }

        cekIncludeTax.setOnCheckedChangeListener { _, checked ->
            if (checked){
                cekTaxAndChg.isEnabled = false
                val priceText = etPriceName.text.toString().toIntOrNull() ?: 0
                textNilaiTax.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getTax).roundToInt())
                textNilaiNet.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(priceText)
                clTextTaxAndService.visibility = View.VISIBLE
            }else{
                textNilaiTax.text = "0"
                if(!cekIncludeChg.isChecked){
                    cekTaxAndChg.isEnabled = true
                    textNilaiTax.text = "0"
                    textNilaiChg.text = "0"
                    textNilaiNet.text = etPriceName.text.toString()
                    clTextTaxAndService.visibility = View.GONE
                }
            }
        }

        cekIncludeChg.setOnCheckedChangeListener { _, checked ->
            if (checked){
                cekTaxAndChg.isEnabled = false
                clTextTaxAndService.visibility = View.VISIBLE
                val priceText = etPriceName.text.toString().toIntOrNull() ?: 0
                textNilaiChg.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getChg).roundToInt())
                textNilaiNet.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(priceText)
            }else{
                textNilaiChg.text = "0"
                if(!cekIncludeTax.isChecked){
                    cekTaxAndChg.isEnabled = true
                    textNilaiTax.text = "0"
                    textNilaiChg.text = "0"
                    textNilaiNet.text = etPriceName.text.toString()
                    clTextTaxAndService.visibility = View.GONE
                }
            }
        }

        cekTaxAndChg.setOnCheckedChangeListener { _, checked ->
            if (checked){
                cekIncludeTax.isEnabled = false
                cekIncludeChg.isEnabled = false
                val priceText = etPriceName.text.toString().toIntOrNull() ?: 0
                textNilaiTax.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getTax).roundToInt())
                textNilaiChg.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getChg).roundToInt())
                textNilaiNet.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText + (priceText * getTax )+ (priceText * getChg)).roundToInt())
                clTextTaxAndService.visibility = View.VISIBLE
            }else{
                cekIncludeTax.isEnabled = true
                cekIncludeChg.isEnabled = true
                textNilaiTax.text = "0"
                textNilaiChg.text = "0"
                textNilaiNet.text = etPriceName.text.toString()
                clTextTaxAndService.visibility = View.GONE
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val priceText = etPriceName.text.toString().toIntOrNull() ?: 0
                if (cekTaxAndChg.isChecked){
                    textNilaiTax.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getTax).roundToInt())
                    textNilaiChg.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getChg).roundToInt())
                    textNilaiNet.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText + (priceText * getTax )+ (priceText * getChg)).roundToInt())
                }else{
                    if (cekIncludeTax.isChecked){
                        textNilaiTax.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getTax).roundToInt())
                    }else{
                        textNilaiTax.text = "0"
                    }

                    if (cekIncludeChg.isChecked){
                        textNilaiChg.text = NumberFormat.getNumberInstance(Locale.getDefault()).format((priceText * getChg).roundToInt())
                    }else{
                        textNilaiChg.text = "0"
                    }

                    textNilaiNet.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(priceText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etPriceName.addTextChangedListener(textWatcher)

        btnBatal.text = "Batal"
        tvTitle.text = "Tambah Menu"

        etPref.imeOptions = EditorInfo.IME_ACTION_DONE
        etPref.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val chip = Chip(this)
                chip.text = etPref.text.toString()

                chip.isCloseIconVisible = true

                chip.setOnCloseIconClickListener {
                    chipGroupPref.removeView(chip)
                }

                chipGroupPref.addView(chip)

                etPref.setText("")
                return@OnEditorActionListener true
            }
            false
        })

        var selectedFlag = ""
        val adapterFlag = ArrayAdapter<String>(this, R.layout.spinner_item, arrayFlag)
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

        if (menuId > 0) {
            tvTitle.text = "Edit $menuName"
            etMenuName.setText(menuName)
            etPriceName.setText(price.toString())
            prefs.split(",").forEach {
                if (it.trim() != ""){
                    val chip = Chip(this)

                    chip.text = it.trim()

                    chip.isCloseIconVisible = true

                    chip.setOnCloseIconClickListener{
                        chipGroupPref.removeView(chip)
                    }
                    chipGroupPref.addView(chip)
                }
            }
            spinnerFlag.setSelection(adapterFlag.getPosition(flag))
        }

        btnClose.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnBatal.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnSimpan.setOnClickListener {
            val priceText = etPriceName.text.toString().toIntOrNull() ?: 0
            val taxPrice = if (cekTaxAndChg.isChecked){ (priceText * getTax).roundToInt() }else{ if (cekIncludeTax.isChecked){ (priceText * getTax).roundToInt() }else{ 0 } }
            val chgPrice = if (cekTaxAndChg.isChecked){ (priceText * getChg).roundToInt() }else{ if (cekIncludeChg.isChecked){ (priceText * getChg).roundToInt() }else{ 0 } }
            val netPrice = if (cekTaxAndChg.isChecked){(priceText + (priceText * getTax ) + (priceText * getChg)).roundToInt()}else{priceText}

            if (etMenuName.text.toString().isEmpty() || etPriceName.text.toString().isEmpty()){
                Toast.makeText(this, "Menu atau Harga tidak boleh kosong ya..", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {

                    var prefChip = ""

                    for (i in 0 until chipGroupPref.childCount) {
                        val chip = (chipGroupPref.getChildAt(i) as Chip).text.toString()
                        prefChip += "${chip},"
                    }

                    val resultSimpan = withContext(Dispatchers.IO) {
                        if (menuId > 0) {
                            queryUpdateMenu(
                                menuId,
                                menuCode,
                                etMenuName.text.toString(),
                                catId,
                                selectedFlag,
                                etPriceName.text.toString().toInt(),
                                prefChip,
                                taxPrice,
                                chgPrice,
                                if(cekIncludeTax.isChecked){ 1 } else{ 0 },
                                if(cekIncludeChg.isChecked){ 1 } else{ 0 },
                                if(cekTaxAndChg.isChecked){ 1 } else{ 0 },
                                netPrice
                            )
                        }
                        else {
                            querySaveMenu(
                                menuCode,
                                etMenuName.text.toString(),
                                catId,
                                selectedFlag,
                                etPriceName.text.toString().toInt(),
                                prefChip,
                                taxPrice,
                                chgPrice,
                                if(cekIncludeTax.isChecked){ 1 } else{ 0 },
                                if(cekIncludeChg.isChecked){ 1 } else{ 0 },
                                if(cekTaxAndChg.isChecked){ 1 } else{ 0 },
                                netPrice
                            )
                        }
                    }
                    when (resultSimpan) {
                        "Berhasil" -> {
                            dialogAddCategory.dismiss()
                            mainViewModel.queryDataMenu(this@ChildMenuActivity, dataId)
                        }

                        "Gagal" -> {
                            Toast.makeText(
                                this@ChildMenuActivity,
                                "Gagal nyimpan guys!!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Toast.makeText(this@ChildMenuActivity, resultSimpan, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }

        dialogAddCategory.show()
        dialogAddCategory.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialogAddCategory.window?.setGravity(Gravity.CENTER)
    }

    private fun showItems() {
        lifecycleScope.launch {
            code = "000".toInt() + mMenu.size
            binding.rvMenuGroup.adapter = menuAdapter
            menuAdapter.setOnItemClickRemoveCallback(object :
                MenuAdapter.OnItemClickRemoveCallback {
                override fun onItemClicked(menuGroup: Menu) {
                    val builder = AlertDialog.Builder(this@ChildMenuActivity)
                    builder.setTitle(menuGroup.menuName + " Ini mau dihapus cok?")

                    builder.setPositiveButton("Yoi") { p, _ ->
                        lifecycleScope.launch {
                            val resultDelete = withContext(Dispatchers.IO) {
                                queryHapusMenu(menuGroup.menuId!!.toInt())
                            }
                            when (resultDelete) {
                                "Berhasil" -> {
                                    p.dismiss()
                                    mainViewModel.queryDataMenu(this@ChildMenuActivity, dataId)
                                }

                                "Gagal" -> {
                                    Toast.makeText(
                                        this@ChildMenuActivity,
                                        "Gagal dihapus guys!!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                else -> {
                                    Toast.makeText(
                                        this@ChildMenuActivity,
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

            menuAdapter.setOnItemClickEditCallback(object :
                MenuAdapter.OnItemClickEditCallback {
                override fun onItemClicked(menuGroup: Menu) {
                    dialogCategory(
                        menuGroup.menuId!!.toInt(),
                        menuGroup.menuCode.toString(),
                        menuGroup.menuName.toString(),
                        menuGroup.catId!!.toInt(),
                        menuGroup.flag.toString(),
                        menuGroup.price!!.toInt(),
                        menuGroup.prefs.toString(),
                        menuGroup.includeTax!!.toInt() == 1,
                        menuGroup.includeChg!!.toInt() == 1,
                        menuGroup.taxAndChg!!.toInt() == 1
                    )
                }
            })
        }
    }

    private fun querySaveMenu(
        menuCode: String,
        menuName: String,
        catId: Int,
        flag: String,
        price: Int,
        prefs: String,
        tax: Int,
        chg: Int,
        includeTax: Int,
        includeChg: Int,
        taxAndChg: Int,
        netPrice: Int
    ): String {
        val conn = conect.connection(this)
        Log.e("Simpan", "$menuCode, $menuName, $catId, $flag, $price, $prefs")
        if (conn != null) {
            return try {
                val query =
                    "EXEC USP_J_Menu_Update @MenuCode = ?, @MenuName = ?, @CatId = ?, @MFlags = ?, @Price = ?, @Prefs = ?, @By = ?, @Tax = ?, @ServiceChg = ?, @IncludeTax = ?, @IncludeChg = ?, @TaxAndService = ?, @NetPrice = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, menuCode)
                pp.setString(2, menuName)
                pp.setInt(3, catId)
                pp.setString(4, flag)
                pp.setInt(5, price)
                pp.setString(6, prefs)
                pp.setString(7, sessionLogin.getUserLogin())
                pp.setInt(8, tax)
                pp.setInt(9, chg)
                pp.setInt(10, includeTax)
                pp.setInt(11, includeChg)
                pp.setInt(12, taxAndChg)
                pp.setInt(13, netPrice)
                pp.execute()
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdateMenu(
        menuId: Int,
        menuCode: String,
        menuName: String,
        catId: Int,
        flag: String,
        price: Int,
        prefs: String,
        tax: Int,
        chg: Int,
        includeTax: Int,
        includeChg: Int,
        taxAndChg: Int,
        netPrice: Int
    ): String {
        val conn = conect.connection(this)
        if (conn != null) {
            return try {
                val query =
                    "EXEC USP_J_Menu_Update @MenuCode = ?, @MenuName = ?, @CatId = ?, @MFlags = ?, @Price = ?, @Prefs = ?, @By = ?, @MenuID = ?, @Tax = ?, @ServiceChg = ?, @IncludeTax = ?, @IncludeChg = ?, @TaxAndService = ?, @NetPrice = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, menuCode)
                pp.setString(2, menuName)
                pp.setInt(3, catId)
                pp.setString(4, flag)
                pp.setInt(5, price)
                pp.setString(6, prefs)
                pp.setString(7, sessionLogin.getUserLogin())
                pp.setInt(8, menuId)
                pp.setInt(9, tax)
                pp.setInt(10, chg)
                pp.setInt(11, includeTax)
                pp.setInt(12, includeChg)
                pp.setInt(13, taxAndChg)
                pp.setInt(14, netPrice)
                pp.execute()
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusMenu(menuId: Int): String {
        val conn = conect.connection(this)
        if (conn != null) {
            return try {
                val query = "EXEC USP_J_Menu_Delete @MenuID = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, menuId)
                pp.execute()
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryDataMenu(catId: Int): String {
        val conn = conect.connection(this)
        mMenu = mutableListOf()
        if (conn != null) {
            return try {
                val query = "SELECT * FROM J_Menu WHERE CatId = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, catId)
                val rs = pp.executeQuery()
                mMenu.clear()
                while (rs.next()) {
                    val menu = Menu()
                    menu.menuId = rs.getInt("MenuId")
                    menu.menuName = rs.getString("MenuName")
                    menu.menuCode = rs.getString("MenuCode")
                    menu.catId = rs.getInt("CatId")
                    menu.price = rs.getInt("Price")
                    menu.flag = rs.getString("MFlags")
                    menu.prefs = rs.getString("Prefs")
                    mMenu.add(menu)
                }
                "Berhasil"
            } catch (e: SQLException) {
                e.toString()
            }
        }
        return "Gagal"
    }
}