package com.example.dayeat.ui.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dayeat.R
import com.example.dayeat.adapter.CategoryAdapter
import com.example.dayeat.adapter.UserAdapter
import com.example.dayeat.conn.Conect
import com.example.dayeat.databinding.FragmentUserBinding
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.model.UserAccount
import com.example.dayeat.mvvm.MainViewModel
import com.example.dayeat.utils.SessionLogin
import com.example.dayeat.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

class UserFragment : Fragment() {

    private var _binding : FragmentUserBinding? = null
    private val binding get() = _binding!!

    private var arrayRole: ArrayList<String> = arrayListOf("","WAITER", "KASIR", "ADMIN")
    private lateinit var mainViewModel: MainViewModel
    private var userAdapter: UserAdapter = UserAdapter()
    private var mUser: MutableList<UserAccount> = mutableListOf()

    private lateinit var connect:Conect
    private lateinit var sessionLogin: SessionLogin

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)

        connect = Conect()
        sessionLogin = SessionLogin(requireContext())

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rvUser.layoutManager = GridLayoutManager(context, 1)
        } else {
            binding.rvUser.layoutManager = GridLayoutManager(context, 3)
        }

        binding.rvUser.setHasFixedSize(true)

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                mainViewModel.queryDataUser(requireContext())
            }
        }

        mainViewModel.getUser().observe(viewLifecycleOwner, Observer { userAcount ->
            if (userAcount != null){
                mUser.clear()
                mUser.addAll(userAcount)
                userAdapter.setData(mUser)
                showItems()
            }
        })

        binding.pullRefres.setOnRefreshListener {
            mainViewModel.queryDataCat(requireContext())
            binding.pullRefres.setRefreshing(false)
        }

        binding.fabAddUser.setOnClickListener {
            dialogCategory(0, "", "", "", "")
        }
        return binding.root
    }

    private fun showItems(){
        binding.rvUser.adapter = userAdapter
        userAdapter.setOnItemClickRemoveCallback(object : UserAdapter.OnItemClickRemoveCallback{
            override fun onItemClicked(userAcount: UserAccount) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(userAcount.userName + " Ini mau dihapus cok?")

                builder.setPositiveButton("Yoi"){p,_->
                    lifecycleScope.launch {
                        val resultDelete = withContext(Dispatchers.IO){
                            queryHapusUser(userAcount.userId!!.toInt())
                        }
                        when(resultDelete){
                            "Berhasil" -> {
                                p.dismiss()
                                mainViewModel.queryDataUser(requireContext())
                            }
                            "Gagal" -> {
                                Toast.makeText(context, "Gagal dihapus guys!!", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(context, resultDelete, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                builder.setNegativeButton("Gak"){p,_->
                    p.dismiss()
                }

                builder.create().show()
            }
        })

        userAdapter.setOnItemClickEditCallback(object : UserAdapter.OnItemClickEditCallback{
            override fun onItemClicked(userAcount: UserAccount) {
                dialogCategory(userAcount.userId!!.toInt(), userAcount.userName.toString(), userAcount.fullName.toString(), userAcount.mobileRole.toString(), userAcount.pin.toString()
                )
            }
        })
    }

    private fun dialogCategory(userId: Int, userName: String, fullName: String, role:String, pin:String){
        val dialogAddCategory = Dialog(requireContext())
        dialogAddCategory.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogAddCategory.setContentView(R.layout.dialog_user)
        dialogAddCategory.setCanceledOnTouchOutside(false)

        val tvTitle = dialogAddCategory.findViewById<TextView>(R.id.tvTitleDialogUser)
        val etUserName = dialogAddCategory.findViewById<EditText>(R.id.etNameUser)
        val etFullName = dialogAddCategory.findViewById<EditText>(R.id.etFullname)
        val etPin = dialogAddCategory.findViewById<EditText>(R.id.etPinUser)
        val spinnerRole = dialogAddCategory.findViewById<Spinner>(R.id.spinnerRoleMobile)
        val btnClose = dialogAddCategory.findViewById<ImageView>(R.id.imgCloseMenuUser)
        val btnBatal = dialogAddCategory.findViewById<TextView>(R.id.tvBatalUser)
        val btnSimpan = dialogAddCategory.findViewById<TextView>(R.id.tvSimpanUser)
        val imgPin = dialogAddCategory.findViewById<ImageView>(R.id.imgRefresh)

        imgPin.setOnClickListener {
            etPin.setText((100000..999999).random().toString())
        }

        var selectedRole = ""
        val adapterRole = ArrayAdapter<String>(requireContext(), R.layout.spinner_item, arrayRole)
        adapterRole.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapterRole
        spinnerRole.setSelection(0)
        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedRole = arrayRole[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        if (userId > 0){
            etUserName.setText(userName)
            etFullName.setText(fullName)
            spinnerRole.setSelection(adapterRole.getPosition(role))
        }

        btnClose.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnBatal.setOnClickListener {
            dialogAddCategory.dismiss()
        }

        btnSimpan.setOnClickListener{
            val p = if (userId < 0 ){
                etPin.text.toString().length < 6
            }else{
                if (etPin.text.toString().isEmpty()){
                    false
                }else{
                    etPin.text.toString().length < 6
                }
            }
            if (etUserName.text.toString().isEmpty() || p || etFullName.text.toString().isEmpty()){
                Toast.makeText(context, "Semua wajib diisi dan Pin wajib 6 angka yaaa", Toast.LENGTH_SHORT).show()
            }else{
                lifecycleScope.launch {
                    val resultSimpan = withContext(Dispatchers.IO){
                        if (userId < 0){
                            querySaveUser(etUserName.text.toString(), etFullName.text.toString(), selectedRole, Tools.md5WithAscii(etPin.text.toString()).toString())
                        }else{
                            queryUpdateUser(userId, etUserName.text.toString(), etFullName.text.toString(), selectedRole, if (etPin.text.toString().isEmpty()){pin}else{Tools.md5WithAscii(etPin.text.toString()).toString()})
                        }
                    }
                    when (resultSimpan){
                        "Berhasil" -> {
                            dialogAddCategory.dismiss()
                            mainViewModel.queryDataUser(requireContext())
                        }
                        "Gagal" -> {
                            Toast.makeText(context, "Gagal nyimpan guys!!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, resultSimpan, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dialogAddCategory.show()
        dialogAddCategory.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogAddCategory.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogAddCategory.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialogAddCategory.window?.setGravity(Gravity.CENTER)
    }

    private fun querySaveUser(userName: String, fullName: String, role:String, pin:String):String{
        val conn = connect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_User_Update @UserName = ?, @FullName = ?, @MobileRole = ?, @DesktopPIN = ?, @User = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, userName)
                pp.setString(2, fullName)
                pp.setString(3, role)
                pp.setString(4, pin)
                pp.setString(5, sessionLogin.getUserLogin())
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryUpdateUser(userId:Int, userName: String, fullName: String, role:String, pin:String):String{
        val conn = connect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_User_Update @UserName = ?, @FullName = ?, @MobileRole = ?, @DesktopPIN = ?, @User = ?, @UserId = ?"
                val pp = conn.prepareStatement(query)
                pp.setString(1, userName)
                pp.setString(2, fullName)
                pp.setString(3, role)
                pp.setString(4, pin)
                pp.setString(5, sessionLogin.getUserLogin())
                pp.setInt(6, userId)
                pp.execute()
                "Berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                e.toString()
            }
        }
        return "Gagal"
    }

    private fun queryHapusUser(userId: Int):String{
        val conn = connect.connection(context)
        if (conn!= null){
            return try {
                val query = "EXEC USP_J_User_Delete @UserID = ?"
                val pp = conn.prepareStatement(query)
                pp.setInt(1, userId)
                pp.execute()
                "Berhasil"
            }catch (e:SQLException){
                e.toString()
            }
        }
        return "Gagal"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}