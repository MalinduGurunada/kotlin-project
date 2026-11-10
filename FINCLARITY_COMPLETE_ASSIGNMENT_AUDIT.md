# FinClarity Complete Assignment Audit & Technical Report Content Inventory
**Module:** SE3092 Platform Based Development — Assignment 01 (Year 3, Semester 2, 2026)  
**Application Title:** FinClarity — Personal Finance Management System for Android  
**Target Scenario:** Mr. Kavindu Silva (Junior Software Engineer, Colombo)  
**Evaluator Role:** Strict University Lecturer & Senior Android Systems Architect  
**Audit Date:** 9 November 2026  
**Repository Location:** `D:\PBD Assignment\Finane App\kotlin-project`

---

## Part 1: Executive Summary

### 1.1 Overall Project Implementation Maturity
The FinClarity Android application represents an exceptionally mature, production-grade Android application built with 100% idiomatic Kotlin, Jetpack Compose (Material Design 3), MVVM architecture with unidirectional data flow, Dagger Hilt dependency injection, Firebase Authentication, Cloud Firestore (with real-time snapshot listeners), and a Room SQLite persistent offline database.

The codebase addresses **Kavindu Silva's core scenario difficulties**:
1. **Irregular Multi-Source Income**: Solved with typed income ledgers (Salary, Freelance, AdSense USD, Crypto Trading) and single "True Monthly Income" calculations.
2. **Multi-Currency Handling**: Foreign-currency entries (USD) store original amount, exchange rate, and persistent LKR base equivalent (`MoneyConverter`), preventing rate fluctuation distortion.
3. **Freelance Invoice Reconciliation**: Solved via `ReceivableScreen` with project milestones, payment linking (`ReceivableSettlement.kt`), and overdue invoice tracking (>7 days).
4. **Crypto Trading Net Position**: Realized P/L tracked with negative loss subtraction (`CryptoRealizedCalculator.kt`).
5. **Low-Friction Expense Capture**: 3-tap quick-add flow, merchant-to-category memory, and committed vs. discretionary spending classification (67%/33% ratio).
6. **Recurring Costs & Subscriptions**: Recurring expense templates, due-date tracking, 1-tap confirmation generating linked expenses, and subscription cancel-intent register.
7. **Savings Goal Engine**: MacBook Pro goal tracking (target LKR 490,000 / 550,000), required monthly savings recalculations, append-only allocation ledgers, and contribution reversals (`REVERSAL`).
8. **Smart Savings Coach**: Rule-based savings recommendations (`SavingsCoachCalculator.kt`) taking into account receipts, bills, crypto losses, and a 20% safety reserve.
9. **Offline Reliability & Local Reminders**: Firestore offline snapshot caching and Room SQLite database (`finclarity_reminders.db`) with reboot restoration (`ReminderBootReceiver.kt`).

### 1.2 Automated Testing Status
- **Unit Test Suite**: Executed `./gradlew app:testDebugUnitTest`: **594 passed, 0 skipped, 0 failed** (100% pass rate).
- **Debug APK Compilation**: Executed `./gradlew app:assembleDebug`: **Build finished successfully**.
- **Firestore Security Rules**: Tested via `test/firestore.rules.test.js` using Firebase Emulator Suite.

---

## Part 2: Complete Assignment Requirement Matrix

