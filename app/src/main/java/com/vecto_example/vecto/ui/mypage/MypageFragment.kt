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
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.ui.notification.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypageBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.followinfo.FollowInfoActivity
import com.vecto_example.vecto.ui.likefeed.LikeFeedActivity
import com.vecto_example.vecto.ui.myfeed.MyFeedActivity
import com.vecto_example.vecto.ui.notification.NotificationViewModel
import com.vecto_example.vecto.ui.notification.NotificationViewModelFactory
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModel
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModelFactory
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils

class MypageFragment : Fragment() {
    lateinit var binding: FragmentMypageBinding
    private val mypageViewModel: MypageViewModel by viewModels{
        MypageViewModelFactory(UserRepository(VectoService.create()))
    }

    private val userInfoViewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        if(!Auth.loginFlag.value!!){
            val intent = Intent(context, LoginActivity::class.java) //Login 화면으로 이동
            startActivity(intent)

            (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.SearchFragment)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userInfoViewModel.getUserInfo(Auth._userId.value.toString())

        initObservers()
        initListeners()
    }

    private fun initListeners() {
        /*   리스너 초기화 함수   */

        binding.FollowerTouchImage.setOnClickListener {
            val intent = Intent(context, FollowInfoActivity::class.java)
            intent.putExtra("userId", Auth._userId.value)
            intent.putExtra("nickName", Auth._nickName.value)
            intent.putExtra("type", "follower")
            intent.putExtra("follower", binding.FollowerCount.text.toString().toInt())
            intent.putExtra("following", binding.FollowingCount.text.toString().toInt())
            startActivity(intent)
        }

        binding.FollowingTouchImage.setOnClickListener {
            val intent = Intent(context, FollowInfoActivity::class.java)
            intent.putExtra("userId", Auth._userId.value)
            intent.putExtra("nickName", Auth._nickName.value)
            intent.putExtra("type", "following")
            intent.putExtra("follower", binding.FollowerCount.text.toString().toInt())
            intent.putExtra("following", binding.FollowingCount.text.toString().toInt())
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
            val intent = Intent(context, MyFeedActivity::class.java)
            startActivity(intent)
        }

        /*   좋아요 한 게시물   */
        binding.MypageMenu3.setOnClickListener {
            val intent = Intent(context, LikeFeedActivity::class.java)
            startActivity(intent)
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

        /*   사용자 정보 Observer   */
        userInfoViewModel.userInfo.observe(viewLifecycleOwner) {
            if(it.userId.isNotEmpty()) {
                setUserProfile(it)
            }
        }
    }

    //사용자 정보 설정
    private fun setUserProfile(userinfo: VectoService.UserInfoResponse) {
        binding.MyFeedCount.text = userinfo.feedCount.toString()
        binding.FollowerCount.text = userinfo.followerCount.toString()
        binding.FollowingCount.text = userinfo.followingCount.toString()

    }

}