package com.example.dayeat.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.dayeat.ui.activity.LoginActivity

class SessionLogin(var context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    var PRIVATE_MODE = 0

    fun createLoginSession(userId: Int, nama: String, mobile:String) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_NAMA, nama)
        editor.putInt(KEY_ID, userId)
        editor.putString(KEY_ROLE, mobile)
        editor.commit()
    }

    fun createTaxChg(tax: Int, chg:Int) {
        editor.putInt(KEY_TAX, tax)
        editor.putInt(KEY_CHG, chg)
        editor.commit()
    }

    fun createCustomBill(header1:String, header2:String, header3:String, header4:String, header5:String, footer1:String, footer2:String, footer3:String, footer4:String, footer5:String) {
        editor.putString(KEY_HEADER1, header1)
        editor.putString(KEY_HEADER2, header2)
        editor.putString(KEY_HEADER3, header3)
        editor.putString(KEY_HEADER4, header4)
        editor.putString(KEY_HEADER5, header5)
        editor.putString(KEY_FOOTER1, footer1)
        editor.putString(KEY_FOOTER2, footer2)
        editor.putString(KEY_FOOTER3, footer3)
        editor.putString(KEY_FOOTER4, footer4)
        editor.putString(KEY_FOOTER5, footer5)
        editor.commit()
    }

    fun noLogin() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun logoutUser() {
        editor.clear()
        editor.commit()
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isLoggedIn(): Boolean = pref.getBoolean(IS_LOGIN, false)
    fun getUserLogin(): String = pref.getString(KEY_NAMA, "").toString()
    fun getUserIdLogin(): Int = pref.getInt(KEY_ID, 0)
    fun getMobileRole(): String = pref.getString(KEY_ROLE, "").toString()

    fun getTax(): Int = pref.getInt(KEY_TAX, 0)
    fun getChg(): Int = pref.getInt(KEY_CHG, 0)

    fun getHeader1(): String = pref.getString(KEY_HEADER1, "").toString()
    fun getHeader2(): String = pref.getString(KEY_HEADER2, "").toString()
    fun getHeader3(): String = pref.getString(KEY_HEADER3, "").toString()
    fun getHeader4(): String = pref.getString(KEY_HEADER4, "").toString()
    fun getHeader5(): String = pref.getString(KEY_HEADER5, "").toString()
    fun getFooter1(): String = pref.getString(KEY_FOOTER1, "").toString()
    fun getFooter2(): String = pref.getString(KEY_FOOTER2, "").toString()
    fun getFooter3(): String = pref.getString(KEY_FOOTER3, "").toString()
    fun getFooter4(): String = pref.getString(KEY_FOOTER4, "").toString()
    fun getFooter5(): String = pref.getString(KEY_FOOTER5, "").toString()

    companion object {
        private const val PREF_NAME = "Prefrence"
        private const val IS_LOGIN = "IsLoggedIn"
        const val KEY_NAMA = "NAMA"
        const val KEY_ID = "ID"
        const val KEY_ROLE = "ROLE"
        const val KEY_TAX = "TAX"
        const val KEY_CHG = "CHG"

        const val KEY_HEADER1 = "header1"
        const val KEY_HEADER2 = "header2"
        const val KEY_HEADER3 = "header3"
        const val KEY_HEADER4 = "header4"
        const val KEY_HEADER5 = "header5"
        const val KEY_FOOTER1 = "footer1"
        const val KEY_FOOTER2 = "footer2"
        const val KEY_FOOTER3 = "footer3"
        const val KEY_FOOTER4 = "footer4"
        const val KEY_FOOTER5 = "footer5"
    }

    init {
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }
}