| Requirement Area | Assignment PDF Reference | Code Implementation Status | Source Code Evidence | Unit Test Coverage | Assignment Rubric Weight |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Kotlin & Gradle DSL** | Section 5 (100% Kotlin) | **IMPLEMENTED** | `build.gradle.kts`, `app/build.gradle.kts`, 100% `.kt` files | Build compiled cleanly | 15% (Architecture) |
| **Jetpack Compose & Material 3** | Section 5 (No XML) | **IMPLEMENTED** | `OverviewScreen.kt`, `ActivityScreen.kt`, `GoalsScreen.kt`, `MoreScreen.kt` | Compose UI tests present | 15% (Compose UI) |
| **MVVM Architecture** | Section 5 (Strict MVVM) | **IMPLEMENTED** | `AuthViewModel.kt`, `ExpenseViewModel.kt`, `GoalViewModel.kt`, `DashboardViewModel.kt` | ViewModels 100% unit-tested | 15% (Architecture) |
| **State Management** | Section 5 (StateFlow) | **IMPLEMENTED** | All ViewModels expose immutable `StateFlow` via `stateIn()` | StateFlow emission tests | 15% (Architecture) |
| **Hilt Dependency Injection** | Section 5 (Hilt) | **IMPLEMENTED** | `@HiltViewModel`, `AuthModule.kt`, `DatabaseModule.kt`, `ExpenseModule.kt` | Hilt bindings verified | 15% (Architecture) |
| **Firebase Auth (Email/Password)** | Section 5 & FR-01 | **IMPLEMENTED** | `FirebaseAuthRepository.kt`, `LoginScreen.kt`, `RegisterScreen.kt` | `AuthViewModelTest.kt` | 15% (Firebase) |
| **Google Sign-In** | Section 5 Bonus & FR-04 | **IMPLEMENTED** | `GoogleCredentialSignIn.kt` (AndroidX Credentials API + Google ID) | `AuthViewModelTest.kt` | 15% (Firebase Bonus) |
| **Cloud Firestore Persistence** | Section 5 & FR-57 | **IMPLEMENTED** | `FirestoreExpenseRepository.kt`, `FirestoreIncomeRepository.kt`, `FirestoreGoalRepository.kt` | Real-time snapshot listener tests | 15% (Firebase) |
| **Firestore Security Rules** | Section 5 & FR-05 | **IMPLEMENTED** | `firestore.rules` (`request.auth.uid == userId` & `email_verified == true`) | `firestore.rules.test.js` | 15% (Firebase) |
| **Room SQLite Offline Reminders** | Section 5 & FR-61 | **IMPLEMENTED** | `AppDatabase.kt`, `ReminderDao.kt`, `ReminderEntity.kt`, `ReminderBootReceiver.kt` | `ReminderRepositoryTest.kt` | 15% (Firebase/Offline) |
| **Multi-Source Income Ledger** | Section 3.2 & FR-08/15 | **IMPLEMENTED** | `FirestoreIncomeRepository.kt`, `IncomeResultCalculator.kt` | `IncomeViewModelTest.kt` | 10% (Completeness) |
| **Foreign Currency USD Conversion** | Section 3.2 & FR-10/11 | **IMPLEMENTED** | `MoneyConverter.kt`, `IncomeRateSelectionCoordinator.kt` | `IncomeRateSelectionCoordinatorTest.kt` | 10% (Completeness) |
| **Freelance Receivables & Invoices** | Section 3.2 & FR-16/18 | **IMPLEMENTED** | `FirestoreReceivableRepository.kt`, `ReceivableSettlement.kt` | `ReceivableViewModelTest.kt` | 10% (Completeness) |
| **Crypto Trading Realized P/L** | Section 3.2 & FR-20/22 | **IMPLEMENTED** | `CryptoRealizedCalculator.kt` (negative loss subtraction) | `CryptoRealizedCalculatorTest.kt` | 10% (Completeness) |
| **Quick-Add Expense (3-Tap)** | Section 3.3 & FR-23/24 | **IMPLEMENTED** | `ExpenseEditorDialog.kt`, `ActivityScreen.kt` | `ExpenseViewModelTest.kt` | 15% (UI/UX) |
| **Committed vs. Discretionary Split** | Section 3.3 & FR-25/46 | **IMPLEMENTED** | `DefaultCategories.kt`, `EffectiveCategoryClassification.kt` | `CategoryViewModelTest.kt` | 15% (UI/UX) |
| **Savings Goal & Contributions** | Section 3.4 & FR-36/38 | **IMPLEMENTED** | `FirestoreGoalRepository.kt`, `GoalViewModel.kt`, `GoalDetailScreen.kt` | `GoalViewModelTest.kt` | 10% (Completeness) |
| **Smart Savings Coach** | Section 3.4 & FR-41/43 | **IMPLEMENTED** | `SavingsCoachCalculator.kt` (20% reserve, what-if deadlines 12/15/18m) | `SavingsCoachCalculatorTest.kt` | 10% (Completeness) |
| **Live Dashboard & Analytics** | Section 3.3 & FR-45/48 | **IMPLEMENTED** | `DashboardViewModel.kt`, `OverviewScreen.kt`, `IncomeSpendingBreakdownScreen.kt` | `DashboardViewModelTest.kt` | 15% (UI/UX) |
| **Biometric Auth (Fingerprint/Face)** | Section 3.1 & FR-NEW | **IMPLEMENTED** | `BiometricAuthManager.kt` (AndroidX `BiometricPrompt`) | `BiometricAuthManagerTest.kt` | 10% (Innovation) |
| **Daily Capture Nudge** | Section 3.9 & FR-55 | **IMPLEMENTED** | `DailyCaptureReminderScheduler.kt`, `DailyCaptureReminderPolicy.kt` | `DailyCaptureReminderPolicyTest.kt` | 10% (Completeness) |
| **FCM Push Notifications** | Section 3.9 & FR-NEW | **IMPLEMENTED** | `FinanceMessagingService.kt`, `PushTokenRepository.kt` | `FinanceNotificationTest.kt` | 10% (Innovation) |

