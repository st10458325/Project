# Mary Hair Salon — Android Application

A modern, full-featured native Android application for **Mary Hair Salon**, built using **Kotlin**, **Jetpack Compose**, **Material 3**, and **Firebase**. The app delivers a tailored experience across three user roles: **Customers**, **Employees**, and **Salon Managers**, complete with offline capabilities, external API integrations, and multi-language localization.

![Android CI](https://github.com/st10458325/Mary-Hair-Salon-Application/actions/workflows/android-ci.yml/badge.svg)

---

## 🎥 Demo Video

📺 **[Watch the full walkthrough on YouTube](PASTE_YOUTUBE_LINK_HERE)**


---

## 📱 Features & Highlights

### 🔐 Authentication & Security
* **Role-Based Access**: Role-driven navigation and screen access for Customers, Employees, and Managers.
* **Firebase Auth & Firestore**: Secure user registration, authentication, and role management.
* **Google Sign-In**: Integrated using Android's modern **Credential Manager API**.
* **Email Verification**: Resend email verification workflow with an interactive `VerifyEmailBanner`.
* **User-Friendly Error Mapping**: Pure `AuthErrorMapper` maps raw Firebase exceptions to friendly messages.

### 💇 Customer Portal
* **Service Browsing**: Explore salon services, descriptions, and pricing.
* **Appointment Booking**: Select preferred staff, date, and time slot.
* **Public Holiday Awareness**: Calendar integrated with public holiday badges so users know salon holiday schedules.
* **Appointment Weather Forecast**: Displays weather conditions for chosen appointment dates to help customers plan ahead.
* **Booking Management**: View pending, upcoming, completed, and cancelled appointments.

### 👔 Employee Portal
* **Daily Schedule View**: View assigned customer appointments and service details.
* **Status Updates**: Update booking statuses (`Pending`, `Upcoming`, `Completed`, `Cancelled`).

### 👑 Manager Portal
* **Dashboard Overview**: Manage staff schedules, appointments, and salon services.
* **Revenue & Status Analytics**: Comprehensive metrics for salon operations and booking management.

### 🌍 Multi-Language Localization
* **Per-App Language Selection**: In-app language switching supported via `LocaleManager`.
* **Supported Languages**:
    * 🇬🇧 **English** (Default)
    * 🇿🇦 **Afrikaans** (`values-af`)
    * 🇿🇦 **isiZulu** (`values-zu`)

### 🔌 External API Integrations (Retrofit + Moshi)
1. **RestCountries API**: Country dial-code lookup for international phone numbers during registration (`CountryCodePicker`).
2. **Nager.Date API**: South African public holiday calendars for booking availability.
3. **Open-Meteo API**: Live weather forecasts integrated into appointment booking summaries.
4. **AdviceSlip API**: Dynamic "Tip of the Day" card on the About screen.

### 🔔 Real-Time Notifications
* **Client-side**: `MarySalonMessagingService` receives and displays push notifications; `NotificationRepository` registers each device's FCM token against the signed-in user.
* **Server-side**: A Firebase Cloud Function (`onBookingWritten`) listens for changes on the `bookings` collection and automatically pushes a notification to the relevant stylist (new booking) or customer (status change) within seconds — no polling, no manual trigger.

### 📶 Offline Support & Background Sync
* **Firestore Offline Persistence**: Unlimited local cache enabling offline browsing of services and bookings.
* **DataStore Queue**: Stores offline booking cancellations in `PendingActionsStore`.
* **WorkManager Sync (`BookingSyncWorker`)**: Automatically syncs queued offline cancellations to Cloud Firestore as soon as internet connectivity is restored.

---

## 🛠️ Tech Stack & Libraries

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Architecture**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
* **Navigation**: [Navigation Compose](https://developer.android.com/guide/navigation)
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/)
* **Backend**: [Firebase Authentication](https://firebase.google.com/products/auth), [Cloud Firestore](https://firebase.google.com/products/firestore), [Firebase Cloud Messaging](https://firebase.google.com/products/cloud-messaging), [Cloud Functions](https://firebase.google.com/products/functions) (Node.js + Admin SDK)
* **Networking**: [Retrofit 2](https://square.github.io/retrofit/), [Moshi](https://github.com/square/moshi), [OkHttp 4](https://square.github.io/okhttp/)
* **Async & Threading**: Kotlin Coroutines & StateFlow
* **Local Storage & Sync**: Jetpack DataStore Preferences & WorkManager
* **Testing**: JUnit 4, MockK, Jetpack Compose UI Testing (`ui-test-junit4`)
* **CI/CD**: [GitHub Actions](https://github.com/features/actions) — automated test run + debug build on every push/PR

---

## 📂 Project Structure

```text
app/src/main/java/com/maryhairsalon/
├── data/
│   ├── local/          # DataStore (PendingActionsStore)
│   ├── models/         # Domain & Firestore models (User, Booking, Service)
│   ├── network/        # Retrofit API interfaces & DTOs (RestCountries, NagerDate, OpenMeteo, AdviceSlip)
│   └── repository/     # AuthRepository, BookingRepository, CountryRepository, etc.
├── ui/
│   ├── auth/           # LoginScreen, SignupScreen, GoogleSignInButton
│   ├── customer/       # ServicesScreen, BookingsScreen, AboutScreen, BookingFormScreen
│   ├── employee/       # EmployeeDashboardScreen, EmployeeScheduleViewModel
│   ├── manager/        # ManagerDashboardScreen, ManagerViewModel
│   ├── settings/       # SettingsScreen, SettingsViewModel (Profile & Language)
│   ├── components/     # Reusable UI (CountryCodePicker, VerifyEmailBanner, RightNavigationDrawer)
│   └── theme/          # Material 3 Color scheme, Typography, Theme definition
├── sync/               # WorkManager workers (BookingSyncWorker)
├── util/               # AuthErrorMapper, BookingStatus, LocaleManager
└── notifications/      # FCM notification services

.github/
└── workflows/
    └── android-ci.yml  # Runs unit tests and builds a debug APK on every push/PR
```

*Note: the Cloud Function that sends push notifications (`onBookingWritten`) is deployed separately via the Firebase CLI and isn't part of this Android project's source tree.*

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug / Meerkat / Jellyfish or newer.
* **JDK**: Version 11 or 17.
* **Android SDK**: `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.
* **Firebase Setup**: A active Firebase project with `google-services.json` placed inside the `app/` directory.

---

### Continuous Integration
Every push and pull request to `main` automatically triggers [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml), which:
1. Runs the full unit test suite (`./gradlew test`) and uploads the test report as a build artifact.
2. Builds a debug APK (`./gradlew assembleDebug`) and uploads it as a downloadable artifact.

Check the **Actions** tab on GitHub for the status and artifacts of any given run.

---

## 📄 License
This project is for Mary Hair Salon management and client booking purposes. All rights reserved.
