package com.vecto_example.vecto.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.ActivityMainBinding
import com.vecto_example.vecto.ui.comment.CommentActivity


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var currentMenuItemId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        // 탭이 다시 선택될 때 스크롤을 최상위로 이동
        navView.setOnNavigationItemReselectedListener { _ ->
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
            val currentFragment = navHostFragment.childFragmentManager.fragments.first()
            if (currentFragment is ScrollToTop) {
                currentFragment.scrollToTop()
            }
        }


        /*Notification Intent 관련*/
        intent.getIntExtra("feedId", -1).let{
            if(it >= 0)//댓글 관련 알림인 경우
            {

                val intent = Intent(this, CommentActivity::class.java)
                intent.putExtra("feedID", it)
                this.startActivity(intent)
            }
        }

        /*글 수정 Intent 관련*/
        intent.getStringExtra("editCourse").let{
            if(it != null) {
                updateBottomNavigationSelection(R.id.EditCourseFragment)
                val bundle = bundleOf("selectedDateKey" to it)
                navController.navigate(R.id.EditCourseFragment, bundle)
            }
        }


    }

    interface ScrollToTop {
        fun scrollToTop()
    }

    fun updateBottomNavigationSelection(menuItemId: Int) {
        binding.navView.selectedItemId = menuItemId
    }
}