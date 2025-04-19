package com.example.dayeat

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.example.dayeat.databinding.ActivityMainBinding
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var sessionLogin: SessionLogin
    private lateinit var setting: Setting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionLogin = SessionLogin(this)
        setting = Setting(this)

        if (!sessionLogin.isLoggedIn()){
            sessionLogin.noLogin()
            finishAffinity()
        }

        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.tvTitleNameHeader).text = sessionLogin.getUserLogin()
        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.tvRoleNameHeader).text = sessionLogin.getMobileRole()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_menu, R.id.nav_complet, R.id.nav_pay, R.id.nav_table, R.id.nav_order, R.id.nav_setting, R.id.nav_user), drawerLayout)

        when(sessionLogin.getMobileRole()){
            "WAITER" -> {
                binding.navView.menu.findItem(R.id.nav_order).isVisible = true
                binding.navView.menu.findItem(R.id.nav_pay).isVisible = false
                binding.navView.menu.findItem(R.id.nav_customer).isVisible = false
                binding.navView.menu.findItem(R.id.nav_eod).isVisible = false
                binding.navView.menu.findItem(R.id.nav_complet).isVisible = false
                binding.navView.menu.findItem(R.id.nav_menu).isVisible = false
                binding.navView.menu.findItem(R.id.nav_table).isVisible = false
                binding.navView.menu.findItem(R.id.nav_user).isVisible = false
                binding.navView.menu.findItem(R.id.nav_home).isVisible = false
                binding.navView.menu.findItem(R.id.nav_setting).isVisible = false
                binding.navView.menu.findItem(R.id.nav_logout).isVisible = true
            }
            "KASIR" -> {
                binding.navView.menu.findItem(R.id.nav_order).isVisible = true
                binding.navView.menu.findItem(R.id.nav_pay).isVisible = true
                binding.navView.menu.findItem(R.id.nav_customer).isVisible = true
                binding.navView.menu.findItem(R.id.nav_eod).isVisible = true
                binding.navView.menu.findItem(R.id.nav_complet).isVisible = true
                binding.navView.menu.findItem(R.id.nav_menu).isVisible = false
                binding.navView.menu.findItem(R.id.nav_table).isVisible = false
                binding.navView.menu.findItem(R.id.nav_user).isVisible = false
                binding.navView.menu.findItem(R.id.nav_home).isVisible = false
                binding.navView.menu.findItem(R.id.nav_setting).isVisible = false
                binding.navView.menu.findItem(R.id.nav_logout).isVisible = true
            }
            "ADMIN" -> {
                binding.navView.menu.findItem(R.id.nav_order).isVisible = true
                binding.navView.menu.findItem(R.id.nav_pay).isVisible = true
                binding.navView.menu.findItem(R.id.nav_menu).isVisible = true
                binding.navView.menu.findItem(R.id.nav_customer).isVisible = true
                binding.navView.menu.findItem(R.id.nav_eod).isVisible = true
                binding.navView.menu.findItem(R.id.nav_complet).isVisible = true
                binding.navView.menu.findItem(R.id.nav_table).isVisible = true
                binding.navView.menu.findItem(R.id.nav_user).isVisible = true
                binding.navView.menu.findItem(R.id.nav_home).isVisible = true
                binding.navView.menu.findItem(R.id.nav_setting).isVisible = true
                binding.navView.menu.findItem(R.id.nav_logout).isVisible = true
            }
            else -> {
                Toast.makeText(this, "Tidak punya akses untuk login..", Toast.LENGTH_SHORT).show()
                sessionLogin.logoutUser()
                finish()
            }
        }

        binding.appBarMain.buttonDrawerToggle.setOnClickListener{
            drawerLayout.open()
        }

        navView.setupWithNavController(navController)
        navView.setCheckedItem(R.id.nav_order)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Apakah anda mau logout?")
                    builder.setPositiveButton("Ya"){_,_->
                        lifecycleScope.launch {
                            sessionLogin.logoutUser()
                            finish()
                        }
                    }
                    builder.setNegativeButton("Batal"){p,_->
                        p.dismiss()
                    }
                    builder.create().show()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    handled
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}