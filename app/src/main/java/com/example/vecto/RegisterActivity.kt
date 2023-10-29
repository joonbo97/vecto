package com.example.vecto

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.vecto.databinding.ActivityRegisterBinding
import com.example.vecto.retrofit.VectoService
import com.google.android.material.button.MaterialButton
import okhttp3.ResponseBody
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

    private lateinit var pwCheckImage: ImageView
    private lateinit var emailCheckImage: ImageView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        emailSendButton = binding.RegesterEmailCheck
        emailSendButton.setOnClickListener {
            if(checkEmail()) {
                startEmailTimer()
            }
        }

        editTextID = binding.editTextID
        editTextNickname = binding.editTextNickname
        editTextPW = binding.editTextPassword
        editTextPWCheck = binding.editTextCheckPassword
        editTextEmail = binding.editTextEmail
        editTextEmailCode = binding.editTextEmailCode

        pwCheckImage = binding.RegesterPWCheck
        emailCheckImage = binding.RegisterEmailCode



        editTextPWCheck.addTextChangedListener {
            pwCheckImage.visibility = View.VISIBLE
            checkPWImage()
        }

        editTextPW.addTextChangedListener {
            if(editTextPWCheck.text.isNotEmpty()) {
                checkPWImage()
            }
        }

        editTextID.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkID()
            }
        }

        editTextPW.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkPW()
            }
        }

        editTextNickname.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkNickname()
            }
        }


        binding.RegesterIDCheck.setOnClickListener {
            if(checkID())
            {
                isIdExist(binding.editTextID.toString())
            }
        }

        binding.RegisterButton.setOnClickListener {
            registerRequest(VectoService.RegisterRequest(
                editTextID.text.toString(), editTextPW.text.toString(), "vecto",
                editTextNickname.text.toString(), editTextEmail.text.toString(), editTextEmailCode.text.toString().toInt()
                ))
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
                    response.body()?.status
                    response.body()?.code
                    response.body()?.message
                    response.body()?.result
                }
                else{
                    Log.d("ID_CHECK", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("ID_CHECK", "실패")
            }
        })
    }

    private fun checkPWImage(){
        if (editTextPW.text.toString() == editTextPWCheck.text.toString())
            pwCheckImage.setImageResource(R.drawable.register_correct)
        else
            pwCheckImage.setImageResource(R.drawable.register_wrong)
    }

    private fun checkEmailImage(){

        //if(인증코드 일치)
        {
            emailCheckImage
        }

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
                }
                else{
                    Log.d("SEND_Email", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("SEND_Email", "실패 ${t.message}")
            }
        })
    }

    private fun checkID(): Boolean{
        if(editTextID.text.isEmpty()) {
            Toast.makeText(this, "아이디 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        val input = editTextID.text.toString()
        val idPattern = Regex("^[a-zA-Z0-9]{4,20}$")

        return if(!idPattern.matches(input)){
            Toast.makeText(this, "영어와 숫자로만 이루어진 4~20글자 아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            false
        } else
            true
    }

    private fun checkPW(): Boolean{
        val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$")
        val input = editTextPW.text.toString()

        if (input.isEmpty()) {
            Toast.makeText(this, "비밀번호 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!passwordPattern.matches(input)) {
            Toast.makeText(this, "영어, 숫자, 특수문자(@#$%^&+=!)를 포함한 8~20글자 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun checkNickname(): Boolean{
        val input = editTextNickname.text.toString()

        if (input.isEmpty()) {
            Toast.makeText(this, "닉네임 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (input.length > 10) {
            Toast.makeText(this, "닉네임은 10글자 이하여야 합니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun checkEmail():Boolean{
        val input = editTextEmail.text.toString()
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}")

        if(input.isEmpty()) {
            Toast.makeText(this, "이메일 항목이 비어있습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if(!emailRegex.matches(input)) {
            Toast.makeText(this, "이메일 형식에 맞지 않습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}