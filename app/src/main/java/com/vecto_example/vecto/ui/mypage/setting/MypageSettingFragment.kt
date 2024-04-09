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
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypageSettingBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.LoadImageUtils
import com.yalantis.ucrop.UCrop
import java.io.File

class MypageSettingFragment : Fragment() {
    lateinit var binding: FragmentMypageSettingBinding
    private val viewModel: MypageSettingViewModel by viewModels{
        MypageSettingViewModelFactory(UserRepository(VectoService.create()))
    }

    lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageSettingBinding.inflate(inflater, container, false)

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
        binding.editTextID.setText(Auth._userId.value.toString())
        binding.editTextNickname.setText(Auth._nickName.value.toString())
    }

    private fun initObservers() {

        Auth._profileImage.observe(viewLifecycleOwner) {
            LoadImageUtils.loadProfileImage(requireContext(), binding.ProfileImage)
        }

        Auth._nickName.observe(viewLifecycleOwner) {
            binding.UserNameText.text = Auth._nickName.value
        }

        viewModel.idDuplicateFlag.observe(viewLifecycleOwner) {
            it.onSuccess { isDuplicate ->
                if(isDuplicate){
                    Toast.makeText(requireContext(), "사용 가능한 아이디 입니다.", Toast.LENGTH_SHORT).show()

                }
                else {
                    binding.editTextID.isEnabled = true
                    Toast.makeText(requireContext(), "중복된 아이디입니다.", Toast.LENGTH_SHORT).show()
                }
            }
                .onFailure {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
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
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun initListeners() {

        binding.ProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/jpeg"
            openGallery()
        }

        binding.idCheckButton.setOnClickListener {
            if(binding.editTextID.text.toString() != Auth._userId.value.toString()){    //ID 변경이 있는 경우
                binding.editTextID.isEnabled = false    //수정이 불가능하게 변경
                viewModel.checkIdDuplicate(binding.editTextID.text.toString())
            }
            else{
                Toast.makeText(requireContext(), "기존 아이디와 동일합니다.", Toast.LENGTH_SHORT).show()
            }

        }

        binding.WriteDoneButton.setOnClickListener{
            var newID: String? = null
            var newPW: String? = null

            if(binding.editTextID.text.toString() != Auth._userId.value.toString())//ID변경이 있는 경우
            {
                if(viewModel.idDuplicateFlag.value?.getOrNull() == true) {
                    if(viewModel.checkValidation(MypageSettingViewModel.Type.ID, binding.editTextID.text.toString())){
                        newID = binding.editTextID.text.toString()
                    }
                    else {
                        Toast.makeText(requireContext(), "아이디 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                else{
                    Toast.makeText(requireContext(), "아이디 중복검사를 해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if(binding.editTextPW.text.isNotEmpty() || binding.editTextCheckPW.text.isNotEmpty())//비밀변호 변경이 있는 경우
            {
                if(binding.editTextPW.text != binding.editTextCheckPW.text) {
                    Toast.makeText(requireContext(), "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else
                {
                    if(viewModel.checkValidation(MypageSettingViewModel.Type.PASSWORD, binding.editTextPW.text.toString()))
                        newPW = binding.editTextPW.text.toString()
                    else{
                        Toast.makeText(requireContext(), "영어, 숫자, 특수문자(@#\$%^&+=!)를 포함한 8~20글자 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            }

            if(binding.editTextNickname.text.toString() != Auth._nickName.value.toString()) //닉네임 변경이 있는 경우
            {
                if(viewModel.checkValidation(MypageSettingViewModel.Type.NICKNAME, binding.editTextNickname.text.toString())) {

                }
                else {
                    Toast.makeText(requireContext(), "닉네임 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            viewModel.updateUserProfile(VectoService.UserUpdateData(newID, newPW, "vecto", binding.editTextNickname.text.toString()))
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