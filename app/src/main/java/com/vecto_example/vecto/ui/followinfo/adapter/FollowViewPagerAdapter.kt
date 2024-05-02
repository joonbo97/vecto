package com.vecto_example.vecto.ui.followinfo.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.vecto_example.vecto.ui.followinfo.FollowerListFragment
import com.vecto_example.vecto.ui.followinfo.FollowingListFragment

class FollowViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf<Fragment>(
        FollowerListFragment(),
        FollowingListFragment()
    )
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}