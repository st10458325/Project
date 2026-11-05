# Mary Hair Salon — Android Application

![Android CI](https://github.com/st10458325/Mary-Hair-Salon-Application/actions/workflows/android-ci.yml/badge.svg)

A native Android app for **Mary Hair Salon** built with **Kotlin**, **Jetpack Compose**, **Material 3** and **Firebase**. It serves three roles — **Customers**, **Employees (stylists)** and **Managers** — with offline support, live push notifications, four external API integrations and three languages.

📺 **Demo video:** [GOOGLE_LINK_HERE](https://drive.google.com/file/d/1i8M2twzMsTMYNGeIUOJGBkUz5gg_ya_q/view?usp=drive_link)

<!-- Add screenshots: create a /screenshots folder, commit the images, and uncomment -->
<!--
| Login | Services | Booking form | My bookings |
|---|---|---|---|
| ![](screenshots/login.png) | ![](screenshots/services.png) | ![](screenshots/booking.png) | ![](screenshots/bookings.png) |

| Employee schedule | Manager dashboard | Dark / light | Afrikaans |
|---|---|---|---|
| ![](screenshots/employee.png) | ![](screenshots/manager.png) | ![](screenshots/theme.png) | ![](screenshots/af.png) |
-->

---

## 1. Purpose of the app

Salon bookings are usually made over WhatsApp or phone calls, which leads to double-bookings, forgotten appointments and no single view of the day. This app solves that by:

* letting **customers** browse services, pick a stylist, date and time, and manage their own appointments;
* giving **stylists** a live schedule where they accept, decline or complete bookings;
* giving the **manager** one dashboard for every booking, the staff list (promote/demote roles) and the service catalogue (categories, prices, discounts);
* **notifying** the right person the moment a booking is created or changes status.

## 2. Design considerations

| Concern | Decision | Why |
|---|---|---|
| **No double-booking** | Every booking writes a `bookingSlots/{employeeId_date_time}` document inside a Firestore **transaction** (`BookingRepository`). If the slot document already exists, the transaction throws `SlotUnavailableException`. | Two customers tapping "Confirm" at the same moment can never both succeed. |
| **Offline first** | Firestore persistent cache (unlimited) + a DataStore queue (`PendingActionsStore`) for cancellations + WorkManager (`BookingSyncWorker`). | Transactions need a live connection, so cancel *intent* is stored locally and replayed when connectivity returns. |
| **Never block the UI on the network** | Role lookup has a 4 s timeout (falls back to customer); FCM token registration is fire-and-forget. | A poor connection must not freeze the launch screen. |
| **Security** | Role-based routing; email verification required before booking; pricing is re-resolved from the catalogue (`resolvePricing`) instead of trusting client values; managers can only assign customer/employee roles from the app. | Prevents tampering and accidental privilege escalation. |
| **Architecture** | MVVM: Compose screens → `ViewModel` (`StateFlow` UI state) → repositories → Firebase / Retrofit. | Keeps UI stateless and logic unit-testable. |
| **Pure, testable logic** | `friendlyAuthError`, `bookingStatusLabel`, `mapCountriesToDialCodes` are pure functions. | Can run in CI on the JVM without an emulator. |
| **Graceful degradation** | Each external API has a fallback (hard-coded dial codes, hidden tip card, empty holiday list, no forecast beyond 16 days). | Decorative APIs must never break booking. |
| **Accessibility & theming** | Light / dark / system theme (DataStore), Playfair Display + Inter typography, supportsRtl, per-app language. | Comfortable on any device and for South African users in their own language. |
| **Logging** | `Log.d/e` with consistent tags (`AuthRepo`, `BookingRepo`, `MainActivity`, `SyncManager`, …) around auth, routing, snapshot errors, FCM and sync. | Makes behaviour traceable in Logcat. |

## 3. Features

* **Auth:** email/password and Google Sign-In (Credential Manager), email-verification banner, friendly error messages.
* **Customer:** services catalogue with search, booking form (stylist, Incall/Outcall, calendar, time grid, live price with discount), reschedule, cancel, status filters.
* **Employee:** live schedule, Accept / Decline / Mark completed.
* **Manager:** all bookings with status change and stylist reassignment, staff role management, catalogue CRUD.
* **External APIs (Retrofit + Moshi):** RestCountries (dial codes), Nager.Date (SA public holidays block the calendar), Open-Meteo (forecast for the chosen date), AdviceSlip (tip of the day).
* **Localisation:** English, Afrikaans, isiZulu via `LocaleManager`.

## 4. Push notifications

Implemented with **Firebase Cloud Messaging** ([docs](https://firebase.google.com/docs/cloud-messaging)):

1. On launch, `NotificationRepository.registerCurrentDevice` fetches the FCM token and stores it in `fcmTokens/{uid}`; `MarySalonMessagingService.onNewToken` keeps it fresh.
2. Android 13+ `POST_NOTIFICATIONS` permission is requested in `MainActivity`.
3. When a booking changes, the backend sends a message to the stylist (new booking, `employeeNotified = false`) or the customer (status change, `customerNotified = false`).
4. `MarySalonMessagingService.onMessageReceived` builds the notification on the high-importance **bookings** channel; tapping it opens the app.

## 5. Automated testing

Unit tests live in `app/src/test/` and run on the JVM with **JUnit 4** and **MockK**:

| Test class | What it covers |
|---|---|
| `AuthErrorMapperTest` | Every Firebase exception → friendly message branch |
| `DataLogicTest` | Booking status labels; RestCountries → dial-code mapping (filtering, sorting, suffix handling) |

Run locally: `./gradlew test`  ·  Reports: `app/build/reports/tests/`

## 6. GitHub & GitHub Actions

* **Version control:** all Kotlin source is committed directly to this repository (no zip files), with small, descriptive commits.
* **Workflow:** [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) runs on every push and pull request to `main`:
  1. checks out the code and sets up JDK 17 with Gradle caching;
  2. recreates `google-services.json` from the `GOOGLE_SERVICES_JSON` repository secret (the real file is never committed);
  3. runs `./gradlew test` and uploads the HTML test report as an artifact;
  4. runs `./gradlew assembleDebug` and uploads the APK as an artifact.
* **Why:** it proves the project builds and passes tests on a clean machine, not just on the developer's laptop. The badge at the top shows the latest status; see the **Actions** tab for runs.

<!-- Add a screenshot of a green Actions run: ![CI run](screenshots/actions-green.png) -->

## 7. Project structure

```text
app/src/main/java/com/maryhairsalon/
├── data/ (local, models, network, repository)
├── ui/   (auth, customer, employee, manager, settings, components, theme)
├── sync/ (BookingSyncWorker, SyncManager)
├── locale/ (LocaleManager)
├── util/ (AuthErrorMapper, BookingStatus)
└── notifications/ (MarySalonMessagingService)
app/src/test/ … unit tests
.github/workflows/android-ci.yml
```

## 8. Getting started

* Android Studio (Ladybug or newer), JDK 17, `minSdk 24`, `targetSdk/compileSdk 36`.
* Add your own `google-services.json` to `app/` and set `google_web_client_id` in `strings.xml`.
* `./gradlew assembleDebug`

## 9. Publication readiness

* Signed release APK/AAB built with the app's own keystore (keystore not committed).
* Application id `com.maryhairsalon.customer`, version `1.0.0` (code 1).
* Store listing, screenshots and Play Console upload evidence: see `/screenshots` and `/publication`.

## 10. Release notes

See [RELEASE_NOTES.md](RELEASE_NOTES.md) — summarised below.

**v1.0.0 — changes since the prototype**

* ⭐ **Innovative:** real-time push notifications (FCM) for new bookings and status changes
* ⭐ **Innovative:** offline cancellations queued in DataStore and synced by WorkManager
* ⭐ **Innovative:** weather forecast + public-holiday awareness in the booking flow
* ⭐ **Innovative:** per-app language switching (English / Afrikaans / isiZulu) and light/dark theme
* Atomic double-booking protection, Google Sign-In, email verification, manager catalogue tools
* Automated unit tests and GitHub Actions CI

## 11. References

* Firebase Cloud Messaging — https://firebase.google.com/docs/cloud-messaging
* Firebase Auth / Firestore — https://firebase.google.com/docs
* Credential Manager — https://developer.android.com/identity/sign-in/credential-manager
* Jetpack Compose & Material 3 — https://developer.android.com/jetpack/compose
* WorkManager — https://developer.android.com/topic/libraries/work
* Retrofit — https://square.github.io/retrofit/ · Moshi — https://github.com/square/moshi
* APIs: RestCountries (restcountries.com), Nager.Date (date.nager.at), Open-Meteo (open-meteo.com), AdviceSlip (api.adviceslip.com)
* GitHub Actions — https://docs.github.com/actions

## License
For Mary Hair Salon management and client booking. All rights reserved.