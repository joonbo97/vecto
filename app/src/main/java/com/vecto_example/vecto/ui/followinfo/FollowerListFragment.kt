package com.vecto_example.vecto.ui.followinfo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentFollowerListBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.followinfo.adapter.FollowListAdapter

class FollowerListFragment : Fragment(), FollowListAdapter.OnFollowActionListener {
    lateinit var binding: FragmentFollowerListBinding

    private val viewModel: FollowInfoViewModel by viewModels {
        FollowInfoViewModelFactory(UserRepository(VectoService.create()))
    }

    private lateinit var followListAdapter: FollowListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFollowerListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = (activity as? FollowInfoActivity)?.getUserIdValue()
        if(!userId.isNullOrEmpty())
            viewModel.getFollowerList(userId)

        initRecyclerView()
        initObservers()


    }

    private fun initObservers() {
        viewModel.followListResponse.observe(viewLifecycleOwner) {
            followListAdapter.addFollowListData(it.followRelations)

            if(followListAdapter.followList.size == 0)
                setNoneImage()
        }

        viewModel.postFollowResult.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                if(it) {
                    followListAdapter.postFollowSuccess()
                    Toast.makeText(requireContext(), "${followListAdapter.followList[followListAdapter.actionPosition].nickName} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "이미 ${followListAdapter.followList[followListAdapter.actionPosition].nickName} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                }

                followListAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                if(it) {
                    Toast.makeText(requireContext(), "${followListAdapter.followList[followListAdapter.actionPosition].nickName} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                    followListAdapter.deleteFollowSuccess()
                } else {
                    Toast.makeText(requireContext(), "이미 ${followListAdapter.followList[followListAdapter.actionPosition].nickName} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            followListAdapter.actionPosition = -1
        }

        viewModel.postFollowError.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(requireContext(), "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                followListAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(requireContext(), "팔로우 취소 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                followListAdapter.actionPosition = -1
            }
        }
    }

    private fun initRecyclerView() {
        followListAdapter = FollowListAdapter()
        followListAdapter.followActionListener = this

        binding.FollowerListRecyclerView.adapter = followListAdapter
        binding.FollowerListRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.FollowerListRecyclerView.itemAnimator = null
    }

    private fun setNoneImage() {
        binding.FollowerNoneImage.visibility = View.VISIBLE
        binding.FollowerNoneText.visibility = View.VISIBLE
    }

    override fun onPostFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.postFollow(userId)
        } else {
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            followListAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.deleteFollow(userId)
        } else {
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            followListAdapter.actionPosition = -1
        }
    }
}