# FinClarity — Personal Finance Management System for Android
**SE3092 Platform Based Development — Assignment 01 (Year 3, Semester 2, 2026)**  
*Developed by Malindu Gurunada, Himasha Hettiarachchi, and Thilakarathna J K D R S*

FinClarity is a modern, self-contained Android personal finance management application written in 100% Kotlin with Jetpack Compose, Material Design 3, MVVM architecture, Dagger Hilt dependency injection, Firebase Authentication, Cloud Firestore, and a Room SQLite offline cache.

The application is specifically designed around the financial scenario of **Mr. Kavindu Silva**—a junior software engineer in Colombo with irregular multi-source income (Salary, Freelance, AdSense USD, Crypto Trading), multi-currency transactions, recurring commitments, and a primary savings goal for a **MacBook Pro M4 (LKR 490,000)**.

---

## Key Features

1. **Authentication & Session Security**:
   - Firebase Authentication (Email/Password & Google Sign-In via AndroidX Credentials API).
   - Biometric Authentication (Fingerprint & Face Recognition via AndroidX `BiometricPrompt`).
   - Strict Firestore Security Rules (`firestore.rules`) enforcing UID data isolation (`request.auth.uid == userId`) and schema field validation.

2. **Multi-Source Income Ledger**:
   - Supports Salary, Freelance, AdSense USD, and Crypto Trading income streams.
   - Converts USD/foreign currency at entry time to LKR base equivalents (`MoneyConverter`).
   - Computes single "True Monthly Income" in LKR across all sources.
   - Crypto Year-To-Date (YTD) realized P/L tracking (gains & negative losses).

3. **Expense Capture & Management**:
   - 3-tap quick-add expense flow with merchant-to-category memory.
   - Categorized as **Committed** ("Locked in") vs. **Discretionary** ("Your choice").
   - Multi-account support (Cash, Debit Card, Bank Transfer, Online).
   - Confirmed deletion dialogs with instant Snackbar undo.

4. **Savings Goal Engine**:
   - Single & multiple savings goals (e.g. `MacBook Pro M3 Fund`, target LKR 550,000, 70% progress bar fill).
   - Automated required monthly savings rate recalculations.
   - Append-only contribution allocations (`ALLOCATION`) and reversal entries (`REVERSAL`).

5. **Live Monthly Dashboard & Analytics**:
   - Launch dashboard with real-time Cloud Firestore snapshot listeners.
   - Total Available Balance (`LKR 284,500.00`), Income vs. Expenses summary cards.
   - Outflow summary card with 67% Committed vs. 33% Discretionary progress bar split.
   - Income-source and expense-category breakdown analytics.

6. **Local Room Reminders & Notifications**:
   - Persistent Room SQLite database (`finclarity_reminders.db`) storing due bill alerts and notification history.
   - Restores scheduled reminders on app restart and device reboot (`ReminderBootReceiver`).
   - Clears user reminders and cancels workers automatically on sign-out.

---

## Technology Stack

| Component | Technology / Library | Version |
| :--- | :--- | :--- |
| **Language** | Kotlin | `2.2.10` |
| **Build System** | Gradle (Kotlin DSL) / AGP | `9.3.1` |
| **SDK Prerequisites** | Compile / Target SDK 37 (Min SDK 26) | Android 8.0+ |
| **UI Framework** | Jetpack Compose / Material Design 3 | Compose BOM `2026.02.01` |
| **Navigation** | Navigation Compose (Type-Safe Routes) | `2.10.2` |
| **Architecture** | MVVM + Unidirectional Data Flow (`StateFlow`) | Lifecycle `2.11.0` |
| **Dependency Injection** | Dagger Hilt | `2.60.1` |
| **Cloud Backend** | Firebase Auth, Cloud Firestore | Firebase BOM `34.19.0` |
| **Local Offline Cache** | Room SQLite Database | `2.8.5` |
| **Local Preferences** | Jetpack DataStore | `1.1.0` |
| **Biometrics** | AndroidX BiometricPrompt | `1.1.0` |

---

## Project Structure

```text
kotlin-project/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/example/personalfinanceapp/
│       │       ├── core/
│       │       │   ├── database/        # Room Database (AppDatabase, ReminderDao, ReminderEntity)
│       │       │   ├── model/           # Shared domain models (Expense, Income, Goal, Money)
│       │       │   └── money/           # Currency conversion & validator logic
│       │       ├── feature/
│       │       │   ├── auth/            # Firebase Auth & BiometricPrompt
│       │       │   ├── category/        # Categories & Firestore persistence
│       │       │   ├── dashboard/       # Dashboard ViewModel & Analytics
│       │       │   ├── expense/         # Expense CRUD & Firestore repository
│       │       │   ├── goal/            # Savings Goals & Contributions
│       │       │   ├── history/         # Unified Transaction History
│       │       │   ├── income/          # Income Ledger & USD Conversion
│       │       │   ├── preferences/     # DataStore User Preferences
│       │       │   └── reminder/        # Room Reminders & Boot Receiver
│       │       ├── navigation/          # Type-safe Routes & AppNavHost
│       │       └── ui/theme/            # Screen Conventions v1.0 Material 3 Theme
│       ├── test/                        # 586 JUnit unit tests
│       └── androidTest/                 # Compose UI instrumentation tests
├── docs/                                # Technical Architecture & Schema
├── firestore.rules                      # Cloud Firestore Security Rules
├── firestore.indexes.json               # Firestore Composite Query Indexes
├── firebase.json                        # Firebase Emulator Suite Configuration
├── gradle/libs.versions.toml            # Version Catalog
└── README.md
```

---

## Setup & Building

### Prerequisites
- **Android Studio** (2026.1.1+ / Ladybug or newer)
- **JDK 17** (configured as Gradle JDK)
- **Android SDK 35/37** installed
- **Android Device or Emulator** running Android 8.0 (API 26+) or higher

### Build Debug APK
Execute from terminal:
```powershell
.\gradlew.bat assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Execute Unit Tests
Execute from terminal:
```powershell
.\gradlew.bat testDebugUnitTest
```
*Result*: **586 unit tests pass 100%**.

---

## Firebase Configuration

The app is connected to the team's live Firebase project:
* **Firebase Project ID**: `se3092-personal-finance`
* **Package Name**: `com.example.personalfinanceapp`
* **Configuration File**: `app/google-services.json`

Security rules (`firestore.rules`) enforce user document ownership (`request.auth.uid == userId`) and schema field/type validation across all collections.

---

## Git Workflow & Conventions

The team enforces:
* `main`: Production release branch.
* `dev`: Integration branch for team features.
* `feat/<member>/<feature>` or `doc/<member>/<topic>`: Isolated feature branches.
* All commits follow the **Conventional Commits** standard (`feat(scope): ...`, `fix(scope): ...`, `docs(scope): ...`, `test(scope): ...`) validated via `.\scripts\validate-commit-messages.ps1`.
