package com.example.vecto.ui_bottom

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.vecto.R
import com.example.vecto.data.Auth
import com.example.vecto.databinding.FragmentMypageSettingBinding
import com.example.vecto.retrofit.VectoService
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MypageSettingFragment : Fragment() {
    lateinit var binding: FragmentMypageSettingBinding

    lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    var idFlag = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageSettingBinding.inflate(inflater, container, false)

        Auth._profileImage.observe(viewLifecycleOwner) { image ->
            if (image == null) {
                binding.ProfileImage.setImageResource(R.drawable.profile_basic)
            }
            else//사용자 정의 이미지가 있을 경우
            {
                Glide.with(this)
                    .load(Auth._profileImage.value)
                    .circleCrop()
                    .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .into(binding.ProfileImage)
            }
        }

        Auth._nickName.observe(viewLifecycleOwner) {
            binding.UserNameText.text = Auth._nickName.value
        }

        binding.ProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/jpeg"
            openGallery()
        }

        binding.editTextID.setText(Auth._userId.value.toString())
        binding.editTextNickname.setText(Auth._nickName.value.toString())


        binding.WriteDoneButton.setOnClickListener{
            var newID: String? = null
            var newPW: String? = null

            if(binding.editTextID.text.toString() != Auth._userId.value.toString())//ID변경이 있는 경우
            {
                if(idFlag)
                    newID = binding.editTextID.text.toString()
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
                    if(checkPW())
                        newPW = binding.editTextPW.text.toString()
                    else{
                        Toast.makeText(requireContext(), "영어, 숫자, 특수문자(@#\$%^&+=!)를 포함한 8~20글자 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            }

            sendRequest(VectoService.UserUpdateData(newID, newPW, "vecto", binding.editTextNickname.text.toString()))
        }

        return binding.root
    }

    private fun sendRequest(updateData: VectoService.UserUpdateData) {
        val vectoService = VectoService.create()

        val call = vectoService.updateUser("Bearer ${Auth.token}", updateData)
        call.enqueue(object : Callback<VectoService.VectoResponse<String>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<String>>, response: Response<VectoService.VectoResponse<String>>) {
                if (response.isSuccessful) {
                    // 성공
                    Log.d("UserUpdate", "업데이트 성공 : " + response.message())
                    Toast.makeText(requireContext(), "변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    Auth._nickName.value = updateData.nickName
                    parentFragmentManager.popBackStack()
                } else {
                    // 실패
                    Log.d("UserUpdate", "업데이트 실패 : " + response.errorBody()?.string())
                    Toast.makeText(requireContext(), "오류가 발생했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onFailure(call: Call<VectoService.VectoResponse<String>>, t: Throwable) {
                // 네트워크 등 기타 에러 처리
                Toast.makeText(requireContext(), getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                Log.d("UserUpdate", "업데이트 실패 : " + t.message)
            }
        })
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

    private fun uploadProfileImage(image: Uri) {
        val vectoService = VectoService.create()

        val file = File(image.path!!)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)


        val call = vectoService.uploadImage("Bearer ${Auth.token}", imagePart)
        call.enqueue(object : Callback<VectoService.VectoResponse<String>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<String>>, response: Response<VectoService.VectoResponse<String>>) {
                if (response.isSuccessful) {
                    Log.d("PROFILE_IMAGE", "업로드 성공 : ")
                    Auth._profileImage.value = response.body()?.result
                } else {
                    // 서버 에러 처리
                    Log.d("PROFILE_IMAGE", "업로드 실패 : " + response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<String>>, t: Throwable) {
                Log.d("PROFILE_IMAGE", "업로드 실패 : " + t.message)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    uploadProfileImage(resultUri)

            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }

    private fun checkPW(): Boolean{
        val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$")
        val input = binding.editTextPW.text.toString()

        if (input.isEmpty()) {
            return false
        }

        if (!passwordPattern.matches(input)) {
            Toast.makeText(requireContext(), "영어, 숫자, 특수문자(@#$%^&+=!)를 포함한 8~20글자 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

}