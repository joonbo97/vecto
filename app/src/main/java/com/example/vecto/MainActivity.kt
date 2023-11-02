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

    }


}