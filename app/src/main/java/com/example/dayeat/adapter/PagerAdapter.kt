package com.example.dayeat.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.dayeat.model.MenuGroup
import com.example.dayeat.ui.fragment.MenuItemFragment

class PagerAdapter(fragment: FragmentActivity): FragmentStateAdapter(fragment) {

    private var menuGroup: MutableList<MenuGroup> = mutableListOf()

    fun setData(mMenuGroup: MutableList<MenuGroup>){
        menuGroup.clear()
        menuGroup.addAll(mMenuGroup)
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = MenuItemFragment()
        fragment.arguments = Bundle().apply {
            putInt(MenuItemFragment.ARG_SECTION_NUMBER, menuGroup[position].catId!!.toInt())
        }
        return fragment
    }

    override fun getItemCount(): Int {
        return menuGroup.size
    }
}