package com.example.dayeat.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.dayeat.model.Product

class DbQuery (context: Context) {

    private var db = DbHelper(context).writableDatabase

    companion object {
        private const val TABEL_NAME_PRODUCT = DbContract.baseColumns.TABLE_NAME_PRODUCT
        private var INSTANCE: DbQuery? = null

        fun getInstance(context: Context): DbQuery =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DbQuery(context)
            }
    }

    fun close() {
        db.close()

        if (db.isOpen)
            db.close()
    }

    fun insert(cv: ContentValues): Long {
        return db.insert(TABEL_NAME_PRODUCT, null, cv)
    }

    fun insertWithCek(id: String, cv: ContentValues): Long{
        if (cekQueryForPesanan(id) == "1"){
            return db.insert(TABEL_NAME_PRODUCT, null, cv)
        }
        return 0
    }

    fun update(id: String, values: ContentValues?): Int{
        return db.update(TABEL_NAME_PRODUCT, values, "${DbContract.baseColumns.ID} = ?", arrayOf(id))
    }

    fun deleteById(id: String): Int {
        return db.delete(TABEL_NAME_PRODUCT, "${DbContract.baseColumns.ID} = ?", arrayOf(id))
    }

    fun delete():Int {
        return db.delete(TABEL_NAME_PRODUCT, null, null)
    }

    private fun queryAll(): Cursor {
        return db.query(
            TABEL_NAME_PRODUCT,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    fun queryGetQty(id: String): Int{
        var a = 0
        val query = db.query(TABEL_NAME_PRODUCT,
            arrayOf(DbContract.baseColumns.QUANTY_PRODUCT),
            "${DbContract.baseColumns.ID} = ?",
            arrayOf(id),
            null,
            null,
            null)
        query.apply {
            while (moveToNext()){
                a = getInt(getColumnIndexOrThrow("quanty_product"))
            }
        }
        return a
    }

    fun cekQueryForPesanan(id: String): String{
        var a = ""
        val query = db.rawQuery("SELECT CASE WHEN COUNT(*) = 0 OR note_product <> '' THEN '1' ELSE '0' END AS Result FROM (SELECT * FROM Product WHERE id_product = ? ORDER BY date_product DESC LIMIT 1)", arrayOf(id))
        query.apply {
            while (moveToNext()){
                a = getString(getColumnIndexOrThrow("Result")) }
            return a
        }
        return a
    }

    fun queryGetUID(id: String): String{
        var a = ""
        val query = db.rawQuery("SELECT ${DbContract.baseColumns.ID} AS Result FROM (SELECT * FROM Product WHERE id_product = ? ORDER BY date_product DESC LIMIT 1)", arrayOf(id))
        query.apply {
            while (moveToNext()){
                a = getString(getColumnIndexOrThrow("Result")) }
            return a
        }
        return a
    }

    private fun queryById(id: String): Cursor {
        return db.query(
            TABEL_NAME_PRODUCT,
            null,
            "${DbContract.baseColumns.ID} = ?",
            arrayOf(id),
            null,
            null,
            null
        )
    }

    fun cekItems():Int{
        val query = db.rawQuery("SELECT COUNT(*) AS JUMLAH FROM $TABEL_NAME_PRODUCT", null)
        var jumlah = 0
        query.apply {
            while (moveToNext()){
                jumlah = getInt(getColumnIndexOrThrow("JUMLAH"))
            }
        }
        return jumlah
    }

    fun getItemsProduc():MutableList<Product>{
        val listProduc = mutableListOf<Product>()
        queryAll().apply {
            listProduc.clear()
            while (moveToNext()){
                val produc = Product()
                produc.uid = getString(getColumnIndexOrThrow("${DbContract.baseColumns.ID}"))
                produc.id = getInt(getColumnIndexOrThrow("${DbContract.baseColumns.ID_PRODUCT}"))
                produc.menuId = getString(getColumnIndexOrThrow("${DbContract.baseColumns.MENU_ID_PRODUCT}"))
                produc.nama = getString(getColumnIndexOrThrow("${DbContract.baseColumns.NAME_PRODUCT}"))
                produc.quanty = getInt(getColumnIndexOrThrow("${DbContract.baseColumns.QUANTY_PRODUCT}"))
                produc.catatan = getString(getColumnIndexOrThrow("${DbContract.baseColumns.NOTE_PRODUCT}"))
                produc.harga = getInt(getColumnIndexOrThrow("${DbContract.baseColumns.HARGA_PRODUCT}"))
//                produc.tax = getInt(getColumnIndexOrThrow("${DbContract.baseColumns.TAX_PRODUCT}"))
//                produc.chg = getInt(getColumnIndexOrThrow("${DbContract.baseColumns.CHG_PRODUCT}"))
                produc.total = getInt(getColumnIndexOrThrow("${DbContract.baseColumns.TOTAL_PRODUCT}"))
                listProduc.add(produc)
            }
        }
        return listProduc
    }
}