package com.example.sd_smart_parking_app.viewmodel

import android.app.Application
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.example.sd_smart_parking_app.data.auth.AuthStrategy
import com.example.sd_smart_parking_app.data.auth.BiometricAuthStrategy
import com.example.sd_smart_parking_app.data.auth.EmailAuthStrategy
import com.example.sd_smart_parking_app.data.repository.AuthRepository
import com.example.sd_smart_parking_app.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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
    private val networkMonitor = NetworkMonitor(application)
    private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _savedEmail = MutableStateFlow<String?>(null)
    val savedEmail: StateFlow<String?> = _savedEmail

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    init {
        _savedEmail.value = authRepository.getSavedEmail()
        _isOffline.value = !networkMonitor.isOnline()
    }

    fun updateConnectionStatus() {
        _isOffline.value = !networkMonitor.isOnline()
    }

    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null
        updateConnectionStatus()

        if (_isOffline.value) {
            handleOfflineLogin(email, pass, onSuccess)
        } else {
            val strategy = EmailAuthStrategy(email, pass)
            executeLoginInternal(strategy, onSuccess)
        }
    }

    fun loginWithBiometrics(activity: FragmentActivity, onSuccess: () -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null
        updateConnectionStatus()

        if (_isOffline.value) {
            val email = authRepository.getSavedEmail()
            val pass = authRepository.getSavedPass()
            
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                activity, androidx.core.content.ContextCompat.getMainExecutor(activity),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        if (email != null && pass != null) {
                            handleOfflineLogin(email, pass, onSuccess)
                        } else {
                            _isLoading.value = false
                            _errorMessage.value = "Please sign in manually first to enable biometric login"
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        _isLoading.value = false
                        _errorMessage.value = errString.toString()
                    }
                })

            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("SD Smart Parking Access")
                .setSubtitle("Use your fingerprint to sign in (Offline Mode)")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            val strategy = BiometricAuthStrategy(activity, authRepository)
            executeLoginInternal(strategy, onSuccess)
        }
    }

    private fun handleOfflineLogin(email: String, pass: String, onSuccess: () -> Unit) {
        if (authRepository.validateOfflineCredentials(email, pass)) {
            _isLoading.value = false
            onSuccess()
        } else {
            _isLoading.value = false
            _errorMessage.value = "Incorrect or expired offline credentials. Please connect to the internet to sign in."
        }
    }

    private fun executeLoginInternal(strategy: AuthStrategy, onSuccess: () -> Unit) {
        strategy.login { success, error ->
            _isLoading.value = false
            if (success) {
                if (strategy is EmailAuthStrategy) {
                    authRepository.saveCredentials(strategy.email, strategy.password)
                    _savedEmail.value = strategy.email
                } else if (strategy is BiometricAuthStrategy) {
                    // Actualizar timestamp de validación exitosa si fue biométrico exitoso en Firebase
                    val email = authRepository.getSavedEmail()
                    val pass = authRepository.getSavedPass()
                    if (email != null && pass != null) {
                        authRepository.saveCredentials(email, pass)
                    }
                }
                onSuccess()
            } else {
                _errorMessage.value = error ?: "Failed to sign in"
            }
        }
    }

    fun logLoginMethod(method: String) {
        firebaseAnalytics.logEvent("login_success_tracking") {
            param("login_type", method)
            param("timestamp", System.currentTimeMillis().toString())
            param("connection_state", if (_isOffline.value) "offline" else "online")
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        if (!networkMonitor.isOnline()) {
            _errorMessage.value = "An internet connection is required to register."
            return
        }
        
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
                                _errorMessage.value = profileTask.exception?.localizedMessage ?: "Error updating profile"
                            }
                        }
                } else {
                    _isLoading.value = false
                    val exception = task.exception
                    _errorMessage.value = when (exception) {
                        is FirebaseAuthUserCollisionException -> "This email is already registered."
                        is FirebaseAuthWeakPasswordException -> "The password is too weak."
                        is FirebaseAuthInvalidCredentialsException -> "The email address is badly formatted."
                        else -> "Registration failed. Please try again."
                    }
                }
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setRememberMe(email: String, isEnabled: Boolean) {
        authRepository.saveRememberMePreferences(email, isEnabled)
    }

    fun getRememberMePreferences() = authRepository.getRememberMePreferences()

    fun checkAndAutoLogin(): Boolean {
        updateConnectionStatus()
        return authRepository.shouldAutoLogin()
    }

    fun refreshAutoLoginSession() {
        val email = authRepository.getSavedEmail() ?: return
        authRepository.saveRememberMePreferences(email, true)
    }

    fun signOut() {
        auth.signOut()
        authRepository.clearRememberMePreferences()
        authRepository.clearCredentials()
        _savedEmail.value = null
        Log.d("AuthViewModel", "User signed out and data cleared")
    }
}
