package com.vecto_example.vecto.ui.myinfo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityMyInfoBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.ServerResponse
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File

class MyInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyInfoBinding

    private val viewModel: MypageSettingViewModel by viewModels{
        MypageSettingViewModelFactory(UserRepository(VectoService.create()))
    }

    private lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
        initObservers()
        initListeners()

        setGalleryResult()
        setCropResult()
    }

    private fun initObservers() {
        Auth._profileImage.observe(this) {
            LoadImageUtils.loadUserProfileImage(this, binding.ProfileImage, Auth._profileImage.value)
        }

        viewModel.idDuplicateResult.observe(this) {
            it.onSuccess {
                viewModel.idCheckFinished = true
                Toast.makeText(this, "사용 가능한 아이디 입니다.", Toast.LENGTH_SHORT).show()
            }.onFailure { failException ->
                viewModel.idCheckFinished = false
                binding.editTextID.isEnabled = true

                if(failException.message == ServerResponse.FAIL_DUPLICATED_USERID.code){
                    Toast.makeText(this, "중복된 아이디입니다.", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.updateResult.observe(this) { it ->
            it.onSuccess {
                Toast.makeText(this, "변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()

                if(Auth.provider == "vecto"){

                    val newID: String? = if(binding.editTextID.text.isEmpty())
                        null
                    else
                        binding.editTextID.text.toString()

                    val newPW: String? = if(binding.editTextPW.text.isEmpty())
                        null
                    else
                        binding.editTextPW.text.toString()

                    saveLoginInformation(newID, newPW)
                }

                Auth._nickName.value = binding.editTextNickname.text.toString()
                finish()
            }

            it.onFailure {
                if(it.message == "FAIL"){
                    Toast.makeText(this, "변경에 실패하였습니다. 형식을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initListeners() {
        binding.BackButton.setOnClickListener {
            finish()
        }

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
                Toast.makeText(this, "기존 아이디와 동일합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.WriteDoneButton.setOnClickListener{
            if(Auth.provider == "vecto"){
                var newID: String? = null
                var newPW: String? = null
                var nickname = Auth._nickName.value

                if(binding.editTextID.text.toString() != Auth._userId.value.toString())//ID변경이 있는 경우
                {
                    if(viewModel.idCheckFinished) {
                        if(viewModel.checkValidation(MypageSettingViewModel.Type.ID, binding.editTextID.text.toString())){
                            newID = binding.editTextID.text.toString()
                        }
                        else {
                            Toast.makeText(this, "아이디 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }
                    else{
                        Toast.makeText(this, "아이디 중복검사를 해주세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                if(binding.editTextPW.text.isNotEmpty() || binding.editTextCheckPW.text.isNotEmpty())//비밀변호 변경이 있는 경우
                {
                    if(binding.editTextPW.text != binding.editTextCheckPW.text) {
                        Toast.makeText(this, "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    else
                    {
                        if(viewModel.checkValidation(MypageSettingViewModel.Type.PASSWORD, binding.editTextPW.text.toString()))
                            newPW = binding.editTextPW.text.toString()
                        else{
                            Toast.makeText(this, "영어, 숫자, 특수문자(@#\$%^&+=!)를 포함한 8~20글자 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }
                }

                if(binding.editTextNickname.text.toString() != Auth._nickName.value.toString()) //닉네임 변경이 있는 경우
                {
                    if(viewModel.checkValidation(MypageSettingViewModel.Type.NICKNAME, binding.editTextNickname.text.toString())) {
                        nickname = binding.editTextNickname.text.toString()
                    }
                    else {
                        Toast.makeText(this, "닉네임 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                viewModel.updateUserProfile(VectoService.UserUpdateData(newID, newPW, "vecto", nickname))
            } else {
                var nickname = Auth._nickName.value

                if(binding.editTextNickname.text.toString() != Auth._nickName.value.toString()) //닉네임 변경이 있는 경우
                {
                    if(viewModel.checkValidation(MypageSettingViewModel.Type.NICKNAME, binding.editTextNickname.text.toString())) {
                        nickname = binding.editTextNickname.text.toString()
                    }
                    else {
                        Toast.makeText(this, "닉네임 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                viewModel.updateUserProfile(VectoService.UserUpdateData(null, null, "kakao", nickname))
            }
        }
    }

    private fun initUI() {
        if(Auth.provider == "kakao"){
            binding.emailText.text = "카카오 계정 로그인"
            binding.emailBox.setImageResource(R.drawable.mypage_emailkakao_box)

            setVisibilityGone()
        } else {
            binding.editTextID.setText(Auth._userId.value.toString())
            binding.emailText.text = Auth.email.toString()
        }

        binding.editTextNickname.setText(Auth._nickName.value.toString())
    }

    private fun setVisibilityGone() {
        binding.idTitle.visibility = View.GONE
        binding.idBox.visibility = View.GONE
        binding.editTextID.visibility = View.GONE
        binding.idCheckButton.visibility = View.GONE
        binding.idCheckButtonText.visibility = View.GONE
        binding.pwTitle.visibility = View.GONE
        binding.pwBox.visibility = View.GONE
        binding.editTextPW.visibility = View.GONE
        binding.pwcheckTitle.visibility = View.GONE
        binding.pwcheckBox.visibility = View.GONE
        binding.editTextCheckPW.visibility = View.GONE
    }

    private fun saveLoginInformation(userId: String?, password: String?) {
        val sharedPreferences = this.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        // 기존 정보 삭제
        if(userId != null)
            editor.remove("userId")
        if(password != null)
            editor.remove("password")
        editor.apply()

        // 새로운 정보 저장
        if(userId != null)
            editor.putString("username", userId)
        if(password != null)
            editor.putString("password", password)

        editor.apply()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

        return super.dispatchTouchEvent(ev)
    }

    /*   uCrop   */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        galleryResultLauncher.launch(intent) // 갤러리 결과를 galleryResultLauncher로 받음
    }

    private fun setGalleryResult() {
        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    startCrop(imageUri) // 이미지를 UCrop으로 전달
                }
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped.jpg"
        val destinationUri = Uri.fromFile(File(this.cacheDir, destinationFileName))
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(uCropOptions())
            .withAspectRatio(1f, 1f) // 1:1 비율로 자르기
            .getIntent(this)
        cropResultLauncher.launch(uCropIntent)
    }

    private fun uCropOptions(): UCrop.Options {
        val options = UCrop.Options()
        // 하단 컨트롤 숨기기
        options.setHideBottomControls(true)
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.black))
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black))
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white))

        // 격자 선 숨기기
        options.setCropGridRowCount(0)
        options.setCropGridColumnCount(0)
        return options
    }

    private fun setCropResult() {
        cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                val resultUri = UCrop.getOutput(result.data!!)
                if(resultUri != null) {
                    addImage(resultUri)
                }

            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }

    //Crop 된 이미지를 전달 받아 adapter 에 추가 하기 위한 전처리 작업 수행
    private fun addImage(newImageUri: Uri) {
        // 이미지 압축
        val compressedBytes = compressImage(newImageUri)
        saveCompressedImage(compressedBytes)
    }

    //이미지 압축을 진행
    private fun compressImage(uri: Uri): ByteArray {
        val outStream = ByteArrayOutputStream()
        val inputStream = this.contentResolver.openInputStream(uri)

        if (inputStream == null) {
            Log.e("MyInfoActivity", "Failed to open InputStream for the provided Uri: $uri")
            return byteArrayOf()  // empty array
        }

        inputStream.use { stream ->
            val original = BitmapFactory.decodeStream(stream)

            if(original == null){
                Log.e("MyInfoActivity", "Failed to decode the image from Uri: $uri")
                return byteArrayOf()
            }

            val resizedImage = Bitmap.createScaledBitmap(original, 1080, 1080, true)

            resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
        }

        return outStream.toByteArray()
    }

    //압축된 이미지를 저장 후 adapter 에 저장
    private fun saveCompressedImage(compressedBytes: ByteArray) {
        val filename = "compressed_image.jpeg"
        val file = File(this.cacheDir, filename)
        file.outputStream().use { it.write(compressedBytes) }

        viewModel.uploadProfileImage(Uri.fromFile(file))
    }
}