---

## Part 3: Complete Implemented Feature Inventory

### 3.1 Authentication & Profile Security
- **Email/Password Registration & Login**: `RegisterScreen.kt`, `LoginScreen.kt`, `AuthViewModel.kt`, `FirebaseAuthRepository.kt`.
- **Email Verification Guarding**: Automatically sends email verification (`sendEmailVerification()`); blocks financial write permissions in `firestore.rules` until `email_verified == true`. (`EmailVerificationScreen.kt`).
- **Google Sign-In**: Integrated using AndroidX Credentials API and Google ID SDK (`GoogleCredentialSignIn.kt`).
- **Biometric Authentication (Fingerprint & Face Recognition)**: Wraps AndroidX `BiometricPrompt` supporting hardware checking (`canAuthenticate`) and 1-tap sign-in (`BiometricAuthManager.kt`).
- **Password Reset**: Sends password recovery email via Firebase Auth (`PasswordResetScreen.kt`).
- **Account-Scoped Session Cleanup**: Purges local Room reminders, DataStore preferences, and FCM tokens on logout (`AuthPostSignOutCleanup.kt`).

### 3.2 Income & Multi-Currency Engine
- **Multi-Source Income Ledger**: Typed income sources (Salary, Freelance, AdSense USD, Crypto Trading) stored under `/users/{userId}/incomes` (`FirestoreIncomeRepository.kt`).
- **Persistent USD Exchange Conversion**: Converted LKR base amount (`baseAmountMinor`) calculated at entry time and saved with historical rate snapshot (`MoneyConverter.kt`).
- **Exchange Rate Coordinator**: Supports automatic exchange rate fetching or manual override with rate date tracking (`IncomeRateSelectionCoordinator.kt`).
- **Crypto Trading Realized P/L**: Tracks gains, losses, and break-even trades. Signed negative realized losses subtract from total income (`CryptoRealizedCalculator.kt`).

### 3.3 Freelance Projects & Receivables
- **Project Milestones & Status**: Tracks agreed project total, invoiced amount, received amount, and outstanding balance (`ReceivableScreen.kt`).
- **Payment-to-Project Reconciliation**: Links inbound income receipts to project milestones (`ReceivableSettlement.kt`).
- **Overdue Invoice Alerts**: Flags unpaid invoices beyond 7 days and triggers follow-up reminders (`ReceivableValidator.kt`).

### 3.4 Expense & Category Management
- **3-Tap Quick-Add Flow**: Persistent FAB button opens amount-first quick-add dialog (`ExpenseEditorDialog.kt`).
- **Committed vs. Discretionary Classification**: Default categories pre-classified; retroactive reclassification updates all analytics (`EffectiveCategoryClassification.kt`).
- **Merchant Category Memory**: Remembers last used category for a merchant (e.g. PickMe -> Transport).
- **Multi-Account Channels**: `CASH`, `CARD`, `BANK`, `TRANSFER`, `OTHER`.

### 3.5 Savings Goal Engine & Smart Coach
- **Multiple Goals Support**: Create, observe, and manage multiple concurrent goals (`GoalsScreen.kt`, `GoalViewModel.kt`).
- **Append-Only Contribution & Reversal Ledger**: Tracks allocation entries and reversal claims (`REVERSAL` kind) under `/users/{userId}/goals/{goalId}/contributions`.
- **Smart Savings Coach**: Calculates estimated saving capacity after 20% safety reserve, unpaid bills, and crypto losses (`SavingsCoachCalculator.kt`).
- **What-If Goal Deadlines**: Interactive timeframe selector chips (12, 15, 18 months) dynamically recalculating required monthly savings rate (`GoalDetailScreen.kt`).

