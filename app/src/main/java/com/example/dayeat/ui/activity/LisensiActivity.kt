package com.example.dayeat.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.dayeat.R
import com.example.dayeat.databinding.ActivityLisensiBinding

class LisensiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLisensiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLisensiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cdDaftar.visibility = View.GONE
        binding.cdLogin.visibility = View.VISIBLE

        binding.tvDaftarDisini.setOnClickListener {
            binding.cdDaftar.visibility = View.VISIBLE
            binding.cdLogin.visibility = View.GONE
        }

        binding.tvLoginDisini.setOnClickListener {
            binding.cdDaftar.visibility = View.GONE
            binding.cdLogin.visibility = View.VISIBLE
        }
    }
}