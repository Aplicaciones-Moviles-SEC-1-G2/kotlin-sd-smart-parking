package com.example.sd_smart_parking_app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import com.example.sd_smart_parking_app.data.model.RememberMePreferences
import android.util.Log

class AuthRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(email: String, pass: String) {
        sharedPreferences.edit {
            putString("saved_email", email)
            putString("saved_pass", pass)
            putLong("last_firebase_validation", System.currentTimeMillis())
        }
    }

    fun getSavedEmail(): String? {
        return sharedPreferences.getString("saved_email", null)
    }

    fun getSavedPass(): String? {
        return sharedPreferences.getString("saved_pass", null)
    }

    fun validateOfflineCredentials(email: String, pass: String): Boolean {
        val savedEmail = getSavedEmail()
        val savedPass = getSavedPass()
        val lastValidation = sharedPreferences.getLong("last_firebase_validation", 0)
        
        // Política de expiración: 7 días para forzar validación con Firebase si está offline
        val isExpired = (System.currentTimeMillis() - lastValidation) > (7 * 24 * 60 * 60 * 1000)
        
        if (isExpired) {
            Log.d("AuthRepository", "Offline credentials expired")
            return false
        }

        return email == savedEmail && pass == savedPass
    }

    fun clearCredentials() {
        sharedPreferences.edit {
            remove("saved_email")
            remove("saved_pass")
            remove("last_firebase_validation")
        }
    }

    fun saveRememberMePreferences(email: String, isEnabled: Boolean) {
        sharedPreferences.edit {
            putBoolean("remember_me_enabled", isEnabled)
            putString("remember_me_email", if (isEnabled) email else "")
            putLong("last_login_time", System.currentTimeMillis())
        }
        Log.d("AuthRepository", "Remember Me preferences saved: $isEnabled for $email")
    }

    fun getRememberMePreferences(): RememberMePreferences {
        val isEnabled = sharedPreferences.getBoolean("remember_me_enabled", false)
        val email = sharedPreferences.getString("remember_me_email", null)
        val lastLoginTime = sharedPreferences.getLong("last_login_time", 0)

        return RememberMePreferences(
            isRememberMeEnabled = isEnabled,
            savedEmail = email.takeIf { it?.isNotEmpty() == true },
            lastLoginTime = lastLoginTime,
            shouldAutoLogin = isEnabled && !email.isNullOrEmpty()
        )
    }

    fun clearRememberMePreferences() {
        sharedPreferences.edit {
            putBoolean("remember_me_enabled", false)
            putString("remember_me_email", "")
            putLong("last_login_time", 0)
        }
        Log.d("AuthRepository", "Remember Me preferences cleared")
    }

    fun shouldAutoLogin(): Boolean {
        val preferences = getRememberMePreferences()
        // 30-day auto-login session — use Long literal to avoid Int overflow
        val isSessionValid = (System.currentTimeMillis() - preferences.lastLoginTime) < (30L * 24 * 60 * 60 * 1000)
        return preferences.shouldAutoLogin && isSessionValid
    }
}
