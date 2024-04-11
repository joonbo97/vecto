package com.vecto_example.vecto.ui.todaycourse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TodayCourseViewModel : ViewModel() {

    private val _isPermissionGained = MutableLiveData<Boolean>()
    val isPermissionGained: LiveData<Boolean> = _isPermissionGained

    fun updatePermissionState(isGained: Boolean) {
        _isPermissionGained.value = isGained
    }
}