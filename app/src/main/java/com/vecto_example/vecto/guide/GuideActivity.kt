package com.vecto_example.vecto.guide

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vecto_example.vecto.databinding.ActivityGuideBinding
import java.lang.IllegalArgumentException

class GuideActivity : AppCompatActivity() {
    /*   Guide Fragment (1 ~ 3) 를 위한 Activity   */

    private lateinit var binding: ActivityGuideBinding
    private lateinit var guideViewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guideViewPager = binding.GuideViewPager



        val adapter = GuidePagerAdapter(this)

        guideViewPager.adapter = adapter
        guideViewPager.isUserInputEnabled = false


        //TODO 페이지 인디케이터를 통해 하단에 진척도 표시
    }

    fun moveToNextFragment() {
        val currentItem = guideViewPager.currentItem
        if (currentItem < 2) {
            guideViewPager.currentItem = currentItem + 1
        }
    }

    inner class GuidePagerAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity){
        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            return when(position){
                0 -> GuideFirstFragment()
                1 -> GuideSecondFragment()
                2 -> GuideThirdFragment()
                //3 -> GuideFourthFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }

    }

}