package com.vecto_example.vecto.ui.mypage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypageBinding
import com.vecto_example.vecto.dialog.UserProfileImageDialog
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.inquiry.InquiryActivity
import com.vecto_example.vecto.ui.followinfo.FollowInfoActivity
import com.vecto_example.vecto.ui.likefeed.LikeFeedActivity
import com.vecto_example.vecto.ui.myfeed.MyFeedActivity
import com.vecto_example.vecto.ui.myinfo.MyInfoActivity
import com.vecto_example.vecto.ui.notice.NoticeActivity
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModel
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModelFactory
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils
import kotlinx.coroutines.launch

class MypageFragment : Fragment() {
    lateinit var binding: FragmentMypageBinding

    private val userInfoViewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
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

        if(Auth.userId.value != "")
            userInfoViewModel.getUserInfo(Auth.userId.value.toString())

        if(Auth.loginFlag.value == true) {
            initObservers()
            initListeners()
        }
    }

    private fun initListeners() {
        /*   리스너 초기화 함수   */
        binding.ProfileImage.setOnClickListener {
            UserProfileImageDialog(requireContext(), Auth.profileImage.value).showDialog()
        }

        binding.FollowerTouchImage.setOnClickListener {
            val intent = Intent(context, FollowInfoActivity::class.java)
            intent.putExtra("userId", Auth.userId.value)
            intent.putExtra("nickName", Auth.nickName.value)
            intent.putExtra("type", "follower")
            intent.putExtra("follower", binding.FollowerCount.text.toString().toInt())
            intent.putExtra("following", binding.FollowingCount.text.toString().toInt())
            startActivity(intent)
        }

        binding.FollowingTouchImage.setOnClickListener {
            val intent = Intent(context, FollowInfoActivity::class.java)
            intent.putExtra("userId", Auth.userId.value)
            intent.putExtra("nickName", Auth.nickName.value)
            intent.putExtra("type", "following")
            intent.putExtra("follower", binding.FollowerCount.text.toString().toInt())
            intent.putExtra("following", binding.FollowingCount.text.toString().toInt())
            startActivity(intent)
        }

        /*   계정 설정   */
        binding.MypageMenu1.setOnClickListener {
            val intent = Intent(context, MyInfoActivity::class.java)
            startActivity(intent)
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
            val intent = Intent(context, InquiryActivity::class.java)
            startActivity(intent)
        }

        /*   문의하기   */
        binding.myPageNoticeMenu.setOnClickListener {
            val intent = Intent(context, NoticeActivity::class.java)
            startActivity(intent)
        }

        /*   로그아웃   */
        binding.MypageMenu5.setOnClickListener {
            userInfoViewModel.postLogout()
        }
    }

    private fun initObservers() {
        Auth.profileImage.observe(viewLifecycleOwner) {
            LoadImageUtils.loadUserProfileImage(requireContext(), binding.ProfileImage, Auth.profileImage.value)
        }

        Auth.nickName.observe(viewLifecycleOwner) {
            binding.UserNameText.text = Auth.nickName.value
        }

        /*   사용자 정보 Observer   */
        userInfoViewModel.userInfo.observe(viewLifecycleOwner) {
            if(it.userId.isNotEmpty()) {
                setUserProfile(it)
            }
        }

        userInfoViewModel.postLogoutResult.observe(viewLifecycleOwner) {
            if(it){
                SaveLoginDataUtils.deleteData(requireContext())
                ToastMessageUtils.showToast(requireContext(), getString(R.string.logout_success))
                (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.SearchFragment)
            }
        }

        lifecycleScope.launch {
            userInfoViewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(requireContext(), it.userToken.accessToken, it.userToken.refreshToken)

                if(it.function == UserInfoViewModel.Function.PostLogout.name){
                    userInfoViewModel.postLogout()
                }
            }
        }



        userInfoViewModel.errorMessage.observe(viewLifecycleOwner) {
            ToastMessageUtils.showToast(requireContext(), getString(it))

            if(it == R.string.expired_login){
                SaveLoginDataUtils.deleteData(requireContext())
                (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.SearchFragment)
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