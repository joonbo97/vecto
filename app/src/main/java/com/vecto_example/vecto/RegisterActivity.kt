package com.vecto_example.vecto

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.vecto_example.vecto.retrofit.VectoService
import com.google.android.material.button.MaterialButton
import com.vecto_example.vecto.databinding.ActivityRegisterBinding
import com.vecto_example.vecto.utils.ValidationUtils
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var emailSendButton: MaterialButton
    private var timer: CountDownTimer? = null

    private lateinit var editTextID: EditText
    private lateinit var editTextNickname: EditText
    private lateinit var editTextPW: EditText
    private lateinit var editTextPWCheck: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextEmailCode: EditText

    var idDuplicateFlag = false
    var emailDuplicateFlag = false

    enum class Type {
        ID, NICKNAME, PASSWORD, EMAIL, EMAILVERIFY
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*   초기화   */
        emailSendButton = binding.RegesterEmailCheck
        editTextID = binding.editTextID
        editTextNickname = binding.editTextNickname
        editTextPW = binding.editTextPassword
        editTextPWCheck = binding.editTextCheckPassword
        editTextEmail = binding.editTextEmail
        editTextEmailCode = binding.editTextEmailCode


        /*   hasFocus를 사용하여, 작성이 완료되면 상단에 알림을 출력하도록 구현   */
        editTextID.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkID()
            }
        }

        editTextNickname.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkNickname()
            }
        }

        editTextPW.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkPW()
                checkPWverify()
            }
        }

        /*   TextChangedListener를 사용하여 변경이 생길 경우 중복확인 여부 초기화   */
        editTextID.addTextChangedListener {
            idDuplicateFlag = false
        }

        /*   TextChangedListener를 사용하여 변경이 생길 때마다 갱신   */
        editTextPW.addTextChangedListener {
            if(editTextPWCheck.text.isNotEmpty()) {
                checkPW()
            }
        }

        editTextPWCheck.addTextChangedListener {
            checkPWverify()
        }


        /*   이메일 전송 버튼 선택 시 3분의 타이머를 실행하고 메일을 전송   */
        emailSendButton.setOnClickListener {
            if(checkEmail()) {
                startEmailTimer()
            }
        }

        /*   ID 중복 검사   */
        binding.RegesterIDCheck.setOnClickListener {
            if(checkID())
            {
                isIdExist(binding.editTextID.toString())
            }
        }

        /*   가입 진행   */
        binding.RegisterButton.setOnClickListener {
            if(
                !(checkID() || checkNickname() || checkPW() || checkPWverify() || checkEmail() || checkEmailverify() || idDuplicateFlag || emailDuplicateFlag)
            ){
                registerRequest(VectoService.RegisterRequest(
                    editTextID.text.toString(), editTextPW.text.toString(), "vecto",
                    editTextNickname.text.toString(), editTextEmail.text.toString(), editTextEmailCode.text.toString().toInt()
                ))
            }
            else{
                Toast.makeText(this, "작성을 완료해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun registerRequest(registerRequest: VectoService.RegisterRequest) {
        val vectoService = VectoService.create()

        val call = vectoService.registerUser(registerRequest)
        call.enqueue(object : Callback<VectoService.VectoResponse<String>>{
            override fun onResponse(call: Call<VectoService.VectoResponse<String>>, response: Response<VectoService.VectoResponse<String>>) {
                if(response.isSuccessful){
                    Log.d("REGISTER", "성공: ${response.body()}}")
                    Toast.makeText(this@RegisterActivity, "회원가입 성공 로그인을 진행해주세요.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else{
                    Log.d("REGISTER", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<String>>, t: Throwable) {
                Log.d("REGISTER", "실패")
            }
        })
    }

    private fun isIdExist(id: String){
        val vectoService = VectoService.create()

        val call = vectoService.idCheck(VectoService.IdCheckRequest(id))
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>>{
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("ID_CHECK", "성공: ${response.body()}}")

                    idDuplicateFlag = true
                }
                else{
                    Log.d("ID_CHECK", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    if(response.code() == 400) {
                        Toast.makeText(this@RegisterActivity, "중복된 아이디가 있습니다.", Toast.LENGTH_SHORT).show()

                        idDuplicateFlag = false
                        binding.IdNotificationText.setTextColor(ContextCompat.getColor(this@RegisterActivity, R.color.red))
                        binding.IdNotificationText.text = "중복된 아이디입니다."
                    }
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("ID_CHECK", "실패")
                idDuplicateFlag = false
            }
        })
    }

    private fun startEmailTimer(){

        sendMail(editTextEmail.text.toString())

        emailSendButton.isEnabled = false
        emailSendButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.vecto_disable))

        timer = object : CountDownTimer(180000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                emailSendButton.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                emailSendButton.text = "메일 발송"
                emailSendButton.isEnabled = true
                emailSendButton.backgroundTintList = null
            }
        }.start()
    }

    private fun sendMail(email: String) {
        val vectoService = VectoService.create()

        val call = vectoService.sendMail(VectoService.MailRequest(email))
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>>{
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("SEND_Email", "성공: ${response.body()}}")
                    Toast.makeText(this@RegisterActivity, "메일 발송에 성공했습니다.", Toast.LENGTH_SHORT).show()

                    emailDuplicateFlag = true
                }
                else{
                    Log.d("SEND_Email", "성공했으나 서버 오류 ${response.errorBody()?.string()}")

                    if(response.code() == 400) {
                        Toast.makeText(this@RegisterActivity, "중복된 이메일이 있습니다.", Toast.LENGTH_SHORT).show()

                        emailDuplicateFlag = false
                        binding.EmailNotificationText.setTextColor(ContextCompat.getColor(this@RegisterActivity, R.color.red))
                        binding.EmailNotificationText.text = "중복된 이메일입니다."
                    }
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("SEND_Email", "실패 ${t.message}")
                emailDuplicateFlag = false
            }
        })
    }

    private fun checkID(): Boolean{
        /*   아이디 형식 체크   */
        val input = editTextID.text.toString()

        return handleValidationResult(ValidationUtils.isValidId(input), Type.ID)
    }

    private fun checkNickname(): Boolean{
        /*   닉네임 형식 체크   */
        val input = editTextNickname.text.toString()

        return handleValidationResult(ValidationUtils.isValidNickname(input), Type.NICKNAME)
    }

    private fun checkPW(): Boolean{
        /*   비밀번호 형식 체크   */
        val input = editTextPW.text.toString()

        return handleValidationResult(ValidationUtils.isValidPw(input), Type.PASSWORD)
    }

    private fun checkPWverify(): Boolean{
        /*   비밀번호 확인 일치 체크   */
        if (editTextPW.text.toString() == editTextPWCheck.text.toString()) {
            binding.PwcheckNotificationText.setTextColor(ContextCompat.getColor(this, R.color.green))
            binding.PwcheckNotificationText.text = "비밀번호가 일치 합니다."
            return true
        }
        else {
            binding.PwcheckNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
            binding.PwcheckNotificationText.text = "비밀번호가 일치하지 않습니다."
            return false
        }
    }

    private fun checkEmail():Boolean{
        /*   이메일 형식 체크   */
        val input = editTextEmail.text.toString()

        return handleValidationResult(ValidationUtils.isValidEmail(input), Type.EMAIL)
    }

    private fun checkEmailverify(): Boolean{
        /*   이메일 인증 공란 확인   */
        val input = editTextEmailCode.text.toString()

        return handleValidationResult(ValidationUtils.isValidEmailVerify(input), Type.EMAILVERIFY)
    }


    private fun handleValidationResult(validationResult: ValidationUtils.ValidationResult, type: Type): Boolean{
        /*   ValidationResult 에 따른 결과 처리   */

        return when(validationResult){
            ValidationUtils.ValidationResult.VALID -> {
                //유효함
                when (type) {
                    Type.ID -> {//ID
                        if(idDuplicateFlag)//중복확인 완료
                        {
                            binding.IdNotificationText.setTextColor(ContextCompat.getColor(this, R.color.green))
                            binding.IdNotificationText.text = "사용 가능한 아이디 입니다."
                        }
                        else
                        {
                            binding.IdNotificationText.setTextColor(ContextCompat.getColor(this, R.color.vecto_warning))
                            binding.IdNotificationText.text = "중복확인을 진행해 주세요."
                        }
                    }
                    Type.NICKNAME -> {//Nickname
                        binding.NicknameNotificationText.setTextColor(ContextCompat.getColor(this, R.color.green))
                        binding.NicknameNotificationText.text = "사용 가능한 닉네임 입니다."
                    }
                    Type.PASSWORD -> {//PW
                        binding.PwNotificationText.setTextColor(ContextCompat.getColor(this, R.color.green))
                        binding.PwNotificationText.text = "사용 가능한 비밀번호 입니다."
                    }
                    Type.EMAIL -> {//Email
                        if(emailDuplicateFlag)
                        {
                            binding.EmailNotificationText.setTextColor(ContextCompat.getColor(this, R.color.green))
                            binding.EmailNotificationText.text = "메일을 발송하였습니다. 인증코드를 입력해주세요."
                        }
                        else
                        {
                            binding.EmailNotificationText.setTextColor(ContextCompat.getColor(this, R.color.vecto_warning))
                            binding.EmailNotificationText.text = "이메일 인증을 진행해주세요."
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
                        Toast.makeText(this, "아이디 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()

                        binding.IdNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.IdNotificationText.text = "아이디를 입력해주세요."
                    }
                    Type.NICKNAME -> {//Nickname
                        Toast.makeText(this, "닉네임 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()

                        binding.NicknameNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.NicknameNotificationText.text = "닉네임을 입력해주세요."
                    }
                    Type.PASSWORD -> {//PW
                        Toast.makeText(this, "비밀번호 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()

                        binding.PwNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.PwNotificationText.text = "비밀번호를 입력해주세요."
                    }
                    Type.EMAIL -> {//Email
                        Toast.makeText(this, "이메일 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()

                        binding.EmailNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.EmailNotificationText.text = "이메일을 입력해주세요."
                    }
                    Type.EMAILVERIFY -> {//Email code
                        Toast.makeText(this, "이메일 인증 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()

                        binding.EmailcodeNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.EmailcodeNotificationText.text = "인증 코드를 입력해주세요."
                    }
                }

                false
            }

            ValidationUtils.ValidationResult.INVALID_FORMAT -> {
                //형식에 맞지 않음
                when (type) {
                    Type.ID -> {//ID
                        Toast.makeText(this, "아이디 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()

                        binding.IdNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.IdNotificationText.text = "올바르지 않은 아이디입니다."
                    }
                    Type.NICKNAME -> {//Nickname
                        Toast.makeText(this, "닉네임 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()

                        binding.NicknameNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.NicknameNotificationText.text = "올바르지 않은 닉네임입니다."
                    }
                    Type.PASSWORD -> {//PW
                        Toast.makeText(this, "비밀번호 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()

                        binding.PwNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.PwNotificationText.text = "올바르지 않은 비밀번호입니다."
                    }
                    Type.EMAIL -> {//Email
                        Toast.makeText(this, "이메일 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()

                        binding.EmailNotificationText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        binding.EmailNotificationText.text = "올바르지 않은 이메일 입니다."
                    }
                    Type.EMAILVERIFY ->{//Email Code

                    }
                }

                false
            }

        }
    }


}