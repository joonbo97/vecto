package com.vecto_example.vecto.ui.mypage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.vecto_example.vecto.LoginActivity
import com.vecto_example.vecto.MainActivity
import com.vecto_example.vecto.ui.notification.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypageBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.LoadImageUtils

class MypageFragment : Fragment() {
    lateinit var binding: FragmentMypageBinding
    private val viewModel: MypageViewModel by viewModels{
        MypageViewModelFactory(UserRepository(VectoService.create()))
    }

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

        initObservers()
        initListeners()
    }

    private fun initListeners() {
        binding.AlarmIconImage.setOnClickListener {
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
            viewModel.logout()
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

        Auth.showFlag.observe(viewLifecycleOwner) {
            if(Auth.showFlag.value == true)//확인 안한 알림이 있을 경우
            {
                binding.AlarmIconImage.setImageResource(R.drawable.alarmon_icon)
            }
            else//확인 안한 알림이 없을 경우
            {
                binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
            }
        }
    }

    private fun goLogin(){
        val intent = Intent(context, LoginActivity::class.java) //Login 화면으로 이동
        startActivity(intent)
    }

}