package com.vecto_example.vecto.ui.delete_account

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityDeleteAccountBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils
import kotlinx.coroutines.launch

class DeleteAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeleteAccountBinding
    private val viewModel: DeleteAccountViewModel by viewModels{
        DeleteAccountViewModelFactory(UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUI()
        initListener()
        initObservers()
    }

    private fun initObservers() {

        viewModel.deleteAccount.observe(this){
            ToastMessageUtils.showToast(this, getString(R.string.delete_success))

            SaveLoginDataUtils.deleteData(this)
            finish()
        }

        lifecycleScope.launch {
            viewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(this@DeleteAccountActivity, it.userToken.accessToken, it.userToken.refreshToken)

                if(it.function == DeleteAccountViewModel.Function.AccountCancellation.name){
                    viewModel.deleteAccount()
                }
            }
        }

        viewModel.errorMessage.observe(this) {
            ToastMessageUtils.showToast(this, getString(it))
        }
    }

    private fun initListener() {
        binding.BackButton.setOnClickListener {
            finish()
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

        binding.deleteButton.setOnClickListener {

        }
    }

    private fun setUI() {
        binding.titleText.text = getString(R.string.delete_account_title, Auth.nickName.value)

        binding.contentText.text = getString(R.string.delete_account_content, Auth.nickName.value)
    }
}