package com.example.sd_smart_parking_app.data.auth
import com.google.firebase.auth.FirebaseAuth

class EmailAuthStrategy(
    val email: String,
    val password: String
) : AuthStrategy {

    private val auth = FirebaseAuth.getInstance()

    override fun login(onResult: (Boolean, String?) -> Unit) {
        // 1. Validar que no estén vacíos
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Email and password are mandatory")
            return
        }

        // 2. Llamar a Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null) // ¡Éxito!
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Wrong User or Password")
                }
            }
    }
}