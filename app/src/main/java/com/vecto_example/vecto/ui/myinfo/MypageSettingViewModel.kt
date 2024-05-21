package com.vecto_example.vecto.ui.myinfo

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import com.vecto_example.vecto.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MypageSettingViewModel (private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModel() {
    private val _reissueResponse = MutableSharedFlow<VectoService.TokenUpdateEvent>(replay = 0)
    val reissueResponse = _reissueResponse.asSharedFlow()

    private val _updateResult = MutableLiveData<String>()
    val updateResult: LiveData<String> = _updateResult

    private val _idDuplicateResult = MutableLiveData<String>()
    val idDuplicateResult: LiveData<String> = _idDuplicateResult

    private val _uploadImageResult = MutableLiveData<String>(Auth.profileImage.value)
    val uploadImageResult: LiveData<String> = _uploadImageResult

    //탈퇴
    private val _deleteAccount = MutableLiveData<String>()
    val deleteAccount: LiveData<String> = _deleteAccount

    //에러
    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    //로딩
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    var idCheckFinished = false

    var imageUri: Uri? = null

    enum class Type {
        ID, NICKNAME, PASSWORD
    }

    enum class Function {
        UpdateUserProfile, UploadProfileImage, DeleteAccount
    }

    fun updateUserProfile(updateData: VectoService.UserUpdateData){
        startLoading()

        viewModelScope.launch {
            val result = userRepository.patchUserProfile(updateData)

            result.onSuccess {
                _updateResult.value = it
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.UpdateUserProfile.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.update_user_fail)
                    }
                }
            }
            endLoading()
        }
    }

    private fun reissueToken(function: String){
        viewModelScope.launch {
            val reissueResponse = tokenRepository.reissueToken()

            reissueResponse.onSuccess { //Access Token이 만료되어서 갱신됨
                _reissueResponse.emit(VectoService.TokenUpdateEvent(function, it))
            }.onFailure {
                when(it.message){
                    //아직 유효한 경우
                    ServerResponse.ACCESS_TOKEN_VALID_ERROR.code -> {}
                    //Refresh Token 만료
                    ServerResponse.REFRESH_TOKEN_INVALID_ERROR.code -> {
                        _errorMessage.postValue(R.string.expired_login)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
                endLoading()
            }
        }
    }
    fun uploadProfileImage(){
        if(imageUri == null) {
            _errorMessage.postValue(R.string.compress_error)
            return
        }

        startLoading()

        val file = File(imageUri?.path!!)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

        viewModelScope.launch {
            val uploadProfileImageResponse = userRepository.postUploadProfileImage(imagePart)

            uploadProfileImageResponse.onSuccess {
                _uploadImageResult.postValue(it)
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.UpdateUserProfile.name)
                    }
                    ServerResponse.FAIL.code -> {
                        _errorMessage.postValue(R.string.upload_image_fail)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }

            endLoading()
        }
    }

    fun checkIdDuplicate(userId: String) {
        startLoading()

        viewModelScope.launch {
            val checkUserIdResponse = userRepository.checkUserId(userId)

            checkUserIdResponse.onSuccess {
                _idDuplicateResult.postValue(it)
                idCheckFinished = true
            }.onFailure {
                when(it.message){
                    ServerResponse.FAIL_DUPLICATED_USERID.code ->{
                        _errorMessage.postValue(R.string.duplicate_id)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }
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

    fun accountCancellation(){
        if(Auth.provider == "kakao"){
            UserApiClient.instance.unlink { error ->
                if (error != null) {
                    _errorMessage.postValue(R.string.delete_kakao_error)
                }else {
                    deleteAccount()
                }
            }
        } else {
            deleteAccount()
        }
    }

    private fun deleteAccount(){
        viewModelScope.launch {
            val deleteResponse = userRepository.deleteAccount()

            deleteResponse.onSuccess {
                _deleteAccount.postValue(it)
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.UpdateUserProfile.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }
        }
    }

    fun startLoading(){
        _isLoading.postValue(true)
    }

    fun endLoading(){
        _isLoading.postValue(false)
    }

}