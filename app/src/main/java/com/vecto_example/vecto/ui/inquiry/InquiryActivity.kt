package com.vecto_example.vecto.ui.inquiry

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.ActivityInquiryBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class InquiryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInquiryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInquiryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
    }

    private fun initListener() {
        /*   리스너 초기화 함수   */

        //뒤로 가기 버튼
        binding.BackButton.setOnClickListener {
            finish()
        }

        binding.QueryBox1.setOnClickListener {
            if(binding.Qcontent.visibility == View.VISIBLE){
                binding.Qcontent.visibility = View.GONE
                binding.SendLogText.visibility = View.GONE
            } else {
                binding.Qcontent.visibility = View.VISIBLE
                binding.SendLogText.visibility = View.VISIBLE
            }
        }

        binding.QueryBox2.setOnClickListener {
            if(binding.Qcontent2.visibility == View.VISIBLE){
                binding.Qcontent2.visibility = View.GONE
            } else {
                binding.Qcontent2.visibility = View.VISIBLE
            }
        }

        binding.QueryBox3.setOnClickListener {
            if(binding.Qcontent31.visibility == View.VISIBLE){
                setContent3(false)
            } else {
                setContent3(true)
            }
        }

        binding.SendLogText.setOnClickListener {
            sendDatabaseByEmail(this, "log_database")
        }

        binding.contractTextBox.setOnClickListener {
            val intent = Intent(this, PolicyActivity::class.java)
            this.startActivity(intent)
        }
    }

    private fun setContent3(flag: Boolean) {
        val visibility = if(flag)
            View.VISIBLE
        else
            View.GONE

        binding.Qcontent31.visibility = visibility
        binding.contentImage31.visibility = visibility
        binding.contentImage32.visibility = visibility
        binding.Qcontent32.visibility = visibility
    }

    private fun sendDatabaseByEmail(context: Context, databaseName: String) {
        try {
            val internalDbFile = context.getDatabasePath(databaseName)
            val externalDbFile = File(context.getExternalFilesDir(null), databaseName)

            FileInputStream(internalDbFile).use { inputStream ->
                FileOutputStream(externalDbFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                externalDbFile
            )

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("joonbo97@gmail.com")) // 받는 사람의 이메일 주소
                putExtra(Intent.EXTRA_SUBJECT, "VECTO_로그데이터") // 이메일 제목
                putExtra(Intent.EXTRA_TEXT, "${Auth.userId.value.toString()}님의 로그데이터입니다.") // 이메일 본문
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Database File")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "로그 데이터가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}