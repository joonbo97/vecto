package com.vecto_example.vecto.ui.notice.noticedetail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.vecto_example.vecto.data.repository.NoticeRepository
import com.vecto_example.vecto.databinding.FragmentNoticeDetailBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.DateTimeUtils

class NoticeDetailFragment : Fragment() {
    private lateinit var binding: FragmentNoticeDetailBinding
    private val noticeDetailViewModel: NoticeDetailViewModel by viewModels {
        NoticeDetailViewModelFactory(NoticeRepository(VectoService.create()))
    }

    private var noticeId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            noticeId = it.getInt("noticeId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNoticeDetailBinding.inflate(inflater, container, false)

        if(noticeId != -1) {
            noticeDetailViewModel.getNotice(noticeId)
        }

        initObserver()

        return binding.root
    }

    private fun initObserver() {
        noticeDetailViewModel.isLoading.observe(viewLifecycleOwner){
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.INVISIBLE
        }

        noticeDetailViewModel.noticeResponse.observe(viewLifecycleOwner){
            binding.noticeTitle.text = it.title
            binding.noticeTimeText.text = DateTimeUtils.getNoticeTime(it.createdAt)
            binding.noticeContent.text = it.content
        }
    }

}