package com.vecto_example.vecto.ui.mypage.inquiry

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.FragmentMypageInquiryBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MypageInquiryFragment : Fragment() {
    lateinit var binding: FragmentMypageInquiryBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageInquiryBinding.inflate(inflater, container, false)

        binding.SendLogText.setOnClickListener {
            sendDatabaseByEmail(requireContext(), "log_database")
        }

        binding.contractTextBox.setOnClickListener {
            val intent = Intent(context, PolicyActivity::class.java)
            requireContext().startActivity(intent)
        }

        return binding.root
    }

    fun sendDatabaseByEmail(context: Context, databaseName: String) {
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
                putExtra(Intent.EXTRA_TEXT, "${Auth._userId.value.toString()}님의 로그데이터입니다.") // 이메일 본문
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Database File")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}