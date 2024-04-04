package com.vecto_example.vecto.ui.mypage.setting

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ValidationUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MypageSettingViewModel (private val repository: UserRepository): ViewModel() {
    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> = _updateResult

    val idDuplicateFlag = MutableLiveData<Result<Boolean>>()

    enum class Type {
        ID, NICKNAME, PASSWORD
    }

    fun updateUserProfile(updateData: VectoService.UserUpdateData){

        viewModelScope.launch {
            val result = repository.patchUserProfile(updateData)
            _updateResult.value = result
        }

    }
    fun uploadProfileImage(image: Uri){
        val file = File(image.path!!)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

        viewModelScope.launch {
            try {
                repository.postUploadProfileImage(imagePart)
            } catch (e: Exception) {
                throw Exception("uploadProfileImage Failed")
            } finally {

            }
        }
    }

    fun checkIdDuplicate(userId: String) {
        viewModelScope.launch {
            val result = repository.checkUserId(userId)

            idDuplicateFlag.value = result
        }
    }

    private fun handleValidationResult(validationResult: ValidationUtils.ValidationResult, type: Type): Boolean {
        /*   ValidationResult 에 따른 결과 처리   */

        return when(validationResult) {
            ValidationUtils.ValidationResult.VALID -> {
                when (type) {
                    Type.ID -> {

                    }
                    Type.NICKNAME -> {

                    }
                    Type.PASSWORD -> {

                    }
                }
                true
            }

            ValidationUtils.ValidationResult.EMPTY -> {
                when (type) {
                    Type.ID -> {

                    }
                    Type.NICKNAME -> {

                    }
                    Type.PASSWORD -> {

                    }
                }
                false
            }

            ValidationUtils.ValidationResult.INVALID_FORMAT -> {
                when (type) {
                    Type.ID -> {

                    }
                    Type.NICKNAME -> {

                    }
                    Type.PASSWORD -> {

                    }
                }
                false
            }
        }
    }

    fun checkValidation(type: Type, input: String): Boolean {
        return when (type){
            Type.ID -> {
                handleValidationResult(ValidationUtils.isValidId(input), Type.ID)
            }

            Type.PASSWORD -> {
                handleValidationResult(ValidationUtils.isValidPw(input), Type.PASSWORD)
            }

            Type.NICKNAME -> {
                handleValidationResult(ValidationUtils.isValidNickname(input), Type.NICKNAME)
            }
        }
    }

}