package com.vecto_example.vecto

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.vecto_example.vecto.MainActivity.DataStoreUtils.myDataStore
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.NotificationDatabase
import com.vecto_example.vecto.databinding.ActivityMainBinding


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


        val hasUnshown = NotificationDatabase(this).checkShowFlag()
        if (hasUnshown) {//읽지않은 데이터가 있을 경우
            Auth.setShowFlag(true)
        }
        else{
            Auth.setShowFlag(false)
        }
        Log.d("CHECK_NOTIFICATION", Auth.showFlag.value.toString())

    }


}