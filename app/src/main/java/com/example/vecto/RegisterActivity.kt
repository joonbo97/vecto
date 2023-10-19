package com.example.vecto

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.example.vecto.databinding.ActivityRegisterBinding
import com.google.android.material.button.MaterialButton

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
            startEmailTimer()
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

        //TODO 메일 전송 요청

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

}