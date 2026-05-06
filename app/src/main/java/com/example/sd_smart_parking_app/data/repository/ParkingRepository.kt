package com.example.sd_smart_parking_app.data.repository

import android.content.Context
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

class ParkingRepository private constructor(private val appContext: Context? = null) {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val prefs = appContext?.getSharedPreferences("parking_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: ParkingRepository? = null

        fun getInstance(context: Context? = null): ParkingRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ParkingRepository(context?.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getParkingConfig(onSuccess: (ParkingConfig) -> Unit) {
        db.collection("config").document("parking")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val config = snapshot?.toObject(ParkingConfig::class.java)
                if (config != null) onSuccess(config)
            }
    }

    fun getParkingSpots(onSuccess: (List<ParkingSpot>) -> Unit) {
        db.collection("parkingSpots")
            .addSnapshotListener { snapshot: QuerySnapshot?, error ->
                if (error != null) return@addSnapshotListener
                val spots = snapshot?.documents?.mapNotNull { doc: DocumentSnapshot ->
                    doc.toObject(ParkingSpot::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                onSuccess(spots)
            }
    }

    fun getParkingSpotsWithSource(onResult: (spots: List<ParkingSpot>, isFromCache: Boolean) -> Unit) {
        db.collection("parkingSpots")
            .addSnapshotListener { snapshot: QuerySnapshot?, error ->
                if (error != null) return@addSnapshotListener
                val spots = snapshot?.documents?.mapNotNull { doc: DocumentSnapshot ->
                    doc.toObject(ParkingSpot::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                val isFromCache = snapshot?.metadata?.isFromCache ?: false
                if (!isFromCache) {
                    prefs?.edit()?.putLong("last_server_update", System.currentTimeMillis())?.apply()
                }
                onResult(spots, isFromCache)
            }
    }

    fun getLastServerUpdate(): Long = prefs?.getLong("last_server_update", 0L) ?: 0L

    fun forceRefreshParkingSpots(
        onResult: (spots: List<ParkingSpot>, timestamp: Long) -> Unit,
        onError: () -> Unit
    ) {
        db.collection("parkingSpots").get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                val spots = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ParkingSpot::class.java)?.apply { id = doc.id }
                }
                val timestamp = System.currentTimeMillis()
                prefs?.edit()?.putLong("last_server_update", timestamp)?.apply()
                onResult(spots, timestamp)
            }
            .addOnFailureListener { onError() }
    }

    fun saveUserProfile(uid: String, profile: UserProfile, onComplete: (Boolean) -> Unit) {
        db.collection("users").document(uid)
            .set(profile)
            .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun getUserProfile(onSuccess: (UserProfile) -> Unit) {
        val uid = getCurrentUserUid() ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) onSuccess(profile)
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun uploadProfilePhoto(
        uid: String,
        photoUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val storage = FirebaseStorage.getInstance()
        val photoRef = storage.reference.child("profile_photos/$uid.jpg")

        photoRef.putFile(photoUri)
            .addOnSuccessListener {
                photoRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun updateUserPhotoURL(uid: String, photoURL: String, onComplete: (Boolean) -> Unit) {
        db.collection("users").document(uid)
            .update("photoURL", photoURL)
            .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }
}
