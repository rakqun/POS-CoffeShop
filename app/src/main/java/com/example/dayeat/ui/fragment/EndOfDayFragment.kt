package com.example.dayeat.ui.fragment

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.ClosingAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentEndOfDayBinding
import com.example.dayeat.databinding.FragmentPayBinding
import com.example.dayeat.model.CLosing
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.SessionLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class EndOfDayFragment : Fragment() {

    private var _binding: FragmentEndOfDayBinding? = null
    private val binding get() = _binding!!

    private lateinit var closingAdapter: ClosingAdapter
    private lateinit var mainViewModel: MainViewModel
    private lateinit var sessionLogin: SessionLogin
    private lateinit var connect: Conect

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentEndOfDayBinding.inflate(inflater, container, false)

        closingAdapter = ClosingAdapter()
        connect = Conect()
        sessionLogin = SessionLogin(requireContext())
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Mohon tunggu..")
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvClosing.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvClosing.layoutManager = GridLayoutManager(context, 2)
        }

        binding.rvClosing.setHasFixedSize(true)

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.queryClosing(requireContext())
            }
        }

        mainViewModel.getClosing().observe(viewLifecycleOwner, Observer { closing ->
            if (closing != null){
                closingAdapter.setData(closing)
                binding.rvClosing.adapter = closingAdapter
                closingAdapter.setOnItemClickCallback(object : ClosingAdapter.OnItemClickCallback{
                    override fun onItemClicked(closing: CLosing) {
                        val bundle = Bundle().apply {
                            putInt("closingID", closing.closingID!!.toInt())
                        }
                        view!!.findNavController().navigate(R.id.action_nav_eod_to_detailCompletActivity, bundle)
                    }
                })
            }
        })

        binding.pullRefres.setOnRefreshListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    mainViewModel.queryClosing(requireContext())
                }
                binding.pullRefres.setRefreshing(false)
            }
        }

        binding.cvCreateEod.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Create End Of Day?")
            builder.setPositiveButton("Ya"){_,_->
                lifecycleScope.launch {
                    progressDialog.show()
                    val resultCreate = withContext(Dispatchers.IO){
                        createEod()
                    }
                    when(resultCreate){
                        "Berhasil" -> {
                            withContext(Dispatchers.IO){
                                mainViewModel.queryClosing(requireContext())
                            }
                        }
                        "Gagal" -> {
                            Toast.makeText(context, "Gagal create, periksa kembali jaringan...", Toast.LENGTH_SHORT).show()
                        }
                        else -> {Toast.makeText(context, resultCreate.replace("java.sql.SQLException: ", ""), Toast.LENGTH_SHORT).show()}
                    }
                    progressDialog.dismiss()
                }
            }

            builder.setNegativeButton("Ga"){p,_->
                p.dismiss()
            }
            builder.create().show()
        }

        return binding.root
    }

    private fun createEod():String{
        val conn = connect.connection(context)
        if (conn != null){
            return try {
                val query = "EXEC USP_J_Closing_Create ?, 1"
                val ps = conn.prepareStatement(query)
                ps.setString(1, sessionLogin.getUserLogin())
                ps.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }
}