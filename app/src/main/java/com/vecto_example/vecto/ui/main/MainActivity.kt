package com.vecto_example.vecto.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityMainBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.login.LoginViewModel
import com.vecto_example.vecto.ui.login.LoginViewModelFactory
import com.vecto_example.vecto.ui.search.SearchFragment


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(UserRepository(VectoService.create()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        // 탭이 다시 선택될 때 스크롤을 최상위로 이동
        navView.setOnNavigationItemReselectedListener { _ ->
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
            val currentFragment = navHostFragment.childFragmentManager.fragments.first()
            if (currentFragment is ScrollToTop) {
                currentFragment.scrollToTop()
            }
        }


        /*Notification Intent 관련*/
        intent.getIntExtra("feedId", -1).let{
            if(it >= 0)//댓글 관련 알림인 경우
            {

                val intent = Intent(this, CommentActivity::class.java)
                intent.putExtra("feedID", it)
                this.startActivity(intent)
            }
        }

        /*글 수정 Intent 관련*/
        intent.getStringExtra("editCourse").let{
            if(it != null) {
                updateBottomNavigationSelection(R.id.EditCourseFragment)
                val bundle = bundleOf("selectedDateKey" to it)
                navController.navigate(R.id.EditCourseFragment, bundle)
            }
        }

        if(Auth.loginFlag.value == false){
            val sharedPreferences = this.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

            // 저장된 로그인 정보 가져오기
            val userId = sharedPreferences.getString("username", null) // 기본값은 null
            val password = sharedPreferences.getString("password", null) // 기본값은 null
            val provider = sharedPreferences.getString("provider", null) // 기본값은 null
            val fcmtoken = sharedPreferences.getString("FCM", null) // FCM 토큰 가져오기, 기본값은 null

            if(userId != null && fcmtoken != null) {
                loginViewModel.loginRequest(VectoService.LoginRequest(userId, password, fcmtoken))
            } else {
                loginViewModel.loginFinish()
            }
        }

        initObservers()
    }

    private fun initObservers() {
        loginViewModel.loginResult.observe(this){ loginResult ->
            loginResult.onSuccess {
                Auth.token = it

                loginViewModel.getUserInfo()
            }.onFailure {
                if(it.message == "FAIL"){
                    Toast.makeText(this, "로그인 정보가 일치하지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                loginViewModel.loginFinish()
            }
        }

        loginViewModel.userInfoResult.observe(this) { userInfoResult ->
            userInfoResult.onSuccess {
                Auth.setLoginFlag(true)

                Auth.setUserData(it.provider, it.userId, it.profileUrl, it.nickName, it.email)
                loginViewModel.loginFinish()
            }.onFailure {
                if(it.message == "E020"){
                    Toast.makeText(this, "사용자 정보가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                loginViewModel.loginFinish()
            }
        }
    }


    interface ScrollToTop {
        fun scrollToTop()
    }

    fun updateBottomNavigationSelection(menuItemId: Int) {
        binding.navView.selectedItemId = menuItemId
    }
}