package com.example.dayeat.mvvm

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dayeat.conn.Conect
import com.example.dayeat.db.DbContract
import com.example.dayeat.db.DbQuery
import com.example.dayeat.model.CLosing
import com.example.dayeat.model.Customer
import com.example.dayeat.model.Konfigurasi
import com.example.dayeat.model.Menu
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.Pay
import com.example.dayeat.model.Product
import com.example.dayeat.model.SalesItem
import com.example.dayeat.model.Table
import com.example.dayeat.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Locale

class MainViewModel():ViewModel() {

    val orderList = MutableLiveData<MutableList<Product>>()
    private val mMenu = MutableLiveData<MutableList<Menu>>()
    private val mMethod = MutableLiveData<MutableList<Konfigurasi>>()
    private val mMenuGroup = MutableLiveData<MutableList<MenuGroup>>()
    private val mTable = MutableLiveData<MutableList<Table>>()
    private val mPay = MutableLiveData<MutableList<Pay>>()
    private val mUser = MutableLiveData<MutableList<UserAccount>>()
    private val mCustomer = MutableLiveData<MutableList<Customer>>()
    private val mClosing = MutableLiveData<MutableList<CLosing>>()
    val mProduct = MutableLiveData<MutableList<SalesItem>>()

    fun getClosing(): LiveData<MutableList<CLosing>>{
        return mClosing
    }

    fun getCustomer(): LiveData<MutableList<Customer>>{
        return mCustomer
    }

    fun getProduct(): LiveData<MutableList<SalesItem>>{
        return mProduct
    }

    fun getOrders(): LiveData<MutableList<Product>> {
        return orderList
    }

    fun getMenu(): LiveData<MutableList<Menu>> {
        return mMenu
    }

    fun getMethod(): LiveData<MutableList<Konfigurasi>> {
        return mMethod
    }

    fun getMenuGroup(): LiveData<MutableList<MenuGroup>> {
        return mMenuGroup
    }

    fun getTable(): LiveData<MutableList<Table>> {
        return mTable
    }

    fun getPay(): LiveData<MutableList<Pay>> {
        return mPay
    }

    fun getUser(): LiveData<MutableList<UserAccount>>{
        return mUser
    }

