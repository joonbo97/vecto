package com.example.vecto.ui_bottom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.vecto.LoginActivity
import com.example.vecto.R
import com.example.vecto.data.Auth
import com.example.vecto.databinding.FragmentMypageBinding
import com.example.vecto.retrofit.VectoService
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MypageFragment : Fragment() {
    lateinit var binding: FragmentMypageBinding
    lateinit var profileImageView: ImageView

    lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        if(!Auth.loginFlag.value!!){
            goLogin()
        }

        profileImageView = binding.ProfileImage

        Auth._profileImage.observe(viewLifecycleOwner) { image ->
            if (image == null) {
                profileImageView.setImageResource(R.drawable.profile_basic)
            }
            else//사용자 정의 이미지가 있을 경우
            {
                Glide.with(this)
                    .load(Auth._profileImage.value)
                    .circleCrop()
                    .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .into(profileImageView)
            }
        }

        Auth._nickName.observe(viewLifecycleOwner) {
            binding.UserNameText.text = Auth._nickName.value
        }

        profileImageView.setOnClickListener {
            // 갤러리에서 이미지 선택
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/jpeg"
            //pickImage.launch(intent)
            openGallery()
        }

        binding.MypageMenu1.setOnClickListener {
            // 클릭 이벤트 처리

            val navController = findNavController()
            navController.navigate(R.id.MypageSettingFragment)
        }


        binding.MypageMenu2.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.MypagePostFragment)
        }

        binding.MypageMenu3.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.MypageLikepostFragment)
        }

        binding.MypageMenu4.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.MypageInquiryFragment)
        }


        return binding.root
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

    private fun goLogin(){
        val intent = Intent(context, LoginActivity::class.java) //Login 화면으로 이동
        startActivity(intent)
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
}