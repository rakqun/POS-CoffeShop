package com.example.dayeat.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection

class Setting(var context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    var PRIVATE_MODE = 0

    fun savePrinter(printer: String, namePrinter: String){
        editor.putString(KEY_PRINTER, printer)
        editor.putString(KEY_NAME_PRINTER, namePrinter)
        editor.commit()
    }

    fun saveBluetooth(bluetooth: Boolean){
        editor.putBoolean(KEY_BLUETOOTH, bluetooth)
        editor.commit()
    }

    fun saveShowChg(chg: Boolean){
        editor.putBoolean(KEY_SHOWCHG, chg)
        editor.commit()
    }

    fun saveShowTax(tax: Boolean){
        editor.putBoolean(KEY_SHOWTAX, tax)
        editor.commit()
    }

    fun getPrinterSave(): BluetoothConnection? {
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(pref.getString(KEY_PRINTER, "").toString())
        return BluetoothConnection(device)
    }

    fun getPrinter(): String = pref.getString(KEY_PRINTER, "").toString()
    fun getNamePrinterSave(): String = pref.getString(KEY_NAME_PRINTER, "").toString()
    fun getBluetoothSwitch(): Boolean = pref.getBoolean(KEY_BLUETOOTH, false)
    fun getShowChg(): Boolean = pref.getBoolean(KEY_SHOWCHG, false)
    fun getShowTax(): Boolean = pref.getBoolean(KEY_SHOWTAX, false)

    companion object {
        private const val PREF_NAME = "PrefrenceSetting"
        private const val KEY_PRINTER = "Printer"
        private const val KEY_NAME_PRINTER = "NamePrinter"
        private const val KEY_BLUETOOTH = "BLUETOOTH"
        private const val KEY_SHOWTAX = "SHOW_TAX"
        private const val KEY_SHOWCHG = "SHOW_CHG"
    }

    init {
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }
}