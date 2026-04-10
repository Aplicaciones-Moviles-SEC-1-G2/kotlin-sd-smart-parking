package com.example.sd_smart_parking_app.data.repository


import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.auth.FirebaseAuth


class ParkingRepository private constructor() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: ParkingRepository? = null

        fun getInstance(): ParkingRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ParkingRepository()
                INSTANCE = instance
                instance
            }
        }
    }
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
    fun saveUserProfile(uid: String, profile: UserProfile, onComplete: (Boolean) -> Unit) {
        db.collection("users").document(uid)
            .set(profile)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }
    fun getCurrentUserUid(): String? = auth.currentUser?.uid
    fun getUserProfile(onSuccess: (UserProfile) -> Unit) {
        val uid = getCurrentUserUid() ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    onSuccess(profile)
                }
            }
    }
    fun logout() {
        auth.signOut()
    }
}
