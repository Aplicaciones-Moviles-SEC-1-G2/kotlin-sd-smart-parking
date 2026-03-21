# SD Smart Parking — Android App

An Android application for real-time smart parking management. The system supports two roles: **Gerente** (parking manager) and **Usuario** (driver), each with a dedicated experience.

---

## Features

### Usuario (Driver)
- View real-time parking spot availability by floor
- Navigate to the parking building via Google Maps
- Manage registered vehicles (plates)
- Edit personal profile

### Gerente (Manager)
- Dashboard with occupancy summary
- Create vehicle entry/exit records with OCR confidence scoring
- Manage and visualize parking spots by floor
- Generate and view usage reports
- Configure parking settings

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9+ |
| UI | Jetpack Compose |
| Auth | Firebase Authentication + Google Sign-In |
| Database | Firebase Firestore |
| Storage | Firebase Storage |
| Maps | Google Maps SDK |
| Location | Google Play Services Location |
| Min Android | API 26 (Android 8.0) |

---

## Project Structure

sd-smart-parking/ ├── app/ │ ├── src/ │ │ ├── main/ │ │ │ ├── java/com/example/sd_smart_parking_app/ │ │ │ │ ├── MainActivity.kt # Entry point │ │ │ │ ├── data/ │ │ │ │ │ ├── LocationManager.kt # Location services │ │ │ │ │ ├── MapsHelper.kt # Google Maps integration │ │ │ │ │ ├── NotificationManager.kt # Push notifications │ │ │ │ │ ├── NotificationService.kt # Notification service │ │ │ │ │ ├── model/ # Data models (User, Car, ParkingSpot, etc.) │ │ │ │ │ └── repository/ # Repository pattern for data access │ │ │ │ ├── ui/ │ │ │ │ │ ├── screens/ # Screen composables │ │ │ │ │ │ ├── Login/ # LoginScreen, RegistrationScreen │ │ │ │ │ │ ├── Parking/ # ParkingSpotsScreen, SpotDetails │ │ │ │ │ │ ├── Gerente/ # Dashboard, Records, Reports │ │ │ │ │ │ └── Usuario/ # Profile, Vehicles, Navigation │ │ │ │ │ ├── components/ # Reusable Compose components │ │ │ │ │ └── theme/ # Material Design theme │ │ │ │ └── viewmodel/ │ │ │ │ ├── NavigationViewModel.kt # Navigation & routing │ │ │ │ └── NotificationViewModel.kt # Notification handling │ │ │ ├── AndroidManifest.xml │ │ │ └── res/ # Resources (layouts, strings, etc.) │ │ ├── androidTest/ # Instrumented tests │ │ └── test/ # Unit tests │ ├── build.gradle.kts # App-level build config │ └── proguard-rules.pro # Obfuscation rules ├── build.gradle.kts # Project-level build config ├── settings.gradle.kts ├── gradle.properties └── google-services.json # Firebase configuration
