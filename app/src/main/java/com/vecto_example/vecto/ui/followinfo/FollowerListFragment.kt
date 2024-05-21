package com.vecto_example.vecto.ui.followinfo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentFollowerListBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailViewModel
import com.vecto_example.vecto.ui.followinfo.adapter.FollowListAdapter
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.vecto_example.vecto.utils.ToastMessageUtils.errorMessageHandler
import kotlinx.coroutines.launch

class FollowerListFragment : Fragment(), FollowListAdapter.OnFollowActionListener {
    lateinit var binding: FragmentFollowerListBinding

    private val viewModel: FollowInfoViewModel by viewModels {
        FollowInfoViewModelFactory(UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
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
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.post_follow_success, followListAdapter.followList[followListAdapter.actionPosition].nickName))
                } else {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.post_follow_already, followListAdapter.followList[followListAdapter.actionPosition].nickName))
                }

                followListAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                if(it) {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.delete_follow_success, followListAdapter.followList[followListAdapter.actionPosition].nickName))
                    followListAdapter.deleteFollowSuccess()
                } else {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.delete_follow_already, followListAdapter.followList[followListAdapter.actionPosition].nickName))
                }
            }

            followListAdapter.actionPosition = -1
        }

        lifecycleScope.launch {
            viewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(requireContext(), it.userToken.accessToken, it.userToken.refreshToken)

                when(it.function){
                    FollowInfoViewModel.Function.GetFollowerList.name -> {
                        viewModel.getFollowerList(viewModel.userId)
                    }
                    FollowInfoViewModel.Function.PostFollow.name -> {
                        viewModel.postFollow(viewModel.postFollowId)
                    }
                    FollowInfoViewModel.Function.DeleteFollow.name -> {
                        viewModel.deleteFollow(viewModel.deleteFollowId)
                    }
                }
            }
        }



        viewModel.errorMessage.observe(viewLifecycleOwner) {
            ToastMessageUtils.showToast(requireContext(), getString(it))

            if(it == R.string.expired_login){
                SaveLoginDataUtils.deleteData(requireContext())
            }
        }

        viewModel.postFollowError.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                errorMessageHandler(requireContext(), ToastMessageUtils.UserInterActionType.FOLLOW_POST.name, it)
                followListAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(viewLifecycleOwner) {
            if(followListAdapter.actionPosition != -1) {
                errorMessageHandler(requireContext(), ToastMessageUtils.UserInterActionType.FOLLOW_DELETE.name, it)
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
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
            followListAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.deleteFollow(userId)
        } else {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
            followListAdapter.actionPosition = -1
        }
    }
}