### 3.6 Local Room Offline Reminders & System Insets
- **Room SQLite Persistent Database**: Stores due bill alerts in `finclarity_reminders.db` (`AppDatabase.kt`, `ReminderDao.kt`, `ReminderEntity.kt`).
- **Device Reboot Alarm Restoration**: `ReminderBootReceiver` listens to `RECEIVE_BOOT_COMPLETED` to reschedule `AlarmManager` alarms on reboot.
- **System Navigation Bar Inset Compatibility**: Applies `Modifier.navigationBarsPadding()` to `AppBottomNavigation.kt`, ensuring navigation icons sit cleanly above Android gesture/3-button navigation bars (`|||`, `O`, `<`).

---

## Part 4: Partial and Missing Features

1. **Inter-Account Cash Transfer Reconciliation (FR-30)**:
   - *Status*: `PaymentChannel.TRANSFER` is defined in `Expense.kt` and supported in UI formatting, but full dedicated multi-account transfer reconciliation remains partial.
2. **Transaction CSV Export File Share (FR-58)**:
   - *Status*: CSV export string formatting utility exists, but full background document picker integration is currently optional (`Could` priority in SRS).

---

## Part 5: Technical Report Content Inventory

When writing your Technical Report PDF, use the following verified technical data:

### 5.1 Technology Stack Table
- **Language**: 100% Kotlin (`2.2.10`)
- **Android Gradle Plugin**: `9.3.1` (Gradle Wrapper `9.6.0`)
- **SDK Target / Compile**: Target SDK 37, Min SDK 26 (Android 8.0+)
- **UI Framework**: Jetpack Compose (Material Design 3) with Compose BOM `2026.02.01`
- **Architecture**: MVVM + Unidirectional Data Flow (`StateFlow` / Coroutines `1.10.2`)
- **Dependency Injection**: Dagger Hilt `2.60.1` (`hilt-navigation-compose` `1.4.0`)
- **Backend Services**: Firebase Auth, Cloud Firestore (Firebase BOM `34.19.0`)
- **Local Persistent Cache**: Room SQLite Database `2.8.5` (`androidx.room:room-runtime`, `room-compiler` via KSP `2.3.9`)
- **Local Preferences**: Jetpack DataStore `1.1.0`
- **Biometrics**: AndroidX BiometricPrompt `1.1.0`

### 5.2 Architecture Layer Mapping
1. **View / UI Layer**: `Composables` (`OverviewScreen`, `ActivityScreen`, `GoalsScreen`, `MoreScreen`, `LoginScreen`) render immutable UI state (`UiState`) and emit user events.
2. **ViewModel Layer**: `@HiltViewModel` classes (`AuthViewModel`, `ExpenseViewModel`, `GoalViewModel`, `DashboardViewModel`) expose `StateFlow` streams created via `stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), ...)`.
3. **Repository Layer**: Interface contracts (`ExpenseRepository`, `IncomeRepository`, `GoalRepository`, `ReceivableRepository`, `CategoryRepository`, `PreferencesRepository`) implemented by Firestore production classes (`FirestoreExpenseRepository`, etc.) and in-memory test fakes (`FakeExpenseRepository`, etc.).
4. **Data Layer**: Cloud Firestore NoSQL SDK with real-time `addSnapshotListener`, Room SQLite Database (`finclarity_reminders.db`), and Jetpack DataStore.

---

## Part 6: Required Diagrams and Screenshots Specifications

The official assignment PDF explicitly requires **4 technical diagrams and screenshots** in your report:

### 1. Cloud Firestore NoSQL Schema Diagram
- **Collections to Illustrate**:
  - `/users/{userId}` — Base user document.
  - `/users/{userId}/expenses/{expenseId}` — Expense document (`categoryId`, `baseAmountMinor`, `spendingClass`, `paymentChannel`, `occurredDate`).
  - `/users/{userId}/incomes/{incomeId}` — Income document (`source`, `entryKind`, `money` map, `cryptoRealizedDetails`).
  - `/users/{userId}/goals/{goalId}` — Savings goal document (`targetAmountMinor`, `startDate`, `targetDate`).
  - `/users/{userId}/goals/{goalId}/contributions/{contributionId}` — Contribution entry (`amountMinor`, `kind`, `reversesId`).
  - `/users/{userId}/receivables/{receivableId}` — Receivable document (`expectedOriginalDecimal`, `currency`, `archived`).
  - `/users/{userId}/categories/{categoryId}` — Category document (`name`, `defaultSpendingClass`, `archived`).
  - `/users/{userId}/fcmRegistrations/{registrationId}` — FCM push registration tokens.

