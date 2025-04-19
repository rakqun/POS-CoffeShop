package com.example.dayeat.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.dayeat.MainActivity
import com.example.dayeat.R
import com.example.dayeat.async.AsyncEscPosPrinter
import com.example.dayeat.model.Product
import kotlinx.coroutines.coroutineScope
import net.sourceforge.jtds.jdbc.UniqueIdentifier
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

object Tools {

    fun md5WithAscii(input: String): UniqueIdentifier {
        val asciiString = input
        val buff = asciiString.toByteArray(Charsets.US_ASCII)
        val md5Digest = MessageDigest.getInstance("MD5")
        val hashBytes = md5Digest.digest(buff)
        return UniqueIdentifier(hashBytes)
    }

    suspend fun getAsyncEscPosPrinter(context: Context, bluetoothConnection: BluetoothConnection, user: String, meja: String, salesNo: String, listPesanan: MutableList<Product>, method: String, cash:Int, promoCode: String, disc: Int, reprint: Boolean, textReprint: String, customer: String, tanggalCetak: String, waktuCetak: String,
                                      header1: String, header2: String, header3: String, header4: String, header5: String,
                                      footer1: String, footer2: String, footer3: String, footer4: String, footer5: String): AsyncEscPosPrinter {
        return coroutineScope {
            val printer = AsyncEscPosPrinter(bluetoothConnection, 203, 48f, 32)
            try {
                printer.addTextToPrint("\n" +
                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, context.resources.getDrawableForDensity(R.drawable.logo_ii, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                        "[L]\n" +
                        if (header1.isNotEmpty()){
                            "[C]$header1\n"
                        }else{
                            ""
                        }+
                        if (header2.isNotEmpty()){
                            "[C]$header2\n"
                        }else{
                            ""
                        }+
                        if (header3.isNotEmpty()){
                            "[C]$header3\n"
                        }else{
                            ""
                        }+
                        if (header4.isNotEmpty()){
                            "[C]$header4\n"
                        }else{
                            ""
                        }+
                        if (header5.isNotEmpty()){
                            "[C]$header5\n"
                        }else{
                            ""
                        }+
                        "[L]No BILL      [R]: [R]$salesNo\n" +
                        "[L]No Meja      [R]: [R]$meja\n" +
                        if (customer.isNotEmpty()){
                            "[L]Customer    [R]: [R]$customer"
                        }else{
                            ""
                        } +
                        "[L]Tanggal Bayar[R]: [R]${tanggalCetak}\n" +
                        "[L]Waktu Bayar  [R]: [R]${waktuCetak}\n" +
                        "[L]Kasir        [R]: [R]${user}\n" +
                        "[L]\n" +
                        "[L]================================\n" +
                        listPesanan.joinToString("\n") { it -> "[L]${it.nama}\n" +
                                "[L]${it.quanty}[L]x[L]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.harga)} [R]${NumberFormat.getNumberInstance(Locale.getDefault()).format(it.total)}"} +
                        "\n[L]================================\n" +
                        if (promoCode != ""){
                            "[L]<b>PROMO [R]$promoCode</b>\n" +
                                    "[L]<b>Disc  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(disc)}</b>\n"
                        }else{
                            ""
                        } +
                        "[L]<b>TOTAL [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}</b>\n" +
//                        if(listPesanan.sumOf { it.disc!!.toInt()} > 0){ "[L]DISC         [R]: [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak[0].disc)}\n"
//                            "[L]NET TOTAL         : [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listCetak.sumOf { it.totalCetak!!.toInt() } - listCetak[0].disc!!.toInt())}\n"
//                        } else { "" }+
                        "[L]ITEMS    [R]${listPesanan.sumOf { it.quanty!!.toInt() }}\n" +
                        if (method == "TUNAI"){
                            "[L]$method  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(cash)}\n" +
                                    "[L]KEMBALIAN[R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(cash - listPesanan.sumOf { it.total!!.toInt() })}\n"
                        }else{
                            "[L]$method  [R]Rp ${NumberFormat.getNumberInstance(Locale.getDefault()).format(listPesanan.sumOf { it.total!!.toInt() })}\n" +
                                    "\n"
                        } +
                        "[L]\n" +
                        if (reprint){
                            "\n[L]$textReprint\n"
                        }else{
                            "\n[L]================================\n"
                        } +
                        if (footer1.isNotEmpty()){
                            "[C]<b>$footer1</b>\n"
                        }else{
                            ""
                        } +
                        if (footer2.isNotEmpty()){
                            "[C]<b>$footer2</b>\n"
                        }else{
                            ""
                        } +
                        if (footer3.isNotEmpty()){
                            "[C]<b>$footer3</b>\n"
                        }else{
                            ""
                        } +
                        if (footer4.isNotEmpty()){
                            "[C]<b>$footer4</b>\n"
                        }else{
                            ""
                        } +
                        if (footer5.isNotEmpty()){
                            "[C]<b>$footer5</b>\n"
                        }else{
                            ""
                        })
            }catch (e: EscPosEncodingException){
                e.toString()
                Log.e("cek printer", e.toString())
            }
            printer
        }
    }
}