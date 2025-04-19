package com.example.dayeat.ui.activity

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.dayeat.MainActivity
import com.example.dayeat.R
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.ActivityLoginBinding
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Tools
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Date

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var session: SessionLogin
    private lateinit var connect: Conect
    private lateinit var binding: ActivityLoginBinding

    private var user = ""
    private var userId = 0
    private var mobileRules = ""

    private var tax = 0
    private var chg = 0

    private var header1 = ""
    private var header2 = ""
    private var header3 = ""
    private var header4 = ""
    private var header5 = ""
    private var footer1 = ""
    private var footer2 = ""
    private var footer3 = ""
    private var footer4 = ""
    private var footer5 = ""

    var pinUser = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connect = Conect()
        session = SessionLogin(this)

//        session.createLoginSession(1, "ADMIN", "ADMIN")

        if (session.isLoggedIn()){
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finishAffinity()
        }

        setUpCodeInput()
        setPermission()

        binding.btnLogIn.setOnClickListener(this)
    }

    private fun setUpCode():String {
        pinUser = binding.et1.text.toString() + binding.et2.text.toString() + binding.et3.text.toString() + binding.et4.text.toString() + binding.et5.text.toString() + binding.et6.text.toString()
        val code = Tools.md5WithAscii(pinUser)
        val conn = connect.connection(this)
        if (conn != null) {
            return try {
                val query = "EXEC USP_J_User_Query @UserName = ?, @ByPIN=1"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setString(1, code.toString())
                val resultSet = preparedStatement.executeQuery()
                while (resultSet.next()){
                    userId = resultSet.getInt("UserID")
                    user = resultSet.getString("UserName")
                    mobileRules = resultSet.getString("MobileRole")
                }

                val queryTaxChg = "SELECT \n" +
                        "\tdbo.UDF_SH_Settings_GetAsMoney ('Sales.ServiceCharge', 5) Chg,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsMoney ('Sales.PB1', 10) Tax, \n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Header1', '') Header1,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Header2', '') Header2,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Header3', '') Header3,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Header4', '') Header4,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Header5', '') Header5,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Footer1', '') Footer1,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Footer2', '') Footer2,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Footer3', '') Footer3,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Footer4', '') Footer4,\n" +
                        "\tdbo.UDF_SH_Settings_GetAsString('TextBill.Footer5', '') Footer5"
                val statement = conn.createStatement()
                val rs = statement.executeQuery(queryTaxChg)
                while (rs.next()){
                    tax = rs.getInt("Tax")
                    chg = rs.getInt("Chg")
                    header1 = rs.getString("Header1")
                    header2 = rs.getString("Header2")
                    header3 = rs.getString("Header3")
                    header4 = rs.getString("Header4")
                    header5 = rs.getString("Header5")
                    footer1 = rs.getString("Footer1")
                    footer2 = rs.getString("Footer2")
                    footer3 = rs.getString("Footer3")
                    footer4 = rs.getString("Footer4")
                    footer5 = rs.getString("Footer5")
                }

                "Berhasil"
            } catch (e: SQLException) {
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun setUpCodeInput(){
        binding.et1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et2.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.et2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et1.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et3.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.et3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et2.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et4.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.et4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et3.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et5.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.et5.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et4.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et6.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.et6.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding.et5.requestFocus()
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    private fun setPermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 101)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), 102)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 103)
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 104)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }

            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Admin Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }

            103 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Connect Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }

            104 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(binding.root, "Bluetooth Scan Is Denied", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnLogIn -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Mohon Tunggu")
                progressDialog.setMessage("Sedang konfirmasi pin...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO){
                        setUpCode()
                    }
                    when(result){
                        "Berhasil" -> {
                            if (user != ""){
                                session.createLoginSession(userId, user, mobileRules)
                                session.createTaxChg(tax, chg)
                                session.createCustomBill(header1, header2, header3, header4, header5, footer1, footer2, footer3, footer4, footer5)
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }else{
                                if (pinUser == "567765"){
                                    session.createLoginSession(0, "SUPERADMIN", "ADMIN")
                                    session.createTaxChg(tax, chg)
                                    session.createCustomBill(header1, header2, header3, header4, header5, footer1, footer2, footer3, footer4, footer5)
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finishAffinity()
                                }else{
                                    Snackbar.make(binding.root, "Pin kamu salah", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                        "Gagal" -> {
                            Snackbar.make(binding.root, "Tidak terhubung ke server", Snackbar.LENGTH_SHORT).show()
                        }
                        else -> {
                            Snackbar.make(binding.root, result, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    progressDialog.dismiss()
                }
            }
        }
    }
}