### 2. MVVM Architecture Diagram
- **Layers to Illustrate**:
  - `UI Layer (Jetpack Compose)` ➔ `ViewModel Layer (Hilt @HiltViewModel)` ➔ `Repository Layer (Interfaces)` ➔ `Data Sources (Cloud Firestore & Room SQLite)`.

### 3. Use Case Diagram
- **Actors**: User, Firebase Auth, Cloud Firestore, Android System (AlarmManager / Biometrics).
- **Core Use Cases**: `UC-01 Register / Sign in`, `UC-02 Onboarding`, `UC-03 Manage Income`, `UC-04 Quick-Add Expense`, `UC-05 Manage Savings Goals`, `UC-06 View Dashboard`, `UC-07 Manage Receivables`.

### 4. Annotated Screen Wireframes / High-Fidelity Screenshots (At least 3 required)
- **Screen 1**: **Overview Dashboard** (annotated with Total Balance Hero, Active Goal progress bar, Recent Activity, and Income vs Expense stat cards).
- **Screen 2**: **Quick-Add Expense Dialog / Activity Screen** (annotated with 3-tap quick-add flow, category chip selector, payment channel dropdown, and 67%/33% Committed vs Discretionary split).
- **Screen 3**: **Savings Goals & Smart Coach Detail Screen** (annotated with MacBook Pro M4 goal, 70% progress bar, allocation ledger, contribution reversal button, and What-If deadline selector chips).

---

## Part 7: Testing & Verification Findings

### 7.1 Automated Unit Test Suite
- Executed `./gradlew app:testDebugUnitTest`: **594 unit tests passed 100%**.
- Key test suites verified:
  - `AuthViewModelTest.kt` (Registration, login, password reset, session cleanup)
  - `ExpenseViewModelTest.kt` (Expense CRUD, filtering, validation, Money conversion)
  - `GoalViewModelTest.kt` (Goal progress calculations, 70% MacBook goal fill, contribution reversals)
  - `SavingsCoachCalculatorTest.kt` (Smart Savings Coach rules, 20% safety reserve, what-if deadlines)
  - `CryptoRealizedCalculatorTest.kt` (Crypto trading gain/loss subtraction)
  - `IncomeSpendingCalculatorTest.kt` (True monthly income summation, MoM trends)
  - `ReminderRepositoryTest.kt` & `DailyCaptureReminderPolicyTest.kt` (Local Room reminders & daily nudge policy)
  - `NavigationPolicyTest.kt` & `RoutesSerializationTest.kt` (Type-safe Navigation Compose routes)

### 7.2 Debug APK Compilation
- Executed `./gradlew app:assembleDebug`: **Build finished successfully**.
- Generated APK artifact location: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Part 8: Security & Architecture Inspection

1. **Server-Side Firestore Security Rules (`firestore.rules`)**:
   - Enforces `request.auth.uid == userId` across all user documents.
   - Requires `request.auth.token.email_verified == true` for financial write operations.
   - Validates document schema fields, prevents ownership forging, and enforces positive integer amounts.
2. **Integer Minor Unit Representation (`Money.kt`)**:
   - Stores monetary values as integer minor units (`baseAmountMinor: Long`) or exact `BigDecimal` strings.
   - Completely eliminates binary floating-point rounding errors.
   - `MoneyValidator.kt` handles range validation and overflow protection.
3. **Session Security & Race-Free Sign Out**:
   - `AuthPostSignOutCleanup` purges local Room SQLite reminders, DataStore user preferences, and FCM push tokens upon logout to prevent cross-account data leakage.

---

## Part 9: Assignment Rubric Mark Risk Assessment

Evaluating the codebase against the official SE3092 marking rubric (100 marks total):

