package com.example.sd_smart_parking_app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

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
        }
    }

    fun getSavedEmail(): String? {
        return sharedPreferences.getString("saved_email", null)
    }

    fun getSavedPass(): String? {
        return sharedPreferences.getString("saved_pass", null)
    }

    fun clearCredentials() {
        sharedPreferences.edit {
            clear()
        }
    }
}
