package com.example.sd_smart_parking_app.data.auth

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.sd_smart_parking_app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class BiometricAuthStrategy(
    private val activity: FragmentActivity,
    private val authRepository: AuthRepository
) : AuthStrategy {

    private val auth = FirebaseAuth.getInstance()

    override fun login(onResult: (Boolean, String?) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    // 1. Obtener credenciales guardadas
                    val email = authRepository.getSavedEmail()
                    val password = authRepository.getSavedPass()

                    if (email != null && password != null) {
                        // 2. Iniciar sesión en Firebase
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onResult(true, null)
                                } else {
                                    onResult(false, task.exception?.localizedMessage ?: "Firebase authentication error")
                                }
                            }
                    } else {
                        onResult(false, "Please sign in manually first to enable biometric login")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SD Smart Parking Access")
            .setSubtitle("Use your fingerprint to sign in")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
