package com.vecto_example.vecto.ui.notice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.ActivityNoticeBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.notice.noticedetail.NoticeDetailFragment
import com.vecto_example.vecto.ui.notice.noticelist.NoticeListFragment

class NoticeActivity : AppCompatActivity() {
    lateinit var binding: ActivityNoticeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, NoticeListFragment())
                .commit()
        }

        initListener()

        supportFragmentManager.addOnBackStackChangedListener {
            Log.d("BackStack", "Current back stack entry count: ${supportFragmentManager.backStackEntryCount}")
        }
    }

    private fun initListener() {
        binding.BackButton.setOnClickListener {
            finish()
        }
    }

    fun showDetailFragment(id: Int) {
        val detailFragment = NoticeDetailFragment().apply {
            arguments = Bundle().apply {
                putInt("noticeId", id)
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, detailFragment)
            .addToBackStack(null)
            .commit()
    }

}