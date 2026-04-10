package com.example.sd_smart_parking_app.viewmodel

import android.app.Application
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.example.sd_smart_parking_app.data.auth.AuthStrategy
import com.example.sd_smart_parking_app.data.auth.BiometricAuthStrategy
import com.example.sd_smart_parking_app.data.auth.EmailAuthStrategy
import com.example.sd_smart_parking_app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.Firebase

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val authRepository = AuthRepository(application)
    private var currentStrategy: AuthStrategy? = null
    private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _savedEmail = MutableStateFlow<String?>(null)
    val savedEmail: StateFlow<String?> = _savedEmail

    init {
        _savedEmail.value = authRepository.getSavedEmail()
    }

    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        val strategy = EmailAuthStrategy(email, pass)
        executeLoginInternal(strategy, onSuccess)
    }

    fun loginWithBiometrics(activity: FragmentActivity, onSuccess: () -> Unit) {
        val strategy = BiometricAuthStrategy(activity, authRepository)
        executeLoginInternal(strategy, onSuccess)
    }

    private fun executeLoginInternal(strategy: AuthStrategy, onSuccess: () -> Unit) {
        this.currentStrategy = strategy
        _isLoading.value = true
        _errorMessage.value = null

        strategy.login { success, error ->
            _isLoading.value = false
            if (success) {
                // If it was an EmailAuthStrategy, we save credentials for future biometric use
                if (strategy is EmailAuthStrategy) {
                    authRepository.saveCredentials(strategy.email, strategy.password)
                    _savedEmail.value = strategy.email
                }
                onSuccess()
            } else {
                _errorMessage.value = error ?: "Error al iniciar sesión"
            }
        }
    }

    fun logLoginMethod(method: String) {
        // Registramos el evento con el parámetro del method utilizado
        firebaseAnalytics.logEvent("login_success_tracking") {
            param("login_type", method) // Valores: "email" o "biometric"
            param("timestamp", System.currentTimeMillis().toString())
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) return

        _isLoading.value = true
        _errorMessage.value = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            _isLoading.value = false
                            if (profileTask.isSuccessful) {
                                onSuccess()
                            } else {
                                _errorMessage.value = profileTask.exception?.localizedMessage ?: "Error al actualizar el perfil"
                            }
                        }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.localizedMessage ?: "Error al registrarse"
                }
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Guardar preferencias de Remember Me
     */
    fun setRememberMe(email: String, isEnabled: Boolean) {
        authRepository.saveRememberMePreferences(email, isEnabled)
        Log.d("AuthViewModel", "Remember Me set: $isEnabled for $email")
    }

    /**
     * Obtener preferencias de Remember Me
     */
    fun getRememberMePreferences() = authRepository.getRememberMePreferences()

    /**
     * Verificar si debe hacer auto-login
     */
    fun checkAndAutoLogin(): Boolean {
        val shouldAutoLogin = authRepository.shouldAutoLogin()

        if (shouldAutoLogin) {
            val preferences = authRepository.getRememberMePreferences()
            Log.d("AuthViewModel", "Auto-login enabled for: ${preferences.savedEmail}")

            return true
        }

        return false
    }

    /**
     * Limpiar Remember Me al hacer logout
     */
    fun clearRememberMeOnLogout() {
        authRepository.clearRememberMePreferences()
        Log.d("AuthViewModel", "Remember Me preferences cleared on logout")
    }

    /**
     * Sign out del usuario
     */
    fun signOut() {
        auth.signOut()
        clearRememberMeOnLogout()
        authRepository.clearCredentials()
        Log.d("AuthViewModel", "User signed out and Remember Me cleared")
    }
}