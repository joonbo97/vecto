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
        //navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        navView.setOnItemSelectedListener { item ->
            if (item.itemId == currentMenuItemId) {
                // 같은 아이템이 다시 선택될 경우 RecyclerView를 최상단으로 스크롤
                scrollToTop(item.itemId)
                true // 이벤트 처리 완료
            } else {
                // 다른 아이템을 선택한 경우
                currentMenuItemId = item.itemId
                navController.navigate(item.itemId)
                true
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.SearchFragment -> binding.navView.selectedItemId = R.id.SearchFragment
                R.id.WriteFragment -> binding.navView.selectedItemId = R.id.WriteFragment
                R.id.TodayCourseFragment -> binding.navView.selectedItemId = R.id.TodayCourseFragment
                R.id.EditCourseFragment -> binding.navView.selectedItemId = R.id.EditCourseFragment
                R.id.MypageFragment -> binding.navView.selectedItemId = R.id.MypageFragment
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

    private fun scrollToTop(menuItemId: Int) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments.first()
        if (currentFragment is ScrollToTop) {
            currentFragment.scrollToTop()
        }
    }

    interface ScrollToTop {
        fun scrollToTop()
    }

    fun updateBottomNavigationSelection(menuItemId: Int) {
        binding.navView.selectedItemId = menuItemId
    }
}