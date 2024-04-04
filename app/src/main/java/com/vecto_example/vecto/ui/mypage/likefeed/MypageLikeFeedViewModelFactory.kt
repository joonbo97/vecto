package com.vecto_example.vecto.ui.mypage.likefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import java.lang.IllegalArgumentException

class MypageLikeFeedViewModelFactory(private val repository: FeedRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MypageLikeFeedViewModel::class.java)) {
            return MypageLikeFeedViewModel(repository) as T
        }

        throw IllegalArgumentException("MypageLikeFeedViewModelFactory ERROR")
    }
}