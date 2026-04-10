package com.example.sd_smart_parking_app.data.model

/**
 * Modelo para las preferencias de Remember Me
 */
data class RememberMePreferences(
    val isRememberMeEnabled: Boolean = false,
    val savedEmail: String? = null,
    val lastLoginTime: Long = 0,
    val shouldAutoLogin: Boolean = false
)