# ClearLedger

**Personal Finance Management System for Android**
SE3092 – Platform Based Development · Assignment 01 · Year 3, Semester 2 (2026) · SLIIT Faculty of Computing

## 1. Project Overview

ClearLedger is a native Android app built for **Kavindu Silva**, a 25-year-old junior software engineer in Colombo. He earns from four sources (salary, freelance projects, Google AdSense in USD and crypto trading), cannot say where his money goes, and has abandoned four earlier tracking tools. His goal is to save about **LKR 490,000 for a MacBook Pro M4 within 12 months**.

The app addresses the reasons those tools failed:

| Kavindu's problem | How ClearLedger responds |
|---|---|
| Irregular multi-source, multi-currency income | Income sources and entries with an Expected/Received status. Only Received income counts towards monthly totals. Foreign amounts store the original amount, rate and base-currency amount. |
| Too much effort to log expenses | Quick-add expense in 3 steps: amount → category → save. Merchant memory and favourite categories speed up repeat entries. |
| No idea what is committed vs discretionary | Every expense is classified, and the dashboard shows the committed vs discretionary split and a category breakdown. |
| Subscriptions he forgot to cancel | Recurring commitments with a planned monthly total, due items and confirm/skip. |
| Goal feels abstract | Savings goals with progress, required monthly saving and an on-track estimate. The primary goal appears on the dashboard. |
| Missed freelance follow-ups, unknown crypto net result | Freelance projects with milestones, and crypto entries with realised profit/loss, kept separate from income. |
| Offline or abandoned tools | Firestore offline persistence and clear loading, empty and error states. |

Dashboard figures come only from what the user records. The app labels them "Based on recorded transactions" and does not present them as a bank balance.

## 2. Features

- Email/password registration, login and logout (Firebase Authentication); onboarding with display name and base currency
- Income sources, income entries and multi-currency recording with manual exchange rates
- Freelance projects with milestones and overdue detection
- Cryptocurrency entries (Buy / Sell / Result) with realised profit/loss
- Expenses: quick-add, edit, delete, list grouped by date, filter by month and category
- Categories with default committed/discretionary classification
- Recurring commitments: planned total, due items, confirm/skip
- Savings goals and contributions: progress, required monthly saving, on-track status
- Monthly dashboard: recorded income vs expenses, committed vs discretionary, category breakdown, goal progress, safe-to-spend
- Weekly check-in (expected vs actual balance)
- Offline indicator, local reminders, per-user security rules

## 3. Tech Stack

| Concern | Technology |
|---|---|
| Language | Kotlin (100%), Kotlin DSL for Gradle |
| UI | Jetpack Compose, Material Design 3 |
| Navigation | Navigation Compose (single activity) |
| Architecture | MVVM + Repository + use cases |
| State | StateFlow, Kotlin Coroutines (`viewModelScope`) |
| Dependency injection | Hilt |
| Auth | Firebase Authentication |
| Database | Cloud Firestore (real-time listeners, offline persistence) |
| Background work | WorkManager (reminders) |
| Min / Target SDK | API 26 (Android 8.0) / API 34+ |

## 4. Architecture Summary

```
UI (Composable screens)
   ↓ observes StateFlow<UiState>, sends events
ViewModel (@HiltViewModel)
   ↓
Use Case (pure Kotlin business rules)
   ↓
Repository interface (domain)
   ↓
Repository implementation (data)
   ↓
Firebase Auth / Cloud Firestore
```

Rules followed:
- Composables only render state and forward user events. They hold no business logic.
- ViewModels expose read-only `StateFlow`, never mutable state.
- Firestore is accessed only in the `data` layer. The UI never calls it directly.
- Business rules (monthly summary, safe-to-spend, goal progress, currency conversion) are pure Kotlin functions with unit tests.
- Money is stored as `Long` minor units (never floating point). Exchange rates are stored as decimal strings.
- Every dated record stores a `monthKey` (`YYYY-MM`) so monthly queries need only one equality filter and no composite index.
- Hilt provides all dependencies. ViewModels are never constructed manually in Composables.

### Package structure

```
com.example.clearledger
├── core/          shared helpers: money, month keys, Result, UiState, network status
├── currency/      exchange-rate handling and base-currency rules
├── data/          dto/mappers, repository implementations, data sources
├── di/            Hilt modules
├── domain/        models, repository interfaces, use cases
├── navigation/    routes and nav graphs
├── notifications/ reminder scheduling and notifications
├── profile/       onboarding and profile
└── ui/            auth, dashboard, expense, income, recurring, goals,
                   freelance, crypto, weeklycheckin, home, theme
```

## 5. Firestore Data Model

All data lives under the signed-in user, so one rule protects everything.

```
users/{uid}
 ├── incomeSources/{id}
 ├── incomeEntries/{id}
 ├── freelanceProjects/{id}
 ├── cryptoTrades/{id}
 ├── categories/{id}
 ├── expenses/{id}
 ├── recurring/{id}
 ├── goals/{id}   (+ contributions)
 └── checkins/{id}
```

<<Verify these collection names against your code and update if they differ.>>

Multi-currency records store the original amount, original currency, exchange rate, and base-currency amount (minor units). Saved base amounts do not change when later rates change.

Security rules are in `firestore.rules`: a user can read and write only documents under their own `users/{uid}`, and everything else is denied.

## 6. Firebase Configuration

1. Go to the [Firebase console](https://console.firebase.google.com) and create a project.
2. Add an **Android app** with package name `com.example.clearledger`.
3. Download `google-services.json` and place it in the `app/` folder. It is git-ignored and **must not be committed**.
4. Enable **Authentication → Sign-in method → Email/Password**.
5. Create a **Cloud Firestore** database (production mode).
6. Publish the security rules:
   - Copy the contents of `firestore.rules` into **Firestore → Rules → Publish**, or
   - Run `firebase deploy --only firestore:rules`.
7. If the Firestore console reports a missing index, open the link in the error message and create it.

## 7. Build and Run

**Requirements:** Android Studio (latest stable), JDK 17, Android SDK 34, a device or emulator running Android 8.0 or higher.

```bash
git clone https://github.com/balasuthagar-code/clearledger-android.git
cd <<repo-folder>>
# add app/google-services.json (see section 6)
./gradlew assembleDebug
```

### Run unit tests

```bash
./gradlew testDebugUnitTest
```

Tests cover the business rules (money and currency, monthly summary, safe-to-spend, goals, recurring), the mappers and the ViewModels.


## 8. Team

| Member | Name | IT number | Area |
|---|---|---|---|
| Member 1 | T.Sanojan | IT23619180 | Platform, authentication, profile, security rules, QA |
| Member 2 | M.Thisanth | IT23613768| Income, currency, freelance, crypto, goals, weekly check-in |
| Member 3 | B.Suthagar | IT23184930 | Expenses, categories, recurring, dashboard |
