package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import com.example.sd_smart_parking_app.data.model.UserProfile
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {
    private val repository = ParkingRepository()
    private val auth = FirebaseAuth.getInstance()

    // Información del perfil
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchUserProfile()
    }

    // Lógica de buscar los datos
    fun fetchUserProfile() {
        _isLoading.value = true
        // Obtiene el perfil
        repository.getUserProfile { profile ->
            _userProfile.value = profile
            _isLoading.value = false
        }
    }

    // Cerrar sesión
    fun logout(onLogoutSuccess: () -> Unit) {
        repository.logout()
        // Limpiamos la información que teníamos en memoria
        _userProfile.value = UserProfile()
        // Avisamos a la vista que ya puede navegar afuera
        onLogoutSuccess()
    }
}