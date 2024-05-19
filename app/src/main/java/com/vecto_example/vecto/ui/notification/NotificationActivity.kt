package com.vecto_example.vecto.ui.notification

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.ui.notification.adapter.MyNotificationAdapter
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.databinding.ActivityNotificationBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils

class NotificationActivity : AppCompatActivity() {
    lateinit var binding: ActivityNotificationBinding
    private lateinit var myNotificationAdapter: MyNotificationAdapter

    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(NotificationRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        initObserver()
        initListener()
        getNotification()
    }

    private fun initListener() {
        binding.BackButton.setOnClickListener {
            finish()
        }
    }

    private fun initObserver() {
        /*   Notification 관련 Observer   */
        notificationViewModel.notificationsLiveData.observe(this){
            notificationViewModel.notificationsLiveData.value?.let {
                myNotificationAdapter.addNotificationData(
                    it.notifications)
            }

            if(notificationViewModel.allNotifications.isEmpty()){
                setNoneImage()
            }
        }

        notificationViewModel.errorMessage.observe(this) {
            ToastMessageUtils.showToast(this, getString(it))

            if(it == R.string.expired_login){
                SaveLoginDataUtils.deleteData(this)
                finish()
            }
        }

        notificationViewModel.reissueResponse.observe(this){
            SaveLoginDataUtils.changeToken(this, notificationViewModel.accessToken, notificationViewModel.refreshToken)

            if(it == NotificationViewModel.Function.GetNotificationResults.name){
                getNotification()
            }
        }

        /*   로딩 관련 Observer   */
        notificationViewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        notificationViewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

    }

    private fun initRecyclerView() {
        myNotificationAdapter = MyNotificationAdapter(this)
        val notificationRecyclerView = binding.NotificationRecyclerView
        notificationRecyclerView.adapter = myNotificationAdapter
        notificationRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        notificationRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if(!recyclerView.canScrollVertically(1)) {
                    if(!notificationViewModel.checkLoading()){
                        getNotification()
                    }
                }
            }
        })
    }

    private fun getNotification(){
        notificationViewModel.getNotificationResults()
    }

    private fun setNoneImage() {
        binding.NoneImage.visibility = View.VISIBLE
        binding.NoneText.visibility = View.VISIBLE
        Log.d("NONE SET", "NONE SET")
    }


    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IS GONE")
    }
}