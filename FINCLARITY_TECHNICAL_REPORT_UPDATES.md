# FinClarity Technical Report Content Updates
**Module:** SE3092 Platform Based Development — Assignment 01 (Year 3, Semester 2, 2026)  
**Application Title:** FinClarity — Personal Finance Management System for Android  
**Target Scenario:** Mr. Kavindu Silva (Junior Software Engineer, Colombo)  
**Date:** 9 November 2026  
**Repository Location:** `D:\PBD Assignment\Finane App\kotlin-project`

---

## 1. Verified Architecture & Layer Mapping

### 1.1 MVVM Layer Decomposition
- **View / UI Layer (`Composables`)**: Declarative Compose screens (`OverviewScreen`, `ActivityScreen`, `GoalsScreen`, `MoreScreen`, `LoginScreen`) render immutable UI state (`UiState`) and emit user events. Zero business logic inside UI composables.
- **ViewModel Layer (`@HiltViewModel`)**: State holders (`AuthViewModel`, `ExpenseViewModel`, `GoalViewModel`, `DashboardViewModel`) expose `StateFlow` streams created via `stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), ...)`.
- **Repository Layer**: Interfaces (`ExpenseRepository`, `IncomeRepository`, `GoalRepository`, `ReceivableRepository`, `CategoryRepository`, `PreferencesRepository`) implemented by Firestore production classes (`FirestoreExpenseRepository`, etc.) and in-memory test fakes (`FakeExpenseRepository`, etc.).
- **Data Layer**: Cloud Firestore NoSQL SDK with real-time `addSnapshotListener`, Room SQLite Database (`finclarity_reminders.db`), and Jetpack DataStore.

### 1.2 Dependency Injection (Dagger Hilt)
- `@SingletonComponent`: Binds long-lived singletons (`FirebaseAuth`, `FirebaseFirestore`, `AppDatabase`, repositories).
- `@ViewModelComponent`: Binds ViewModel-scoped dependencies.
- Hilt Modules: `AuthModule.kt`, `DatabaseModule.kt`, `ExpenseModule.kt`, `GoalModule.kt`, `IncomeModule.kt`, `NotificationModule.kt`, `RecurringModule.kt`.

---

## 2. Verified Firestore NoSQL Schema & Field Types

### 2.1 Collection Hierarchy
- `/users/{userId}` — Base user document.
- `/users/{userId}/expenses/{expenseId}` — Expense document (`categoryId`, `categoryNameSnapshot`, `originalAmountDecimal`, `baseAmountMinor`, `spendingClass`, `paymentChannel`, `occurredDate`).
- `/users/{userId}/incomes/{incomeId}` — Income document (`source`, `entryKind`, `money` map, `cryptoRealizedDetails`).
- `/users/{userId}/goals/{goalId}` — Savings goal document (`targetAmountMinor`, `startDate`, `targetDate`).
- `/users/{userId}/goals/{goalId}/contributions/{contributionId}` — Contribution entry (`amountMinor`, `kind`, `reversesId`).
- `/users/{userId}/receivables/{receivableId}` — Receivable document (`expectedOriginalDecimal`, `currency`, `archived`).
- `/users/{userId}/categories/{categoryId}` — Category document (`name`, `defaultSpendingClass`, `archived`).
- `/users/{userId}/fcmRegistrations/{registrationId}` — FCM push registration tokens.

---

## 3. Multi-Currency & Financial Accuracy Model

- **Integer Minor Units**: Monetary values stored strictly as minor unit integers (`baseAmountMinor: Long`) or exact `BigDecimal` strings, eliminating binary floating-point rounding errors.
- **USD Conversion Snapshots**: Foreign currency USD entries preserve original amount, applied exchange rate, and converted LKR base amount at entry time (`MoneyConverter.kt`).
- **Crypto Realized P/L**: Tracks trading gains and negative losses (`CryptoRealizedCalculator.kt`), correctly subtracting losses from total monthly income in `IncomeResultCalculator.kt`.

---

## 4. Server-Side Security Architecture (`firestore.rules`)

- Enforces `request.auth.uid == userId` across all `/users/{userId}/*` collections.
- Requires `request.auth.token.email_verified == true` for financial write operations.
- Validates document schema fields, prevents ownership forging, and enforces positive integer amounts.
- Tested via `test/firestore.rules.test.js` using Firebase Emulator Suite.

---

## 5. Local Offline Cache & Room Database

- **Room SQLite Database (`finclarity_reminders.db`)**: Stores due bill alerts and notification history (`AppDatabase.kt`, `ReminderDao.kt`, `ReminderEntity.kt`).
- **Device Reboot Restoration**: `ReminderBootReceiver` listens to `RECEIVE_BOOT_COMPLETED` to reschedule `AlarmManager` alarms on reboot.
- **Firestore Offline Caching**: Firestore snapshot listeners maintain offline document caching for responsive reads and queued pending writes during offline mode.

---

## 6. Testing & Build Verification Evidence

- **Unit Test Suite**: Executed `./gradlew app:testDebugUnitTest`: **594 unit tests passed 100%**.
- **Debug APK Build**: Executed `./gradlew app:assembleDebug`: **Build finished successfully** (`app/build/outputs/apk/debug/app-debug.apk`).
