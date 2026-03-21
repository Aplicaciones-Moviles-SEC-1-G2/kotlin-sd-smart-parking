# SD Smart Parking — Android App

An Android application for real-time smart parking management. The system supports two roles: **Gerente** (parking manager) and **Usuario** (driver), each with a dedicated experience.

---

## Features

### Usuario (Driver)
- View real-time parking spot availability and summary
- Navigate to the parking building via Google Maps integration
- Manage registered vehicles (plates) and personal profile
- View parking history and records
- Receive real-time notifications via Firebase Cloud Messaging

### Gerente (Manager)
- Dashboard with occupancy summary and real-time updates
- Manage and visualize parking spots and configuration
- Generate and view usage reports/history
- Configure parking settings (floors, spots, rates)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Navigation | Compose Navigation |
| Auth | Firebase Authentication |
| Database | Firebase Firestore |
| Messaging | Firebase Cloud Messaging (FCM) |
| Maps | Google Maps SDK for Android |
| Location | Google Play Services Location |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 15) |

---

## Project Structure

```
sd-smart-parking/
├── app/src/main/java/com/example/sd_smart_parking_app/
│   ├── data/
│   │   ├── model/             # Data classes (ParkingConfig, UserProfile, etc.)
│   │   ├── repository/        # Firebase Firestore data access
│   │   ├── LocationManager.kt # FusedLocationProvider integration
│   │   ├── MapsHelper.kt      # Google Maps utilities
│   │   └── NotificationService.kt # Firebase Messaging service
│   ├── ui/
│   │   ├── components/        # Reusable Compose UI components (Cards, Buttons, etc.)
│   │   ├── screens/           # Feature-specific screens (Home, Login, Profile, etc.)
│   │   ├── theme/             # Material3 Theme, Color, and Typography definitions
│   │   └── MainActivity.kt    # Main entry point and navigation container
│   └── viewmodel/             # State management using ViewModels
├── app/src/main/res/          # Android resources (drawables, layouts, etc.)
├── build.gradle.kts           # Project-level build configuration
└── google-services.json       # Firebase configuration file
```

---

## Getting Started

### Prerequisites

- Android Studio Ladybug or later
- Android SDK 34+
- A Firebase project with Authentication, Firestore, and Cloud Messaging enabled
- Google Maps API key (configured in the project)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd sd_smart_parking_app
   ```

2. **Open the project**
   Open Android Studio and select **Open** -> Navigate to the `sd_smart_parking_app` folder.

3. **Configure Firebase**
   Place your `google-services.json` file in the `app/` directory.

4. **Sync Project**
   Wait for Gradle to sync and download all dependencies (Firebase, Compose, Google Maps).

5. **Run**
   Select an Android Emulator or physical device (API 24+) and press `Shift + F10`.

---

## Architecture

The app follows the **MVVM (Model-View-ViewModel)** pattern recommended by Google.

- **UI Layer**: Built entirely with **Jetpack Compose**, making it reactive and declarative.
- **ViewModel Layer**: Manages UI state and business logic, surviving configuration changes and interacting with repositories.
- **Data Layer**: Centralized in **Repositories**, which handle data fetching from Firebase Firestore and local services.
- **Navigation**: Uses **Compose Navigation** with a centralized `NavGraph` and role-based routing.

---

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for branch naming, commit conventions, PR process, and code style guidelines.
