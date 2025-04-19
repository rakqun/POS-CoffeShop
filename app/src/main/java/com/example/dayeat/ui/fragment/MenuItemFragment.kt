package com.example.dayeat.ui.fragment

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.ListOrderMenuAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentMenuItemBinding
import com.example.dayeat.db.DbContract
import com.example.dayeat.db.DbQuery
import com.example.dayeat.model.OrderItems
import com.example.dayeat.mvvm.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MenuItemFragment : Fragment() {

    companion object {
        const val ARG_SECTION_NUMBER = "section_number"
    }

    private var _binding : FragmentMenuItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var listOrderMenuAdapter: ListOrderMenuAdapter

    private lateinit var connect: Conect
    private lateinit var dbQuery: DbQuery
    private lateinit var listMenu: MutableList<OrderItems>

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMenuItemBinding.inflate(inflater, container, false)

        dbQuery = DbQuery.getInstance(requireContext())
        connect = Conect()

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        showList()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showList(){
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.shimmerRecyclerView.layoutManager = GridLayoutManager(context, 2)
        }else{
            binding.shimmerRecyclerView.layoutManager = GridLayoutManager(context, 3)
        }
        binding.shimmerRecyclerView.setHasFixedSize(false)
        val index = arguments?.getInt(ARG_SECTION_NUMBER, 0)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO){
                loadMenu(index!!.toInt())
            }
            when (result){
                "Berhasil" -> {
                    listOrderMenuAdapter = ListOrderMenuAdapter(listMenu)
                    binding.shimmerRecyclerView.adapter = listOrderMenuAdapter
                    listOrderMenuAdapter.setOnItemClickCallback(object : ListOrderMenuAdapter.OnItemClickCallback{
                        override fun onItemClicked(items: OrderItems) {
                            val progressBar = ProgressDialog(context)
                            progressBar.setMessage("Sebentar cok..")
                            progressBar.setCanceledOnTouchOutside(false)
                            progressBar.show()

                            lifecycleScope.launch {
                                val result2 = withContext(Dispatchers.IO){
                                    menuStatus(items.id!!.toInt())
                                }
                                when (result2) {
                                    "0" -> {
                                        progressBar.dismiss()
                                        val cv = ContentValues()
                                        cv.put(DbContract.baseColumns.ID, UUID.randomUUID().toString())
                                        cv.put(DbContract.baseColumns.ID_PRODUCT, items.id)
                                        cv.put(DbContract.baseColumns.MENU_ID_PRODUCT, items.menuId)
                                        cv.put(DbContract.baseColumns.NAME_PRODUCT, items.nama)
                                        cv.put(DbContract.baseColumns.HARGA_PRODUCT, items.harga)
                                        cv.put(DbContract.baseColumns.QUANTY_PRODUCT, 1)
                                        cv.put(DbContract.baseColumns.NOTE_PRODUCT, "")
                                        cv.put(DbContract.baseColumns.TOTAL_PRODUCT, items.harga)
                                        cv.put(DbContract.baseColumns.DATE_PRODUCT, formateDate())
                                        lifecycleScope.launch{
                                            val resultCek = withContext(Dispatchers.IO){
                                                dbQuery.cekQueryForPesanan(items.id.toString())
                                            }

                                            if (resultCek == "1"){
                                                val resultInsert = withContext(Dispatchers.IO){
                                                    dbQuery.insert(cv)
                                                }
                                                if (resultInsert > 0){
                                                    val post = withContext(Dispatchers.IO){
                                                        dbQuery.getItemsProduc()
                                                    }
                                                    mainViewModel.orderList.postValue(post)
                                                }else{
                                                    Snackbar.make(binding.root, "Gagal Ditambahkan!", Snackbar.LENGTH_SHORT).show()
                                                }
                                            }else{
                                                val resultGetUid = withContext(Dispatchers.IO){
                                                    dbQuery.queryGetUID(items.id.toString())
                                                }
                                                val resultUpdate = withContext(Dispatchers.IO){
                                                    val qty = dbQuery.queryGetQty(resultGetUid)
                                                    val cvUpdate = ContentValues()
                                                    cvUpdate.put(DbContract.baseColumns.QUANTY_PRODUCT, qty + 1)
                                                    cvUpdate.put(DbContract.baseColumns.TOTAL_PRODUCT, (qty + 1) * items.harga!!)
                                                    return@withContext dbQuery.update(resultGetUid, cvUpdate)
                                                }

                                                if (resultUpdate > 0){
                                                    val post = withContext(Dispatchers.IO){
                                                        dbQuery.getItemsProduc()
                                                    }
                                                    mainViewModel.orderList.postValue(post)
                                                }
                                            }
                                        }
                                    }
                                    "1" -> {
                                        progressBar.dismiss()
                                        Snackbar.make(binding.root, "SOLD OUT", Snackbar.LENGTH_SHORT).show()
                                        showList()
                                    }
                                    else -> {
                                        progressBar.dismiss()
                                        Snackbar.make(binding.root, result2, Snackbar.LENGTH_SHORT).show()
                                        showList()
                                    }
                                }
                            }
                        }
                    })
                }
                "Gagal" -> {
                    Snackbar.make(binding.root, "Gagal menampilkan menu!!", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    Snackbar.make(binding.root, result, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formateDate():String{
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    private fun menuStatus(id: Int): String{
        val conn = connect.connection(context)
        var result = ""
        if (conn != null){
            return try {
                val query = "EXEC USP_J_MenuStatus_Disable @MenuID = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, id)
                val rs = st.executeQuery()
                while (rs.next()){
                    result = rs.getString("TodayDisabled")
                }
                result
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return result
    }

    private fun loadMenu(position: Int): String{
        listMenu = mutableListOf()
        val conn = connect.connection(context)
        if (conn != null){
            try {
                val query = "EXEC USP_J_Mobile_QueryStatic @Action = 'Menu', @CatId = ?"
                val st = conn.prepareStatement(query)
                st.setInt(1, position)
                val rs = st.executeQuery()
                listMenu.clear()
                while (rs.next()){
                    if (rs.getInt("HideInOrder") != 1){
                        val produc = OrderItems()
                        produc.id = rs.getInt("MenuID")
                        produc.menuId = rs.getString("MenuCode")
                        produc.nama = rs.getString("MenuName")
                        if (rs.getInt("NetPrice") > 0){
                            produc.harga = rs.getInt("NetPrice")
                        }else{
                            produc.harga = rs.getInt("Price")
                        }
                        produc.todayDisable = rs.getInt("TodayDisabled")
                        listMenu.add(produc)
                    }
                }
                return "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return e.toString()
            }
        }
        return "Gagal"
    }
}