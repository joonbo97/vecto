package com.example.vecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.vecto.MainActivity.DataStoreUtils.myDataStore
import com.example.vecto.databinding.ActivityMainBinding
import com.example.vecto.editlocation.EditLocationActivity


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    object DataStoreUtils {
        private val Context.dataStore by preferencesDataStore("settings")
        val Context.myDataStore get() = this.dataStore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dataStore = applicationContext.myDataStore // Utilizing the DataStore instance here.


        binding.EditMapBtn.setOnClickListener{
            val intent = Intent(this, EditLocationActivity::class.java) //EditLocation 화면으로 이동
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null



    }
}