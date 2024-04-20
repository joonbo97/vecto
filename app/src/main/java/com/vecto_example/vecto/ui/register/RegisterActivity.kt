package com.vecto_example.vecto.ui.register

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.vecto_example.vecto.retrofit.VectoService
import com.google.android.material.button.MaterialButton
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityRegisterBinding
import com.vecto_example.vecto.utils.ServerResponse
import com.vecto_example.vecto.utils.ValidationUtils
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val registerViewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(UserRepository(VectoService.create()))
    }

    private var timer: CountDownTimer? = null

    enum class Type {
        ID, NICKNAME, PASSWORD, EMAIL, EMAILVERIFY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*   초기화   */
        initObservers()

        /*   hasFocus를 사용하여, 작성이 완료되면 상단에 알림을 출력하도록 구현   */

        binding.editTextNickname.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkNickname()
            }
        }

        /*binding.editTextPassword.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkPW()
                if(binding.editTextPassword.text.isNotEmpty())
                    checkPWverify()
            }
        }*/

        /*   TextChangedListener를 사용하여 변경이 생길 때마다 갱신   */
        binding.editTextPassword.addTextChangedListener {
            checkPW()
            if(binding.editTextCheckPassword.text.isNotEmpty())
                checkPWverify()
        }

        binding.editTextCheckPassword.addTextChangedListener {
            if(binding.editTextPassword.text.isNotEmpty())
                checkPWverify()
        }

        binding.RegesterEmailCheck.setOnClickListener {
            if(checkEmail()) {
                binding.editTextEmail.isEnabled = false
                registerViewModel.sendMail(binding.editTextEmail.text.toString())
            }
        }

        /*   ID 중복 검사   */
        binding.RegesterIDCheck.setOnClickListener {
            if(checkID())
            {
                binding.editTextID.isEnabled = false
                registerViewModel.checkIdDuplicate(binding.editTextID.text.toString())
            }
        }

        /*   가입 진행   */
        binding.RegisterButton.setOnClickListener {
            if(
                checkID() && checkNickname() && checkPW() && checkPWverify() && checkEmail() && checkEmailverify() &&
                        registerViewModel.idCheckFinished && registerViewModel.isMailSent
            ){
                registerViewModel.registerRequest(VectoService.RegisterRequest(
                    binding.editTextID.text.toString(), binding.editTextPassword.text.toString(), "vecto",
                    binding.editTextNickname.text.toString(), binding.editTextEmail.text.toString(), binding.editTextEmailCode.text.toString().toInt()
                ))
            }
            else{
                Toast.makeText(this, "작성을 완료해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initObservers() {
        /*   ID 중복 검사   */
        registerViewModel.idDuplicateResult.observe(this) {
            it.onSuccess {
                registerViewModel.idCheckFinished = true
                Toast.makeText(this, "사용 가능한 아이디 입니다.", Toast.LENGTH_SHORT).show()
                checkID()
            }.onFailure { failException ->
                registerViewModel.idCheckFinished = false
                binding.editTextID.isEnabled = true
                checkID()

                if(failException.message == ServerResponse.FAIL_DUPLICATED_USERID.code){
                    Toast.makeText(this, "중복된 아이디입니다.", Toast.LENGTH_SHORT).show()
                    updateText(binding.IdNotificationText, "중복된 아이디입니다.",
                        R.color.red
                    )
                }
                else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

        /*   메일 발송   */
        registerViewModel.sendMailResult.observe(this) {
            it.onSuccess {
                registerViewModel.isMailSent = true
                Toast.makeText(this, "메일이 발송되었습니다. 인증코드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                startEmailTimer()
                checkEmail()
            }.onFailure { failException ->
                registerViewModel.isMailSent = false
                binding.editTextEmail.isEnabled = true
                checkEmail()

                if(failException.message == ServerResponse.FAIL_DUPLICATED_EMAIL.code){
                    Toast.makeText(this, "중복된 이메일입니다.", Toast.LENGTH_SHORT).show()
                    updateText(binding.EmailNotificationText, "중복된 이메일입니다.",
                        R.color.red
                    )
                }
                else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

        /*   회원 가입   */
        registerViewModel.registerResult.observe(this){ registerResult ->
            registerResult.onSuccess {
                Toast.makeText(this, "회원가입에 성공하였습니다. 로그인을 진행해 주세요.", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                when (it.message) {
                    ServerResponse.FAIL_EMAILCODE_INVALID.code -> {
                        Toast.makeText(this, "인증번호가 만료되었거나 이메일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                        updateText(binding.EmailcodeNotificationText, "인증번호가 일치하지 않습니다.",
                            R.color.red
                        )
                    }
                    "ERROR" -> {
                        Toast.makeText(this, R.string.APIErrorToastMessage, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "회원가입에 실패하였습니다. 입력한 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        /*   로딩   */
        registerViewModel.isLoading.observe(this){
            if(it){
                binding.progressBarCenter.visibility = View.VISIBLE
            } else {
                binding.progressBarCenter.visibility = View.GONE
            }
        }
    }

    private fun startEmailTimer(){
        binding.RegesterEmailCheck.isEnabled = false
        binding.RegesterEmailCheck.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this,
            R.color.vecto_disable
        ))

        timer = object : CountDownTimer(180000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.RegesterEmailCheck.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.RegesterEmailCheck.text = "메일 발송"
                binding.RegesterEmailCheck.isEnabled = true
                binding.RegesterEmailCheck.backgroundTintList = null

                binding.editTextEmail.isEnabled = true
                registerViewModel.isMailSent = false
            }
        }.start()
    }

    private fun checkID(): Boolean{
        /*   아이디 형식 체크   */
        val input = binding.editTextID.text.toString()

        return handleValidationResult(ValidationUtils.isValidId(input), Type.ID)
    }

    private fun checkNickname(): Boolean{
        /*   닉네임 형식 체크   */
        val input = binding.editTextNickname.text.toString()

        return handleValidationResult(ValidationUtils.isValidNickname(input), Type.NICKNAME)
    }

    private fun checkPW(): Boolean{
        /*   비밀번호 형식 체크   */
        val input = binding.editTextPassword.text.toString()

        return handleValidationResult(ValidationUtils.isValidPw(input), Type.PASSWORD)
    }

    private fun checkPWverify(): Boolean{
        /*   비밀번호 확인 일치 체크   */
        if (binding.editTextPassword.text.toString() == binding.editTextCheckPassword.text.toString()) {
            updateText(binding.PwcheckNotificationText, "비밀번호가 일치 합니다.", R.color.green)

            return true
        }
        else {
            updateText(binding.PwcheckNotificationText, "비밀번호가 일치하지 않습니다.", R.color.red)

            return false
        }
    }

    private fun checkEmail():Boolean{
        /*   이메일 형식 체크   */
        val input = binding.editTextEmail.text.toString()

        return handleValidationResult(ValidationUtils.isValidEmail(input), Type.EMAIL)
    }

    private fun checkEmailverify(): Boolean{
        /*   이메일 인증 공란 확인   */
        val input = binding.editTextEmailCode.text.toString()

        return handleValidationResult(ValidationUtils.isValidEmailVerify(input), Type.EMAILVERIFY)
    }


    private fun handleValidationResult(validationResult: ValidationUtils.ValidationResult, type: Type): Boolean{
        /*   ValidationResult 에 따른 결과 처리   */

        return when(validationResult){
            ValidationUtils.ValidationResult.VALID -> {
                //유효함
                when (type) {
                    Type.ID -> {//ID
                        if(registerViewModel.idCheckFinished)//중복확인 완료
                        {
                            updateText(binding.IdNotificationText, "사용 가능한 아이디 입니다.", R.color.green)
                        }
                        else
                        {
                            updateText(binding.IdNotificationText, "중복확인을 진행해 주세요.",
                                R.color.vecto_warning
                            )
                        }
                    }
                    Type.NICKNAME -> {//Nickname
                        updateText(binding.NicknameNotificationText, "사용 가능한 닉네임 입니다.",
                            R.color.green
                        )
                    }
                    Type.PASSWORD -> {//PW
                        updateText(binding.PwNotificationText, "사용 가능한 비밀번호 입니다.", R.color.green)
                    }
                    Type.EMAIL -> {//Email
                        if(registerViewModel.isMailSent)
                        {
                            updateText(binding.EmailNotificationText, "메일을 발송하였습니다. 인증코드를 입력해주세요.",
                                R.color.green
                            )
                        }
                        else
                        {
                            updateText(binding.EmailNotificationText, "이메일 인증을 진행해주세요.",
                                R.color.vecto_warning
                            )
                        }

                    }
                    Type.EMAILVERIFY -> {//Email Code

                    }
                }

                true
            }

            ValidationUtils.ValidationResult.EMPTY -> {
                //비어있음
                when (type) {
                    Type.ID -> {//ID
                        updateText(binding.IdNotificationText, "아이디를 입력해주세요.", R.color.red)
                    }
                    Type.NICKNAME -> {//Nickname
                        updateText(binding.NicknameNotificationText, "닉네임을 입력해주세요.", R.color.red)
                    }
                    Type.PASSWORD -> {//PW
                        updateText(binding.PwNotificationText, "비밀번호를 입력해주세요.", R.color.red)
                    }
                    Type.EMAIL -> {//Email
                        updateText(binding.EmailNotificationText, "이메일을 입력해주세요.", R.color.red)

                    }
                    Type.EMAILVERIFY -> {//Email code
                        updateText(binding.EmailcodeNotificationText, "인증 코드를 입력해주세요.", R.color.red)
                    }
                }

                false
            }

            ValidationUtils.ValidationResult.INVALID_FORMAT -> {
                //형식에 맞지 않음
                when (type) {
                    Type.ID -> {//ID
                        updateText(binding.IdNotificationText, "올바르지 않은 아이디입니다.", R.color.red)
                    }
                    Type.NICKNAME -> {//Nickname
                        updateText(binding.NicknameNotificationText, "올바르지 않은 닉네임입니다.", R.color.red)
                    }
                    Type.PASSWORD -> {//PW
                        updateText(binding.PwNotificationText, "올바르지 않은 비밀번호입니다.", R.color.red)
                    }
                    Type.EMAIL -> {//Email
                        updateText(binding.EmailNotificationText, "올바르지 않은 이메일 입니다.", R.color.red)
                    }
                    Type.EMAILVERIFY ->{//Email Code

                    }
                }

                false
            }

        }
    }

    private fun updateText(textView: TextView, message: String, color: Int) {
        textView.text = message
        textView.setTextColor(ContextCompat.getColor(this, color))
    }


}