package com.vecto_example.vecto.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.kakao.sdk.common.KakaoSdk
import com.vecto_example.vecto.BuildConfig
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityMainBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.login.LoginViewModel
import com.vecto_example.vecto.ui.login.LoginViewModelFactory
import com.vecto_example.vecto.ui.onefeed.OneFeedActivity
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        KakaoSdk.init(this, BuildConfig.KAKAO_KEY)

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
                loginViewModel.isLoginFinished.observe(this){ isLoginFinish ->
                    if(isLoginFinish){
                        val intent = Intent(this, OneFeedActivity::class.java)
                        intent.putExtra("feedId", it)
                        intent.putExtra("isComment", true)

                        this.startActivity(intent)
                    }
                }
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

        intent.getStringExtra("type").let {
            when(it){
                "EditCourseFragment" -> {
                    updateBottomNavigationSelection(R.id.EditCourseFragment)
                    navController.navigate(R.id.EditCourseFragment)
                }

                "TodayCourseFragment" -> {
                    updateBottomNavigationSelection(R.id.TodayCourseFragment)
                    navController.navigate(R.id.TodayCourseFragment)
                }
            }
        }

        intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                it.data?.let { uri ->
                    val feedId = uri.getQueryParameter("feedId")

                    if(!feedId.isNullOrEmpty()){

                        loginViewModel.isLoginFinished.observe(this){ isLoginFinish ->
                            if(isLoginFinish){
                                val intent = Intent(this, OneFeedActivity::class.java)
                                intent.putExtra("feedId", feedId.toInt())
                                intent.putExtra("isComment", false)

                                this.startActivity(intent)
                            }
                        }
                    }

                }
            }
        }


        if(Auth.loginFlag.value == false) {
            sendLoginRequest()
        }

        initObservers()
    }

    private fun sendLoginRequest() {
        val sharedPreferences = this.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

        // 저장된 로그인 정보 가져오기
        loginViewModel.userId = sharedPreferences.getString("userId", null)
        val accessToken = sharedPreferences.getString("accessToken", null)
        val refreshToken = sharedPreferences.getString("refreshToken", null)
        val fcmToken = sharedPreferences.getString("FCM", null)

        if(loginViewModel.userId != null && accessToken != null && refreshToken != null && fcmToken != null) {
            Auth.setToken(VectoService.UserToken(accessToken, refreshToken))
            loginViewModel.reissueToken()
        } else {
            loginViewModel.loginFinish()
        }

    }


    private fun initObservers() {
        loginViewModel.userInfoResult.observe(this) {
            Auth.setLoginFlag(true)

            Auth.setUserData(it.provider, it.userId, it.profileUrl, it.nickName, it.email)
            loginViewModel.loginFinish()
        }

        lifecycleScope.launch {
            loginViewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(this@MainActivity, it.accessToken, it.refreshToken)
                loginViewModel.getUserInfo()
            }
        }

        loginViewModel.errorMessage.observe(this){
            ToastMessageUtils.showToast(this, getString(it))
            loginViewModel.loginFinish()

            if(it == R.string.expired_login)
                SaveLoginDataUtils.deleteData(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        /*마이페이지 Intent 관련*/
        val fragmentId = intent.getIntExtra("MyPage", -1)
        if (fragmentId != -1) {
            updateBottomNavigationSelection(R.id.MypageFragment)
            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.MypageFragment)
        }
    }

    override fun onResume() {
        super.onResume()

        if(loginViewModel.isLoginFinished.value == false && Auth.loginFlag.value == true){    //재시작 된 경우

            sendLoginRequest()
        }
    }
    interface ScrollToTop {
        fun scrollToTop()
    }

    fun updateBottomNavigationSelection(menuItemId: Int) {
        binding.navView.selectedItemId = menuItemId
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        currentFocus?.let { view ->
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}