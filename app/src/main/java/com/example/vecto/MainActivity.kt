package com.example.vecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.vecto.databinding.ActivityMainBinding
import com.example.vecto.editlocation.EditLocationActivity
import com.example.vecto.guide.GuideActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val Context.dataStore by preferencesDataStore("settings")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.EditMapBtn.setOnClickListener{
            val intent = Intent(this, EditLocationActivity::class.java) //EditLocation 화면으로 이동
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        navView.setOnItemSelectedListener {item ->
            if(item.itemId == R.id.TodayCourseFragment)
            {
                lifecycleScope.launch {
                    if(!isGuideFlag()){
                        showGuide()
                        saveGuideFlag(true)
                    }
                }
            }
            true
        }
    }

    private object PreferencesKeys{
        val guideFlag = booleanPreferencesKey("guide_flag")
    }

    private suspend fun saveGuideFlag(value: Boolean){
        dataStore.edit{preferences ->
            preferences[PreferencesKeys.guideFlag] = value
        }
    }

    private suspend fun isGuideFlag(): Boolean{
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.guideFlag] ?: false
    }

    private fun showGuide(){
        val intent = Intent(this, GuideActivity::class.java) //Guide 화면으로 이동
        startActivity(intent)
    }
}