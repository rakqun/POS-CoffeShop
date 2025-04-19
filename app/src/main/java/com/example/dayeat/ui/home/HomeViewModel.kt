package com.example.dayeat.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dayeat.conn.Conect
import com.example.dayeat.model.Category
import com.example.dayeat.model.MenuName
import com.example.dayeat.model.Payment
import com.example.dayeat.model.Penjualan
import com.example.dayeat.model.TimeSales
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class HomeViewModel : ViewModel() {

    private val _penjualan = MutableLiveData<MutableList<Penjualan>>()
    val gePenjualan: LiveData<MutableList<Penjualan>> = _penjualan

    private val _payment = MutableLiveData<MutableList<Payment>>()
    val getPayment: LiveData<MutableList<Payment>> = _payment

    private val _category = MutableLiveData<MutableList<Category>>()
    val getCategory: LiveData<MutableList<Category>> = _category

    private val _time = MutableLiveData<MutableList<TimeSales>>()
    val getTimeSale: LiveData<MutableList<TimeSales>> = _time

    private val _menu = MutableLiveData<MutableList<MenuName>>()
    val getMenuName: LiveData<MutableList<MenuName>> = _menu

    fun queryGetPenjualan(context: Context, startDate: String, endDate:String){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listPenjualan = mutableListOf<Penjualan>()
            if (conn!= null){
                try {
                    val query = "SELECT SUM(JSNOTVOID.Qty) NotQty, SUM(JSNOTVOID.NetTotal) NotTotal, SUM(JSVOID.Qty) VQty, SUM(JSVOID.NetTotal) VTotal \n" +
                            "FROM J_Sales JS\n" +
                            "LEFT JOIN (SELECT * FROM J_Sales WHERE VoidDateTime IS NULL) JSNOTVOID ON JS.SalesID = JSNOTVOID.SalesID\n" +
                            "LEFT JOIN (SELECT * FROM J_Sales WHERE VoidDateTime IS NOT NULL) JSVOID ON JS.SalesID = JSVOID.SalesID\n" +
                            "WHERE CAST(JS.ClosedDateTime AS DATE) BETWEEN ? AND ? AND JS.ClosedDateTime IS NOT NULL \n"
                    val pp = conn.prepareStatement(query)
                    pp.setString(1, startDate)
                    pp.setString(2, endDate)
                    val rs = pp.executeQuery()
                    while (rs.next()){
                        val penjualan = Penjualan()
                        penjualan.notQty = rs.getInt("NotQty")
                        penjualan.notTotal = rs.getInt("NotTotal")
                        penjualan.vQty = rs.getInt("VQty")
                        penjualan.vTotal = rs.getInt("VTotal")
                        listPenjualan.add(penjualan)
                    }
                    withContext(Dispatchers.Main){
                        _penjualan.value = listPenjualan
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryGetPayment(context: Context, startDate: String, endDate:String){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listPayment = mutableListOf<Payment>()
            if (conn!= null){
                try {
                    val query = "SELECT SP.Method, SUM(NetTotal) Total FROM J_Sales S\n" +
                            "JOIN J_SalesPayment SP ON SP.SalesID = S.SalesID\n" +
                            "WHERE VoidDateTime IS NULL AND S.ClosedDateTime IS NOT NULL AND CAST(S.ClosedDateTime AS DATE) BETWEEN ? AND ?\n" +
                            "GROUP BY SP.Method"
                    val pp = conn.prepareStatement(query)
                    pp.setString(1, startDate)
                    pp.setString(2, endDate)
                    val rs = pp.executeQuery()
                    while (rs.next()){
                        val payment = Payment()
                        payment.jenis = rs.getString("Method")
                        payment.total = rs.getInt("Total")
                        listPayment.add(payment)
                    }
                    withContext(Dispatchers.Main){
                        _payment.value = listPayment
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryGetCategory(context: Context, startDate: String, endDate:String){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listCategory = mutableListOf<Category>()
            if (conn!= null){
                try {
                    val query = "SELECT C.CatName, SUM(CASE WHEN ISNULL(SI.NetTotal, 0) = 0 THEN SI.LineTotal ELSE SI.NetTotal END) Total \n" +
                            "FROM J_Sales S\n" +
                            "JOIN J_SalesItem SI ON SI.SalesID = S.SalesID\n" +
                            "JOIN J_Menu M ON M.MenuID = SI.MenuID\n" +
                            "JOIN J_Cat C ON C.CatID = M.CatID\n" +
                            "WHERE S.VoidDateTime IS NULL AND SI.VoidDateTime IS NULL AND S.ClosedDateTime IS NOT NULL AND CAST(S.ClosedDateTime AS DATE) BETWEEN ? AND ?\n" +
                            "GROUP BY C.CatName\n" +
                            "ORDER BY SUM(CASE WHEN ISNULL(SI.NetTotal, 0) = 0 THEN SI.LineTotal ELSE SI.NetTotal END) DESC"
                    val pp = conn.prepareStatement(query)
                    pp.setString(1, startDate)
                    pp.setString(2, endDate)
                    val rs = pp.executeQuery()

                    while (rs.next()){
                        val category = Category()
                        category.category = rs.getString("CatName")
                        category.total = rs.getInt("Total")
                        listCategory.add(category)
                    }
                    withContext(Dispatchers.Main){
                        _category.value = listCategory
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryGetTimeSales(context: Context, startDate: String, endDate:String){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listTime = mutableListOf<TimeSales>()
            if (conn!= null){
                try {
                    val query = "SELECT RIGHT('0' + CAST(DATEPART(HOUR, ClosedDateTime) AS VARCHAR), 2) + ':00' Time, SUM(NetTotal) NetTotal  FROM J_Sales\n" +
                            "WHERE ClosedDateTime IS NOT NULL AND VoidDateTime IS NULL AND CAST(ClosedDateTime AS DATE) BETWEEN ? AND ?\n" +
                            "GROUP BY RIGHT('0' + CAST(DATEPART(HOUR, ClosedDateTime) AS VARCHAR), 2) + ':00'"
                    val pp = conn.prepareStatement(query)
                    pp.setString(1, startDate)
                    pp.setString(2, endDate)
                    val rs = pp.executeQuery()
                    while (rs.next()){
                        val timeSales = TimeSales()
                        timeSales.time = rs.getString("Time")
                        timeSales.total = rs.getInt("NetTotal")
                        listTime.add(timeSales)
                    }
                    withContext(Dispatchers.Main){
                        _time.value = listTime
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryGetMenuName(context: Context, startDate: String, endDate:String){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listMenuName = mutableListOf<MenuName>()
            if (conn!= null){
                try {
                    val query = "SELECT M.MenuName, SUM(CASE WHEN ISNULL(SI.NetTotal, 0) = 0 THEN SI.LineTotal ELSE SI.NetTotal END) Total FROM J_Sales S\n" +
                            "JOIN J_SalesItem SI ON SI.SalesID = S.SalesID\n" +
                            "JOIN J_Menu M ON M.MenuID = SI.MenuID\n" +
                            "WHERE S.VoidDateTime IS NULL AND SI.VoidDateTime IS NULL AND S.ClosedDateTime IS NOT NULL AND CAST(S.ClosedDateTime AS DATE) BETWEEN ? AND ?\n" +
                            "GROUP BY M.MenuName\n"
                    val pp = conn.prepareStatement(query)
                    pp.setString(1, startDate)
                    pp.setString(2, endDate)
                    val rs = pp.executeQuery()
                    while (rs.next()){
                        val menu = MenuName()
                        menu.menuName = rs.getString("MenuName")
                        menu.total = rs.getInt("Total")
                        listMenuName.add(menu)
                    }
                    withContext(Dispatchers.Main){
                        _menu.value = listMenuName
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }
}