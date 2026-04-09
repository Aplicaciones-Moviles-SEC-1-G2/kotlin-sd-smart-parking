package com.example.sd_smart_parking_app.data.auth

interface AuthStrategy {
    // Todas las estrategias devuelven un resultado (éxito o error)
    fun login(onResult: (Boolean, String?) -> Unit)
}