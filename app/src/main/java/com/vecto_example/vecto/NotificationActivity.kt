package com.vecto_example.vecto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.NotificationDatabase
import com.vecto_example.vecto.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {
    lateinit var binding: ActivityNotificationBinding
    lateinit var myNotificationAdapter: MyNotificationAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myNotificationAdapter = MyNotificationAdapter(this)
        val notificationRecyclerView = binding.NotificationRecyclerView
        notificationRecyclerView.adapter = myNotificationAdapter
        notificationRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        if(Auth.loginFlag.value == true) {
            val notifications = NotificationDatabase(this).getAllNotificationData()
            for (i in 0 until notifications.size)
                myNotificationAdapter.notificationData.add(notifications[i])
        }

        if(myNotificationAdapter.notificationData.isEmpty())
        {
            binding.NoneImage.visibility = View.VISIBLE
            binding.NoneText.visibility = View.VISIBLE
        }
        else
        {
            binding.NoneImage.visibility = View.GONE
            binding.NoneText.visibility = View.GONE
        }

        binding.BackButton.setOnClickListener {
            finish()
        }




        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                val hasUnshown = NotificationDatabase(this@NotificationActivity).checkShowFlag()
                if (hasUnshown) {//읽지않은 데이터가 있을 경우
                    Auth.setShowFlag(true)
                }
                else{
                    Auth.setShowFlag(false)
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}