    fun queryClosing(context: Context){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listCLosing:MutableList<CLosing> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "EXEC USP_J_Closing_QueryHistory_ForMobile"
                    val pp = conn.createStatement()
                    val rs = pp.executeQuery(query)
                    listCLosing.clear()
                    while (rs.next()){
                        val closing = CLosing().apply {
                            this.closingID = rs.getInt("ClosingID")
                            this.closingDate = rs.getString("ClosingDate")
                            this.closingBy = rs.getString("ClosedBy")
                            this.closingQty = rs.getInt("ClosingQty")
                            this.closingTotal = rs.getInt("ClosingTotal")
                        }
                        listCLosing.add(closing)
                    }
                    withContext(Dispatchers.Main){
                        mClosing.value = listCLosing
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryDataCustomer(context: Context){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listCustomer:MutableList<Customer> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "SELECT * FROM J_Cust"
                    val pp = conn.createStatement()
                    val rs = pp.executeQuery(query)
                    listCustomer.clear()
                    while (rs.next()){
                        val customer = Customer()
                        customer.custId = rs.getInt("CustID")
                        customer.regNo = rs.getString("RegNo")
                        customer.cName = rs.getString("CName")
                        customer.addr = rs.getString("Addr")
                        customer.phone = rs.getString("Phone")
                        customer.email = rs.getString("Email")
                        customer.ktp = rs.getString("KTP")
                        customer.cDisc = rs.getInt("CDisc")
                        customer.lastMod = rs.getString("LastMod")
                        customer.lastModBy = rs.getString("LastModBy")
                        customer.remarks = rs.getString("Remarks")
                        customer.birtDay = rs.getString("BirthDate")
                        customer.cFlags = rs.getString("CFlags")
                        listCustomer.add(customer)
                    }
                    withContext(Dispatchers.Main){
                        mCustomer.value = listCustomer
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryDataTable(context: Context){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listTable:MutableList<Table> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "SELECT * FROM J_Table"
                    val pp = conn.createStatement()
                    val rs = pp.executeQuery(query)
                    listTable.clear()
                    while (rs.next()){
                        val table = Table()
                        table.tableId = rs.getInt("TableID")
                        table.tableCode = rs.getString("TableCode")
                        table.tGroupId = rs.getInt("TGroupID")
                        table.tFlags = rs.getString("TFlags")
                        listTable.add(table)
                    }
                    withContext(Dispatchers.Main){
                        mTable.value = listTable
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryDataCat(context: Context){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listMenuGroup:MutableList<MenuGroup> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "SELECT * FROM J_Cat"
                    val pp = conn.createStatement()
                    val rs = pp.executeQuery(query)
                    listMenuGroup.clear()
                    while (rs.next()){
                        val getCatName = rs.getString("CatName")
                        if (getCatName != ""){
                            val menuGroup = MenuGroup()
                            menuGroup.catId = rs.getInt("CatId")
                            menuGroup.catName = getCatName
                            menuGroup.catDept = rs.getString("Dept")
                            menuGroup.catFlag = rs.getString("CatFlags")
                            listMenuGroup.add(menuGroup)
                        }
                    }
                    withContext(Dispatchers.Main){
                        mMenuGroup.value = listMenuGroup
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryDataMenu(context: Context, catId: Int){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listMenu:MutableList<Menu> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "EXEC USP_J_Menu_Query @CatId = ?"
                    val pp = conn.prepareStatement(query)
                    pp.setInt(1, catId)
                    val rs = pp.executeQuery()
                    listMenu.clear()
                    while (rs.next()){
                        val menu = Menu()
                        menu.menuId = rs.getInt("MenuId")
                        menu.menuName = rs.getString("MenuName")
                        menu.menuCode = rs.getString("MenuCode")
                        menu.catId = rs.getInt("CatId")
                        menu.price = rs.getInt("Price")
                        menu.netPrice = rs.getInt("NetPrice")
                        menu.flag = rs.getString("MFlags")
                        menu.prefs = rs.getString("Prefs")
                        menu.includeTax = rs.getInt("IncludeTax")
                        menu.includeChg = rs.getInt("IncludeChg")
                        menu.taxAndChg = rs.getInt("TaxAndService")
                        listMenu.add(menu)
                    }
                    withContext(Dispatchers.Main){
                        mMenu.value = listMenu
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryDataMethod(context: Context){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listMenu:MutableList<Konfigurasi> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "SELECT RecordId, Code, TName, Description, CustomV1, CustomV2, CustomV3, CustomV4, CustomStr1, CustomStr2, CAST(CustomD1 AS DATE) CustomD1, CAST(CustomD2 AS DATE) CustomD2  FROM J_Lookup"
                    val pp = conn.createStatement()
                    val rs = pp.executeQuery(query)
                    listMenu.clear()
                    while (rs.next()){
                        val menu = Konfigurasi()
                        menu.recordId = rs.getInt("RecordId")
                        menu.code = rs.getString("Code")
                        menu.tName = rs.getString("TName")
                        menu.description = rs.getString("Description")
                        menu.customV1 = rs.getInt("CustomV1")
                        menu.customV2 = rs.getInt("CustomV2")
                        menu.customV3 = rs.getInt("CustomV3")
                        menu.customV4 = rs.getInt("CustomV4")
                        menu.customStr1 = rs.getString("CustomStr1")
                        menu.customStr2 = rs.getString("CustomStr2")
                        menu.customD1 = rs.getString("CustomD1")
                        menu.customD2 = rs.getString("CustomD2")
                        listMenu.add(menu)
                    }
                    withContext(Dispatchers.Main){
                        mMethod.value = listMenu
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }

    fun queryPay(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val conn = Conect().connection(context)
            val list: MutableList<Pay> = mutableListOf()
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForOnGoing"
                    val preparedStatement = conn.createStatement()
                    val rs = preparedStatement.executeQuery(query)
                    list.clear()
                    while (rs.next()){
                        val pay = Pay()
                        pay.salesNo = rs.getString("SalesNo")
                        pay.tableId = rs.getInt("TableID")
                        pay.cName = rs.getString("CName")
                        pay.noMeja = rs.getString("TableCode")
                        pay.date = rs.getString("TableLastMod")
                        pay.detail = rs.getString("DetailJson")
                        pay.salesId = rs.getInt("SalesID")
                        pay.group = rs.getString("GroupName")
                        pay.lastMod = rs.getString("LastModBy")
                        pay.pax = rs.getInt("Pax")
                        pay.promo = rs.getString("Promo")
                        pay.disc = rs.getInt("Disc")
//                        pay.recorded = rs.getString("RecordedDateTime")
                        pay.qty = rs.getInt("Qty")
                        pay.total = rs.getInt("NetTotal")
                        list.add(pay)
                    }
                    withContext(Dispatchers.Main) {
                        mPay.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun queryComplet(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val conn = Conect().connection(context)
            val list: MutableList<Pay> = mutableListOf()
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForHistory"
                    val preparedStatement = conn.createStatement()
                    val rs = preparedStatement.executeQuery(query)
                    list.clear()
                    while (rs.next()){
                        val tanggalCetak = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(rs.getString("ClosedDateTime"))!!
                        )
                        val waktuCetak = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(rs.getString("ClosedDateTime"))
                            ?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) }
                        val pay = Pay()
                        pay.salesNo = rs.getString("SalesNo")
                        pay.tableId = rs.getInt("TableID")
                        pay.cName = rs.getString("CName")
                        pay.noMeja = rs.getString("TableCode")
                        pay.date = rs.getString("TableLastMod")
                        pay.detail = rs.getString("DetailJson")
                        pay.salesId = rs.getInt("SalesID")
                        pay.group = rs.getString("GroupName")
                        pay.lastMod = rs.getString("LastModBy")
                        pay.pax = rs.getInt("Pax")
                        pay.promo = rs.getString("Promo")
                        pay.disc = rs.getInt("Disc")
                        pay.amountPaid = rs.getInt("TotalPaid")
                        pay.completBy = rs.getString("ClosedBy")
                        pay.completDate = tanggalCetak
                        pay.completTime = waktuCetak
                        pay.method = rs.getString("Method")
//                        pay.recorded = rs.getString("RecordedDateTime")
                        pay.qty = rs.getInt("Qty")
                        pay.total = rs.getInt("NetTotal")
                        list.add(pay)
                    }
                    withContext(Dispatchers.Main) {
                        mPay.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun querySales(context: Context, startDate: String, endDate: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val conn = Conect().connection(context)
            val list: MutableList<Pay> = mutableListOf()
            if (conn != null){
                try {
                    val query = "EXEC USP_J_Sales_QueryForComplet @StartDate = ?, @EndDate = ?"
                    val preparedStatement = conn.prepareStatement(query)
                    preparedStatement.setString(1, startDate)
                    preparedStatement.setString(2, endDate)
                    val rs = preparedStatement.executeQuery()
                    list.clear()
                    while (rs.next()){
                        val pay = Pay()
                        pay.noMeja = rs.getString("TableCode")
                        pay.date = rs.getString("TableLastMod")
                        pay.detail = rs.getString("DetailJson")
                        pay.salesId = rs.getInt("SalesID")
                        pay.group = rs.getString("GroupName")
                        pay.lastMod = rs.getString("LastModBy")
                        pay.recorded = rs.getString("RecordedDateTime")
                        pay.qty = rs.getInt("Qty")
                        pay.total = rs.getInt("Total")
                        pay.method = rs.getString("TBMethod")
                        list.add(pay)
                    }
                    withContext(Dispatchers.Main) {
                        mPay.value = list
                    }
                }catch (e: SQLException){
                    e.printStackTrace()
                }
            }
        }
    }

    fun queryDataUser(context: Context){
        GlobalScope.launch(Dispatchers.IO + Job()) {
            val conn = Conect().connection(context)
            val listUser:MutableList<UserAccount> = mutableListOf()
            if (conn!= null){
                try {
                    val query = "SELECT * FROM J_User"
                    val pp = conn.createStatement()
                    val rs = pp.executeQuery(query)
                    listUser.clear()
                    while (rs.next()){
                        val userAccount = UserAccount()
                        userAccount.userId = rs.getInt("UserID")
                        userAccount.userName = rs.getString("UserName")
                        userAccount.mobileRole = rs.getString("MobileRole")
                        userAccount.pin = rs.getString("DesktopPIN")
                        userAccount.fullName = rs.getString("FullName")
                        listUser.add(userAccount)
                    }
                    withContext(Dispatchers.Main){
                        mUser.value = listUser
                    }
                }catch (e: SQLException){
                    Log.e("error sql", e.toString())
                }
            }
        }
    }
}