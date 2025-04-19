package com.example.dayeat.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper (context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "DRINK"
        const val DATABASE_VERSION = 1

        private const val CREATE_TABLE_PRODUCT = "CREATE TABLE ${DbContract.baseColumns.TABLE_NAME_PRODUCT}" +
                " (${DbContract.baseColumns.ID} TEXT NOT NULL," +
                " ${DbContract.baseColumns.ID_PRODUCT} INTEGER NOT NULL," +
                " ${DbContract.baseColumns.MENU_ID_PRODUCT} TEXT NOT NULL," +
                " ${DbContract.baseColumns.NAME_PRODUCT} TEXT NOT NULL," +
                " ${DbContract.baseColumns.HARGA_PRODUCT} INTEGER NOT NULL," +
//                " ${DbContract.baseColumns.TAX_PRODUCT} INTEGER NOT NULL," +
//                " ${DbContract.baseColumns.CHG_PRODUCT} INTEGER NOT NULL," +
                " ${DbContract.baseColumns.NOTE_PRODUCT} TEXT NOT NULL," +
                " ${DbContract.baseColumns.QUANTY_PRODUCT} INTEGER NOT NULL,"+
                " ${DbContract.baseColumns.TOTAL_PRODUCT} INTEGER NOT NULL," +
                " ${DbContract.baseColumns.DATE_PRODUCT} TEXT NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_PRODUCT)
    }

    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        db.execSQL("DROP TABLE IF EXISTS $CREATE_TABLE_PRODUCT")
        onCreate(db)
    }
}