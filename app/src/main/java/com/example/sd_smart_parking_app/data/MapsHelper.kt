package com.example.sd_smart_parking_app.data

import android.content.Context
import android.content.Intent
import android.net.Uri

object MapsHelper {

    // Coordenadas del parqueadero SD Building (Universidad de los Andes)
    private const val PARKING_LATITUDE = 4.604427117795944
    private const val PARKING_LONGITUDE = -74.06590234232677
    private const val PARKING_NAME = "SD+Building+Parking"

    /**
     * Abre Google Maps con la ruta desde la ubicación actual del usuario
     * hasta el parqueadero SD Building
     */
    fun openMapsNavigation(context: Context, userLatitude: Double, userLongitude: Double) {
        try {
            // URL de Google Maps para navegación
            // Format: https://www.google.com/maps/dir/?api=1&origin=start_lat,start_lng&destination=end_lat,end_lng&travelmode=driving
            val uri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&" +
                        "origin=$userLatitude,$userLongitude&" +
                        "destination=$PARKING_LATITUDE,$PARKING_LONGITUDE&" +
                        "travelmode=driving"
            )

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            // Si Google Maps no está instalado, usar navegador
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: abrir en navegador
                val webUri = Uri.parse(
                    "https://maps.google.com/maps?saddr=$userLatitude,$userLongitude&" +
                            "daddr=$PARKING_LATITUDE,$PARKING_LONGITUDE&travelmode=driving"
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Abre Google Maps solo con el destino (parqueadero)
     */
    fun openMapsDestination(context: Context) {
        try {
            val uri = Uri.parse(
                "geo:$PARKING_LATITUDE,$PARKING_LONGITUDE?q=$PARKING_NAME"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = Uri.parse(
                    "https://www.google.com/maps/search/$PARKING_LATITUDE,$PARKING_LONGITUDE"
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}