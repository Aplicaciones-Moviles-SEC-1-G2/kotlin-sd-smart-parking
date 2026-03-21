package com.example.sd_smart_parking_app.data.repository

import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.model.UserProfile
import com.example.sd_smart_parking_app.data.model.UserCar
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

class ParkingRepository {
    private val db = Firebase.firestore

    // Obtener configuración del parqueadero con actualizaciones en vivo
    fun getParkingConfig(onSuccess: (ParkingConfig) -> Unit) {
        db.collection("config").document("parking")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val config = snapshot?.toObject(ParkingConfig::class.java)
                if (config != null) {
                    onSuccess(config)
                }
            }
    }

    // Obtener estado de los cupos con actualizaciones en vivo
    fun getParkingSpots(onSuccess: (List<ParkingSpot>) -> Unit) {
        db.collection("parkingSpots")
            .addSnapshotListener { snapshot: QuerySnapshot?, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                val spots = snapshot?.documents?.mapNotNull { doc: DocumentSnapshot ->
                    val spot = doc.toObject(ParkingSpot::class.java)
                    spot?.apply { id = doc.id }
                } ?: emptyList()
                
                onSuccess(spots)
            }
    }

    // Guardar perfil de usuario
    fun saveUserProfile(uid: String, profile: UserProfile, onComplete: (Boolean) -> Unit) {
        db.collection("users").document(uid)
            .set(profile)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    // Obtener perfil del usuario actual
    fun getUserProfile(uid: String, onSuccess: (UserProfile) -> Unit) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    onSuccess(profile)
                }
            }
    }
}
