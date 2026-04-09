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
                                    onResult(false, task.exception?.localizedMessage ?: "Error en la autenticación con Firebase")
                                }
                            }
                    } else {
                        onResult(false, "Inicia sesión manual para activar biometría")
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
            .setTitle("Acceso SD Smart Parking")
            .setSubtitle("Usa tu huella para entrar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
