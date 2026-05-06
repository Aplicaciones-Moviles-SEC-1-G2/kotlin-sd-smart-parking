package com.example.sd_smart_parking_app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.model.UserProfile
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = ParkingRepository.getInstance()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _photoUploadState = MutableStateFlow<PhotoUploadState>(PhotoUploadState.Idle)
    val photoUploadState: StateFlow<PhotoUploadState> = _photoUploadState

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        _isLoading.value = true
        repository.getUserProfile { profile ->
            _userProfile.value = profile
            _isLoading.value = false
        }
    }

    fun uploadProfilePhoto(photoUri: Uri) {
        val uid = repository.getCurrentUserUid() ?: return
        _photoUploadState.value = PhotoUploadState.Loading

        viewModelScope.launch {
            repository.uploadProfilePhoto(
                uid = uid,
                photoUri = photoUri,
                onSuccess = { downloadUrl ->
                    repository.updateUserPhotoURL(uid, downloadUrl) { success ->
                        if (success) {
                            _userProfile.value = _userProfile.value.copy(photoURL = downloadUrl)
                            _photoUploadState.value = PhotoUploadState.Success
                        } else {
                            _photoUploadState.value = PhotoUploadState.Error("Failed to update profile")
                        }
                    }
                },
                onError = { e ->
                    _photoUploadState.value = PhotoUploadState.Error(e.message ?: "Upload failed")
                }
            )
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        repository.logout()
        _userProfile.value = UserProfile()
        onLogoutSuccess()
    }
}

sealed class PhotoUploadState {
    object Idle : PhotoUploadState()
    object Loading : PhotoUploadState()
    object Success : PhotoUploadState()
    data class Error(val message: String) : PhotoUploadState()
}