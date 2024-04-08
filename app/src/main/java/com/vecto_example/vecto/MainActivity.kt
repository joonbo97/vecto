package com.vecto_example.vecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.vecto_example.vecto.MainActivity.DataStoreUtils.myDataStore
import com.vecto_example.vecto.databinding.ActivityMainBinding
import com.vecto_example.vecto.ui.comment.CommentActivity


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    object DataStoreUtils {
        private val Context.dataStore by preferencesDataStore("settings")
        val Context.myDataStore get() = this.dataStore

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dataStore = applicationContext.myDataStore // Utilizing the DataStore instance here.

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        /*Notification Intent 관련*/
        intent.getIntExtra("feedId", -1).let{
            if(it >= 0)//댓글 관련 알림인 경우
            {
                /*val bundle = bundleOf("selectedDateKey" to it)
                navController.navigate(R.id.SearchFragment, bundle)*/

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

    fun updateBottomNavigationSelection(menuItemId: Int) {
        binding.navView.selectedItemId = menuItemId
    }
}