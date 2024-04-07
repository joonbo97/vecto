package com.vecto_example.vecto.ui.mypage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.vecto_example.vecto.LoginActivity
import com.vecto_example.vecto.MainActivity
import com.vecto_example.vecto.ui.notification.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypageBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.notification.NotificationViewModel
import com.vecto_example.vecto.ui.notification.NotificationViewModelFactory
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils

class MypageFragment : Fragment() {
    lateinit var binding: FragmentMypageBinding
    private val mypageViewModel: MypageViewModel by viewModels{
        MypageViewModelFactory(UserRepository(VectoService.create()))
    }
    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(NotificationRepository(VectoService.create()))
    }

    private lateinit var notificationReceiver: BroadcastReceiver


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        if(!Auth.loginFlag.value!!){
            goLogin()
            (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.SearchFragment)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initObservers()
        initListeners()
        initReceiver()
    }

    private fun initUI() {
        if(Auth.loginFlag.value == true){
            notificationViewModel.getNewNotificationFlag()
        }
    }

    private fun initListeners() {
        /*   리스너 초기화 함수   */

        //알림 아이콘 클릭 이벤트
        binding.AlarmIconImage.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                RequestLoginUtils.requestLogin(requireContext())
                return@setOnClickListener
            }

            val intent = Intent(context, NotificationActivity::class.java)
            startActivity(intent)
        }

        /*   계정 설정   */
        binding.MypageMenu1.setOnClickListener {
            val navController = findNavController()
            if(Auth.provider == "vecto")
                navController.navigate(R.id.MypageSettingFragment)
            else
                navController.navigate(R.id.MypageSettingkakaoFragment)
        }

        /*   내 게시물   */
        binding.MypageMenu2.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.MypagePostFragment)
        }

        /*   좋아요 한 게시물   */
        binding.MypageMenu3.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.MypageLikepostFragment)
        }

        /*   문의하기   */
        binding.MypageMenu4.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.MypageInquiryFragment)
        }

        /*   로그아웃   */
        binding.MypageMenu5.setOnClickListener {
            mypageViewModel.logout()
            Toast.makeText(requireContext(), " 로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.SearchFragment)
        }
    }

    private fun initObservers() {
        Auth._profileImage.observe(viewLifecycleOwner) {
            LoadImageUtils.loadProfileImage(requireContext(), binding.ProfileImage)
        }

        Auth._nickName.observe(viewLifecycleOwner) {
            binding.UserNameText.text = Auth._nickName.value
        }

        /*   알림 아이콘 관련 Observer   */
        notificationViewModel.newNotificationFlag.observe(viewLifecycleOwner) {
            it.onSuccess { newNotificationFlag->
                if(newNotificationFlag){    //새로운 알림이 있을 경우
                    Log.d("SEARCH_INIT_UI", "SUCCESS_TRUE")
                    binding.AlarmIconImage.setImageResource(R.drawable.alarmon_icon)
                }
                else{   //새로운 알림이 없는 경우
                    Log.d("SEARCH_INIT_UI", "SUCCESS_FALSE")
                    binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
                }
            }
                .onFailure {//실패한 경우
                    Log.d("SEARCH_INIT_UI", "FAIL")
                    binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
                }
        }
    }

    private fun goLogin(){
        val intent = Intent(context, LoginActivity::class.java) //Login 화면으로 이동
        startActivity(intent)
    }

    private fun initReceiver() {
        /*   Receiver 초기화 함수   */

        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                notificationViewModel.getNewNotificationFlag()
            }
        }

        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                notificationReceiver,
                IntentFilter("NEW_NOTIFICATION")
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(notificationReceiver)
        }
    }

    override fun onResume() {
        super.onResume()

        initUI()
    }
}