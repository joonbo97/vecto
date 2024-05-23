package com.vecto_example.vecto.ui.notice.noticelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.data.repository.NoticeRepository
import com.vecto_example.vecto.databinding.FragmentNoticeListBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.notice.NoticeActivity
import com.vecto_example.vecto.ui.notice.noticelist.adapter.NoticeAdapter
import com.vecto_example.vecto.utils.ToastMessageUtils


class NoticeListFragment : Fragment(), NoticeAdapter.OnItemClickListener {
    private lateinit var binding: FragmentNoticeListBinding
    private lateinit var noticeAdapter: NoticeAdapter

    private val noticeListViewModel: NoticeListViewModel by viewModels {
        NoticeListViewModelFactory(NoticeRepository(VectoService.create()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNoticeListBinding.inflate(inflater, container, false)

        if(noticeListViewModel.noticeListResponse.value.isNullOrEmpty())
            noticeListViewModel.getNoticeList()

        initRecyclerView()
        initObserver()

        return binding.root
    }

    private fun initObserver() {
        noticeListViewModel.isLoading.observe(viewLifecycleOwner){
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.INVISIBLE
        }

        noticeListViewModel.noticeListResponse.observe(viewLifecycleOwner){
            if(noticeListViewModel.noticeListResponse.value?.size != noticeAdapter.noticeList.size)
                noticeAdapter.setData(it)
        }

        noticeListViewModel.errorMessage.observe(viewLifecycleOwner){
            ToastMessageUtils.showToast(requireContext(), getString(it))
        }
    }

    private fun initRecyclerView() {
        noticeAdapter = NoticeAdapter()
        noticeAdapter.itemClickListener = this

        val noticeRecyclerView = binding.noticeRecyclerView
        noticeRecyclerView.adapter = noticeAdapter
        noticeRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        noticeRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), 1))
    }

    override fun onNoticeItemClick(noticeId: Int) {
        (activity as? NoticeActivity)?.showDetailFragment(noticeId)
    }

}