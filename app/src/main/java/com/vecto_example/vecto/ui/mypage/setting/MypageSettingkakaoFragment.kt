package com.vecto_example.vecto.ui.mypage.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypageSettingkakaoBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.mypage.MypageViewModel
import com.vecto_example.vecto.ui.mypage.MypageViewModelFactory
import com.vecto_example.vecto.utils.LoadImageUtils
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MypageSettingkakaoFragment : Fragment() {
    lateinit var binding: FragmentMypageSettingkakaoBinding
    private val viewModel: MypageSettingViewModel by viewModels{
        MypageSettingViewModelFactory(UserRepository(VectoService.create()))
    }

    lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageSettingkakaoBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initObservers()
        initListeners()

        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    startCrop(imageUri) // 이미지를 UCrop으로 전달
                }
            }
        }

        cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                val resultUri = UCrop.getOutput(result.data!!)
                if(resultUri != null)
                    viewModel.uploadProfileImage(resultUri)

            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }

    private fun initUI() {
        binding.editTextNickname.setText(Auth._nickName.value.toString())
    }

    private fun initListeners() {
        binding.ProfileImage.setOnClickListener {
            // 갤러리에서 이미지 선택
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/jpeg"
            openGallery()
        }

        binding.WriteDoneButton.setOnClickListener{
            if(Auth._nickName.value != binding.editTextNickname.text.toString()) {
                viewModel.updateUserProfile(VectoService.UserUpdateData(null, null, "kakao", binding.editTextNickname.text.toString()))
            }
            else {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun initObservers() {
        Auth._profileImage.observe(viewLifecycleOwner) {
            LoadImageUtils.loadProfileImage(requireContext(), binding.ProfileImage)
        }

        Auth._nickName.observe(viewLifecycleOwner) {
            binding.UserNameText.text = Auth._nickName.value
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { it ->
            it.onSuccess {
                Toast.makeText(requireContext(), "변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                Auth._nickName.value = binding.editTextNickname.text.toString()
                parentFragmentManager.popBackStack()
            }

            it.onFailure {
                if(it.message == "FAIL"){
                    Toast.makeText(requireContext(), "변경에 실패하였습니다. 형식을 확인해주세요.", Toast.LENGTH_SHORT).show()

                }
                else{
                    Toast.makeText(requireContext(), getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        galleryResultLauncher.launch(intent) // 갤러리 결과를 galleryResultLauncher로 받음
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, destinationFileName))
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(uCropOptions())
            .withAspectRatio(1f, 1f) // 1:1 비율로 자르기
            .getIntent(requireContext())
        cropResultLauncher.launch(uCropIntent)
    }

    private fun uCropOptions(): UCrop.Options {
        val options = UCrop.Options()
        // 하단 컨트롤 숨기기
        options.setHideBottomControls(true)
        options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
        options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
        options.setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.white))

        // 격자 선 숨기기
        options.setCropGridRowCount(0)
        options.setCropGridColumnCount(0)
        return options
    }

}