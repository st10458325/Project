# Release Notes — Mary Hair Salon

## v1.0.0 (Final / Play Store release) — changes since the prototype


### ⭐ Innovative features added in this version

| Feature | What it does | Where in code |
|---|---|---|
| **Push notifications (FCM)** | Stylists are notified of new bookings; customers are notified when a booking is accepted, declined or changed — even when the app is closed. Android 13+ permission handled. | `MarySalonMessagingService`, `NotificationRepository`, `MainActivity` |
| **Offline cancellations + background sync** | Cancelling with no internet is queued locally and replayed automatically by WorkManager when the connection returns. A "Cancelling…" badge shows the pending state. | `PendingActionsStore`, `BookingSyncWorker`, `SyncManager`, `NetworkMonitor` |
| **Weather forecast for your appointment** | Choosing a date shows the forecast and temperature range for the salon's location. | `WeatherRepository` (Open-Meteo) |
| **Public-holiday awareness** | South African public holidays are blocked on the calendar with a "Closed for…" note. | `HolidayRepository` (Nager.Date) |
| **Multi-language** | In-app switching between English, Afrikaans and isiZulu. | `LocaleManager`, `values-af`, `values-zu` |
| **Tip of the Day** | Fresh advice card on the stylists screen. | `AdviceRepository` (AdviceSlip) |
| **Country dial-code picker** | International phone numbers at sign-up, with offline fallback list. | `CountryRepository`, `CountryCodePicker` (RestCountries) |

### Other improvements

* Atomic slot locking with Firestore transactions — no double-bookings
* Google Sign-In using Credential Manager
* Email verification banner and enforcement before booking
* Manager dashboard: all bookings, stylist reassignment, staff roles, catalogue CRUD (categories, services, discounts)
* Light / dark / system theme
* Friendly authentication error messages
* Redesigned collapsing headers and floating search on the Services screen
* Logging added across auth, booking, sync and notification flows

### Quality & delivery

* Automated unit tests (JUnit 4 + MockK)
* GitHub Actions CI: runs tests and builds a debug APK on every push/PR
* Signed release build prepared for Google Play

### Known issues / next steps

* Employee profile tab is a placeholder (statistics and availability to come)
* Weather forecast only available up to 16 days ahead (API limit)
