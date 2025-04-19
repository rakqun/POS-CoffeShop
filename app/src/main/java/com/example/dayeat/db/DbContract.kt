package com.example.dayeat.db

import android.provider.BaseColumns

class DbContract {

    internal class baseColumns: BaseColumns {
        companion object{
            const val TABLE_NAME_PRODUCT = "Product"
            const val ID = "id"
            const val ID_PRODUCT = "id_product"
            const val MENU_ID_PRODUCT = "menu_id_product"
            const val NAME_PRODUCT = "name_product"
            const val QUANTY_PRODUCT = "quanty_product"
            const val HARGA_PRODUCT = "harga_product"
//            const val TAX_PRODUCT = "tax_product"
//            const val CHG_PRODUCT = "chg_product"
            const val TOTAL_PRODUCT = "total_product"
            const val NOTE_PRODUCT = "note_product"
            const val DATE_PRODUCT = "date_product"
        }
    }
}