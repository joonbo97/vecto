package com.vecto_example.vecto.ui.mypage.myfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import java.lang.IllegalArgumentException

class MypageFeedViewModelFactory (private val repository: FeedRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MypageFeedViewModel::class.java)) {
            return MypageFeedViewModel(repository) as T
        }

        throw IllegalArgumentException("MypageFeedViewModelFactory ERROR")
    }
}