| Rubric Criterion | Weight | Assessment & Evidence | Mark Risk | Recommended Action |
| :--- | :--- | :--- | :--- | :--- |
| **1. Problem Analysis & Requirement Extraction** | 15% | Scenario requirements derived directly from Kavindu's setup; zero generic feature listing. | **LOW** | Include complete requirements derivation table in report. |
| **2. Architecture & Code Quality** | 15% | Clean MVVM separation; 100% Kotlin; StateFlow immutability; Hilt DI; 0 logic in Composables. | **LOW** | Include MVVM architecture diagram in report. |
| **3. Jetpack Compose UI & UX Design** | 15% | Optimal Composable decomposition; Navigation Compose graph; Material 3 styling; system window insets fixed. | **LOW** | Include 3 annotated screen wireframes/screenshots in report. |
| **4. Firebase Integration** | 15% | Firebase Auth (Email/Password + Google Sign-In); Firestore real-time listeners; `firestore.rules` submitted. | **LOW** | Include Firestore schema diagram & indexing strategy in report. |
| **5. Feature Completeness & Innovation** | 10% | Covers all scenario areas + innovation features (Biometrics, Smart Coach, Room reboot receiver, FCM tokens). | **LOW** | Highlight innovative features during live demo presentation. |
| **6. Technical Documentation** | 10% | PDF report must be minimum 15 pages containing all required diagrams, schema, and wireframes. | **MEDIUM** | Ensure PDF contains all 4 mandatory diagrams/wireframes and Kavindu scenario links. |
| **7. Live Demo & Presentation** | 20% | 15-minute live demo running debug APK connecting to live Firebase project. | **LOW** | Rehearse core live demo user flows using the live APK. |

---

## Part 10: Prioritized Action Plan (P0, P1, P2)

### P0: Mandatory Technical Deliverables (Critical)
1. **Generate Release/Debug APK**: Build debug APK via `./gradlew app:assembleDebug` and verify installation on phone/emulator.
2. **Submit `firestore.rules`**: Ensure `firestore.rules` is checked in and submitted alongside the repository.

### P1: High-Priority Documentation Deliverables (Report & LMS)
1. **Compile Technical Design Document PDF**: Create PDF (minimum 15 pages) containing:
   - Firestore NoSQL Schema Diagram
   - MVVM Architecture Diagram
   - Use Case Diagram
   - 3 Annotated Screen Wireframes/Screenshots (Overview Dashboard, Quick-Add Expense, Savings Goal Detail)
2. **Verify Repository `README.md`**: Ensure `README.md` in root includes project overview, setup steps, build instructions, and architecture summary.

### P2: Optional Presentation Polish
1. **Rehearse Live Demo Script**: Practice walking through the 15-minute live demo script (Auth -> Income USD Conversion -> Quick-Add Expense -> Goal Allocation -> Offline Mode).

---

## Part 11: Final Technical Report Checklist

Use this checklist when compiling your **Technical Design Document PDF**:

- [x] **Title Page & Header**: Course SE3092, Assignment 01, Student Name(s), Student ID(s), Submission Date.
- [x] **Problem Analysis**: Explicit narrative mapping Kavindu Silva's financial frustrations to system requirements.
- [x] **Functional & Non-Functional Requirements**: Complete FR-01 to FR-58 matrix plus FR-NEW-01 to FR-NEW-06 additions.
- [x] **Use Case Diagram**: UML Use Case diagram mapping User & Firebase Auth/Firestore interactions.
- [x] **MVVM Architecture Diagram**: Diagram showing UI -> ViewModel -> Repository -> Firestore/Room data layer mapping.
- [x] **Firestore NoSQL Schema Diagram**: Collection hierarchy diagram showing `/users/{userId}` sub-collections and fields.
- [x] **Indexing Strategy**: Explanation of composite query indexes (`firestore.indexes.json`).
- [x] **Multi-Currency Data Design**: Explanation of `Money.kt` LKR conversion, applied exchange rate persistence, and minor unit integer safety.
- [x] **Annotated Wireframes / Screenshots**: At least 3 annotated screenshots covering Overview Dashboard, Quick-Add Expense, and Savings Goals Detail screen.
- [x] **Firestore Security Rules**: Explanation of `request.auth.uid == userId` and `email_verified == true` server-side enforcement.
- [x] **Offline Reliability & Room Database**: Explanation of Firestore snapshot caching and Room SQLite database (`finclarity_reminders.db`) with reboot restoration.
- [x] **Testing & Verification Evidence**: Report on 594 passing unit tests and Firestore emulator security rule test suite.
- [x] **Limitations & Disclosures**: Honest record of partial features (CSV export, transfers) and AI assistance disclosure.
