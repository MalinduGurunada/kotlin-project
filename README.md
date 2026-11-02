# SE3092 Personal Finance App

An Android personal-finance application developed as a third-year Computer Science group project. The project aims to help an individual record income and expenses, organise spending, plan savings goals, track expected payments, and manage recurring costs in one place.

The repository is in active feature and regression development. Authentication, Firestore-backed finance repositories, Compose screens, dashboard navigation, recurring commitments, due-item confirmation, and reminder scheduling are implemented, with maturity and test coverage varying by feature.

## Problem and objectives

Personal financial information is often split across notes, spreadsheets, banking applications, and memory. This makes it difficult to understand cash flow and plan upcoming commitments. This application is intended to provide a consistent record of financial activity while preserving the original currency and historical exchange-rate context of each transaction.

### Assignment scenario and proposed response

The SE3092 assignment describes Kavindu managing irregular income from multiple sources while needing to track expenses, recurring commitments, outstanding freelance payments, and savings goals. Those are the **scenario requirements**. Our **proposed solution** maps them to separate income records by source, categorised expense records, recurring-expense templates, receivables that remain distinct from received income, and savings goals with contribution history. These solution features are planned unless the status table below marks their supporting code as implemented.

The main objectives are to:

- record income and expenses using decimal-safe money representations;
- distinguish committed and discretionary spending;
- organise transactions with user-defined categories;
- track savings goals and an append-only history of contributions and reversals;
- track receivables without counting them as received income;
- model recurring expense templates without treating them as confirmed expenses;
- present useful history and dashboard summaries; and
- protect each user's cloud data with Firebase Authentication and user-scoped Firestore access.

## Feature status

Status in this table refers to the current branch, not to work that may exist on another branch.

| Area | Status | Current evidence |
| --- | --- | --- |
| Android application shell | Implemented foundation | Single `MainActivity`, Compose theme, Hilt application/activity annotations |
| Navigation | Implemented | Type-safe authentication, dashboard, management, recurring, income, expense, receivable, and category routes |
| Shared finance models | Implemented | Category, income, expense, goal, contribution, recurring expense, receivable, money, and ID contracts |
| Repository contracts | Implemented | Interfaces for category, income, expense, goal, recurring expense, and receivable data |
| In-memory repositories | Implemented | Coroutine `Flow`-based fake implementations with validation and update behaviour |
| Repository unit tests | Implemented | JUnit/coroutines-test coverage for all six fake repositories |
| Authentication | Implemented | Firebase Auth repository, recovery-safe ViewModel, Compose screens, session navigation, and regression tests |
| Firestore persistence | Implemented by feature | Income, expense, category, receivable, goal, and recurring repositories use UID-scoped Firestore paths |
| Feature screens and ViewModels | Implemented by feature | Dashboard, Category, Income, Expense, Receivable, Recurring Commitments, Due Items, and More screens are connected |
| Goals and reminders | Partial | Goal progress and ledger persistence exist; full goal-management UI is incomplete. Recurring reminders support permission and session-owner checks |
| Security rules and indexes | Implemented | Checked-in Firestore rules, indexes, and Emulator Suite rule tests cover implemented cloud paths |

See [Architecture](docs/architecture.md) and [Firestore schema](docs/firestore-schema.md) for the verified design boundary and proposed next steps.

## Technology stack

Versions below come from `gradle/libs.versions.toml`, the Gradle wrapper, and the app module configuration.

| Technology | Configured version or setting |
| --- | --- |
| Kotlin | 2.2.10 |
| Android Gradle Plugin | 9.3.1 |
| Gradle wrapper | 9.6.0 |
| Java language level | 17 |
| Compile / target SDK | 37 |
| Minimum SDK | 26 |
| Jetpack Compose BOM | 2026.02.01 |
| Material 3 | Managed by the Compose BOM |
| Navigation Compose | 2.10.2 |
| AndroidX Lifecycle Runtime | 2.11.0 |
| Hilt | 2.60.1 |
| AndroidX Hilt Navigation Compose | 1.4.0 |
| Firebase BOM | 34.19.0 |
| Firebase products declared | Authentication and Cloud Firestore |
| JUnit | 4.13.2 |
| Kotlin Coroutines Test | 1.10.2 |
| AndroidX Test JUnit / Espresso | 1.3.0 / 3.7.0 |

The app uses Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, coroutines and `Flow`, Firebase libraries, and a single-activity architecture. Kotlin serialization is configured for typed navigation routes.

## Architecture summary

The intended architecture is feature-based and MVVM-style:

1. Compose screens render immutable state and send user actions.
2. ViewModels coordinate use cases and expose observable UI state.
3. Repository interfaces define feature-level data operations.
4. Firestore-backed and in-memory implementations provide production persistence and deterministic tests.

`MainActivity` hosts the Compose navigation graph, while Hilt modules bind authentication, finance repositories, clocks, reminder coordination, and supporting services.

More detail and diagrams are available in [docs/architecture.md](docs/architecture.md).

## Firebase Android emulator tests

Repository tests running inside an Android AVD reach the host Firebase emulators through ADB reverse tunnels. Start the local Auth and Firestore emulators in one terminal:

```powershell
firebase emulators:start --config firebase.json --project demo-personal-finance-income-rules --only auth,firestore
```

With the AVD running, configure the temporary tunnels before executing Goal or Recurring Firestore instrumentation tests:

```powershell
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
```

Remove the tunnels after the tests:

```powershell
adb reverse --remove tcp:9099
adb reverse --remove tcp:8080
```

These commands use only local emulators and do not deploy or modify cloud Firebase resources.

## Project structure

```text
kotlin-project/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/example/personalfinanceapp/
│       │       ├── core/model/       # Shared domain contracts
│       │       ├── feature/          # Repository interfaces and in-memory fakes
│       │       ├── navigation/       # Typed routes and root NavHost
│       │       ├── ui/theme/         # Compose Material theme
│       │       ├── FinanceApplication.kt
│       │       └── MainActivity.kt
│       ├── test/                     # Local JUnit repository tests
│       └── androidTest/              # Instrumented context smoke test
├── docs/
│   ├── architecture.md
│   └── firestore-schema.md
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

There are currently no separate validation/calculation, data-source, ViewModel, or Hilt module packages on this branch.

## Development prerequisites

- Android Studio with support for Android Gradle Plugin 9.4.1
- JDK 17
- Android SDK 37 for compilation
- An Android device or emulator running API 26 or later
- Git
- Access to the team's Firebase project if Firebase-backed development is required

Use the checked-in Gradle wrapper rather than a separately installed Gradle version.

## Clone and open the project

```powershell
git clone https://github.com/MalinduGurunada/kotlin-project.git
cd kotlin-project
```

1. Open the repository root in Android Studio.
2. Select JDK 17 for the Gradle JDK.
3. Allow Android Studio to install any missing Android SDK components and complete Gradle sync.
4. Select the `app` run configuration.

If working on an assigned feature, branch from the team's integration branch rather than committing directly to `main`.

## Firebase Android configuration

The Google Services plugin, Firebase BOM, Firebase Authentication, and Cloud Firestore dependencies are already declared. The current checkout also contains an Android Firebase configuration file whose package name matches `com.example.personalfinanceapp`; its values are intentionally not reproduced in this documentation.

For a fresh or replacement Firebase setup:

1. Obtain access to the approved team Firebase project. Do not create an unrelated production project without team agreement.
2. Register an Android app with package name `com.example.personalfinanceapp` if it is not already registered.
3. Download the Firebase Android configuration file from the Firebase console.
4. Place it at `app/google-services.json`. Never paste its contents into documentation or issue reports.
5. Enable only the Firebase products required by the feature being developed, such as Authentication and Firestore.
6. Coordinate security rules and indexes with the team before connecting production repositories.
7. Run a Gradle sync and build the app.

The repository currently contains no Firestore rules or index configuration, and no deployment status can be verified. See [docs/firestore-schema.md](docs/firestore-schema.md) before implementing persistence.

## Build and run

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

Alternatively, select an emulator or connected device in Android Studio and choose **Run app**. The current UI starts at a placeholder login destination and can navigate to a placeholder home destination; it does not yet perform real authentication.

### Emulator guidance

1. Open **Device Manager** in Android Studio.
2. Create a phone virtual device with API 26 or later; an image close to target API 37 is preferred when available.
3. Use a Google APIs or Google Play system image for later Firebase testing.
4. Start the emulator, wait for Android to finish booting, and run the `app` configuration.
5. Confirm that the emulator has network access before testing future Firebase integrations.

## Tests

Run local unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run instrumented tests on a running emulator or connected device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The meaningful local tests exercise the fake repositories' observation, save/update, filtering, archive/activation, deletion, contribution, reversal, and validation behaviour. The instrumented suite currently contains only the generated application-context check. No Firebase emulator or Firestore security-rule tests are present.

## Git workflow

The team workflow uses:

- `main` for stable integrated work;
- `dev` as the shared integration branch; and
- `feat/<member>/<topic>` or `doc/<member>/<topic>` branches for isolated work.

Create a focused branch from the agreed base, keep commits scoped to one concern, open a pull request, and merge only after review and checks. Avoid committing directly to shared branches or mixing unrelated features in one pull request.

## Team responsibilities

| Member | Responsibility |
| --- | --- |
| Malindu | Android foundation, shared models, repository contracts, fake repositories, money rules, and income |
| Himasha | Authentication, expenses, Firebase security, dashboard, and assigned features |
| Raveen | Categories, UID-scoped Android DataStore preferences, history, goals, recurring expenses, and documentation |

These are team assignments, not claims that every listed feature is complete. The submission deadline is **30 October 2026**.

## Documentation

- [Architecture and domain contracts](docs/architecture.md)
- [Firestore schema, security, and index plan](docs/firestore-schema.md)

## Current development status

As of 16 October 2026, the checked-out branch provides a compilable project foundation and tested in-memory finance repositories. Integration work remains for authentication, ViewModels, complete Compose screens, dependency bindings, Firebase repositories, security rules, indexes, dashboard/history behaviour, and protected navigation. Documentation labels intended behaviour as planned until corresponding code or configuration is present in this branch.
