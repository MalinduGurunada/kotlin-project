# FinClarity Firebase Security and Technical Report Audit

Audit date: 10 November 2026

Scope: repository at `D:\PBD Assignment\Finane App\kotlin-project`

Method: read-only source inspection, inspection of the existing debug APK, and execution of the existing local Firestore Rules emulator suite. No application code, Firebase resource, rule, index, database, or Word document was changed or deployed.

## Scope limitations

- `FinClarity_Technical_Design_Report_Final_v1.4.docx` is not present in the workspace, the surrounding assignment directory, or the supplied attachment directory. Its pages, statements, tables, and diagrams therefore could not be classified directly.
- The official SE3092 Assignment 01 PDF is also unavailable. A public-web search did not locate an authoritative copy. This audit does not invent rubric wording, page numbers, or marks.
- There is no `.firebaserc`, exported production ruleset, Firebase Console session, or authoritative deployment record in the repository. Production rules and indexes are **UNVERIFIED**.
- The existing APK is a debug APK, not a confirmed final-submission artifact. It was built on 5 November 2026, while relevant checked-in source files have later timestamps. Its Firebase project identifier was inspected, but it cannot prove the configuration of a different submitted APK.

## 1. Executive Verdict

| Area | Verdict | Basis |
|---|---|---|
| Firebase configuration | **PASS (repository and inspected debug APK); submitted build UNVERIFIED** | The Google Services plugin, Firebase BoM, Auth, Firestore, Messaging, and Installations dependencies are present. The package in `google-services.json` matches `applicationId`; the existing debug APK contains the intended project identifier. No production source calls `useEmulator`. |
| Authentication | **PASS (implementation); Console provider enablement UNVERIFIED** | Email/password, Google credential sign-in, password reset, verification email, user reload, and forced ID-token refresh are implemented. Whether Email/Password and Google providers are enabled in the live project requires Console access. |
| Firestore Security Rules | **FAIL** | UID and verified-email authorization are strong, but expense validation is materially incomplete and recurring-payment expense links remain mutable/deletable after creation. |
| Cross-account security | **PASS for checked-in rules and local emulator; production UNVERIFIED** | Owner paths are UID-scoped, unmatched paths deny access, and the executed emulator suite passed its cross-account assertions. This does not prove the deployed rules. |
| Firestore data modelling | **PASS with integrity defects** | The actual schema is coherent and preserves historical money data, but some server-side validations do not match the domain model. |
| Indexing | **PASS for current queries, with two unused declarations** | No missing manual/composite index was identified for the current query shapes. Both declared Expense composite indexes are unused by the current repositories and should not be described as active query support. |
| Offline functionality | **PARTIAL** | Persistent cache and queued non-transaction writes exist. Firestore transactions require a network connection, several operations use transactions or prerequisite reads, and production cache is not cleared on sign-out. |
| Production rules deployment | **UNVERIFIED** | Local files and emulator tests do not prove what is published. |
| Technical report accuracy | **UNVERIFIED — document unavailable** | Direct `CORRECT`/`NEEDS UPDATES` classification is impossible. Section 6 supplies verified replacement text that the report should use where applicable. |

### Overall conclusion

The checked-in design prevents an authenticated user from reading or writing another user's financial paths. All financial subcollections require `request.auth.token.email_verified == true`; the parent user document and FCM deletion are intentional exceptions. However, the rules are not complete enough to call the financial data layer secure: a verified owner can create malformed or internally inconsistent Expense documents, and can later update or delete the Expense paired with an immutable confirmed recurring occurrence. The application also exposes `PaymentChannel.TRANSFER` in the recurring UI while the rules reject it.

The project is not ready for an unqualified report claim such as “all financial records are fully validated by Firestore Security Rules” or “the repository rules are deployed to production.”

## 2. Firestore Security Findings

### 2.1 Confirmed strengths

1. **Default deny is present.** `firestore.rules:542-545` denies all unmatched paths.
2. **Owner isolation is path based.** `isOwner` requires both authentication and `request.auth.uid == userId` (`firestore.rules:5-8`). All application financial paths are nested below `/users/{userId}`.
3. **Email verification is enforced server-side for financial data.** `isVerifiedOwner` checks the token claim (`firestore.rules:10-15`). It is used for reads and writes to Expenses, Recurring Expenses, Recurring Occurrences, Categories, Incomes, Receivables, Goals, Contributions, and Reversal Claims (`firestore.rules:453-539`).
4. **Nested goal records are owner scoped and immutable where required.** Contributions are create-only and Reversal Claims are create-only/get-only (`firestore.rules:516-538`). A reversal must exactly negate an existing non-reversal contribution and be paired atomically with one claim (`firestore.rules:383-435`).
5. **Recurring occurrence creation is atomic.** A confirmed occurrence requires a linked Expense in the same post-write state, with deterministic IDs and matching link fields (`firestore.rules:88-110`). Occurrence documents cannot be updated or deleted (`firestore.rules:472-480`).
6. **Receivables and Incomes have exact schemas.** The rules restrict their field sets, types, enumerations, document IDs, supported currencies, and rate sources (`firestore.rules:126-328`, `270-327`, `499-506`). Receivable currency is immutable on update (`firestore.rules:503-505`).
7. **FCM registrations use the real path `users/{uid}/fcmRegistrations/{registrationId}`.** They are owner-written, client-unreadable, and validated against an exact five-field schema (`firestore.rules:17-30`, `442-450`; `PushTokenRepository.kt:110-130`).

### 2.2 Vulnerabilities and integrity gaps

#### Finding F-01 — Expense writes do not enforce the actual schema or valid money values

Severity: **High / assignment-critical integrity defect**

Evidence:

- `isValidExpenseData` checks only a subset of fields (`firestore.rules:32-50`).
- It does not use `keys().hasAll(...)` or `keys().hasOnly(...)`.
- `originalAmountDecimal` only has to be a string; it can be empty, negative-looking, non-numeric, or zero.
- `baseAmountMinor >= 0` permits zero, although the repository requires a positive amount (`app/src/main/java/com/example/personalfinanceapp/feature/expense/FirestoreExpenseRepository.kt:107-113`) and the mapper requires positive, consistent money (`app/src/main/java/com/example/personalfinanceapp/feature/expense/ExpenseFirestoreMapper.kt:61-74`).
- `id` does not have to match `{expenseId}`.
- `originalCurrency`, `rateLkrPerUnitDecimal`, `rateSource`, and `rateDate` are neither required nor validated.
- The rules do not verify that `baseAmountMinor` matches the original amount and applied rate.
- The rules do not require the referenced Category to exist.
- Arbitrary extra fields are accepted.
- The full application write shape is visible at `app/src/main/java/com/example/personalfinanceapp/feature/expense/FirestoreExpenseRepository.kt:122-140`.

Impact: a modified client can store data that the application later cannot map, can inflate or suppress dashboard totals, can forge a historical conversion snapshot, or can introduce unsupported fields. Ownership is still confined to the caller's UID, so this is not a cross-account disclosure.

Required remediation after this investigation: define an exact Expense field set; require `id == expenseId`; validate positive decimal amount, positive `baseAmountMinor`, allowed currencies/rate-source combinations, ISO date shapes, nullable optional-field types, and Category existence. Where Rules cannot safely recompute decimal multiplication, document that limitation and validate all server-verifiable invariants.

#### Finding F-02 — A confirmed recurring occurrence can be detached from its Expense after creation

Severity: **High / ledger-integrity defect**

Evidence:

- Occurrences are immutable after creation (`firestore.rules:472-480`).
- The linked Expense is validated during atomic occurrence creation through `existsAfter/getAfter` (`firestore.rules:105-110`).
- After creation, the generic Expense rule permits the verified owner to update or delete that Expense (`firestore.rules:453-457`). It does not preserve `recurringExpenseId` or `recurringOccurrenceId`, and it does not deny deletion of a linked recurring Expense.
- The repository's confirmation transaction writes both records together (`app/src/main/java/com/example/personalfinanceapp/feature/recurring/FirestoreRecurringRepository.kt:98-168`).

Impact: a user or modified client can leave a permanent `CONFIRMED` occurrence pointing to a missing Expense, or modify/remove its linkage. This weakens duplicate-payment and settlement integrity even though occurrence IDs themselves are deterministic.

Required remediation after this investigation: protect linked recurring Expenses after creation, or implement an atomic, explicitly modelled correction/reversal operation. Add tests for link removal, link replacement, amount mutation, and deletion after confirmation.

#### Finding F-03 — `TRANSFER` is a valid application value but an invalid Rules value

Severity: **Medium / user-visible functional defect**

Evidence:

- The Kotlin enum contains `TRANSFER` (`app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt:5-11`).
- The Recurring Commitments UI iterates every enum value (`app/src/main/java/com/example/personalfinanceapp/feature/recurring/ui/RecurringCommitmentsScreen.kt:585`).
- The repository writes `paymentChannel.name` (`app/src/main/java/com/example/personalfinanceapp/feature/recurring/FirestoreRecurringRepository.kt:243-259`).
- The rules allow only `CASH`, `CARD`, `BANK`, and `OTHER` for both Expenses and Recurring Expenses (`firestore.rules:41-42`, `76-78`).

Impact: selecting “Cash Transfer” for a recurring commitment produces a permission-denied write. The existing rules suite does not test this application operation.

Required remediation after this investigation: either allow `TRANSFER` consistently in Rules or remove/migrate the application value. Add a rules test that uses every UI-exposed enum value.

#### Finding F-04 — Structured cryptocurrency arithmetic is client-validated but not Rules-validated

Severity: **Medium / financial-integrity limitation**

Evidence:

- Client validation recalculates `baseAmountMinor`, derives signed profit/loss, and requires direction and magnitude to match proceeds minus cost basis minus fees (`app/src/main/java/com/example/personalfinanceapp/feature/income/IncomeValidator.kt:22-34`, `92-118`; `app/src/main/java/com/example/personalfinanceapp/feature/income/CryptoRealizedCalculator.kt:35-50`).
- Rules validate field types and non-negative decimal shapes but do not verify the arithmetic relationship or that the selected `pnlDirection` matches it (`firestore.rules:235-267`, `306-327`).

Impact: a modified client can store a valid-looking but arithmetically inconsistent crypto result within its own account. The stored amount is an **unsigned magnitude**; sign is represented separately by `pnlDirection`. A report must not describe `baseAmountMinor` as a signed loss value.

#### Finding F-05 — Parent user documents allow arbitrary owner-controlled content

Severity: **Low now; Medium if profile/financial fields are later stored there**

Evidence: `/users/{userId}` permits owner read/write without verification or schema validation (`firestore.rules:438-440`). The current application stores the display name in Firebase Authentication, not in this Firestore document (`app/src/main/java/com/example/personalfinanceapp/feature/auth/FirebaseAuthRepository.kt:111-132`), and no production repository writes a Firestore user profile.

Impact: currently limited because the path is unused by the app. Do not place trusted roles, plan entitlements, financial summaries, or authorization fields there under the present rule.

#### Finding F-06 — Several date and text constraints are syntactic rather than semantic

Severity: **Low to Medium data-quality limitation**

Evidence: `isIsoDate` checks only a `YYYY-MM-DD` regular expression (`firestore.rules:152-155`). Invalid calendar dates can pass. Goal/Contribution schema versions use equality without an explicit `is int` check (`firestore.rules:349`, `400`, `430`). Several notes/labels have no length limits.

Impact: malformed owner data can pass Rules and later fail Kotlin parsing. Add bounded text and stronger server-verifiable type checks; keep semantic calendar validation in the application where Rules cannot express it.

### 2.3 Email-verification operation matrix

| Path | Get/List | Create | Update | Delete |
|---|---:|---:|---:|---:|
| `/users/{uid}` | Owner, verification not required | Owner, verification not required | Owner, verification not required | Owner, verification not required |
| `/users/{uid}/fcmRegistrations/{id}` | Always denied to clients | Verified owner | Verified owner | Owner; verification not required |
| Expenses | Verified owner | Verified owner + partial validation | Verified owner + partial validation | Verified owner |
| Recurring Expenses | Verified owner | Verified owner + validation | Verified owner + validation | Always denied |
| Recurring Occurrences | Verified owner | Verified owner + validation | Always denied | Always denied |
| Categories | Verified owner | Verified owner + validation | Verified owner + validation | Always denied |
| Incomes | Verified owner | Verified owner + validation | Verified owner + validation | Verified owner |
| Receivables | Verified owner | Verified owner + validation | Verified owner + validation + immutable currency | Always denied |
| Goals | Verified owner | Verified owner + validation | Verified owner + validation | Always denied |
| Contributions | Verified owner | Verified owner + validation | Always denied | Always denied |
| Reversal Claims | Verified owner (`get` only) | Verified owner + validation | Always denied | Always denied |

The verification restriction is not merely a UI check. It is evaluated by Firestore Rules using the Firebase Auth token claim. The application reloads the user and forces an ID-token refresh after verification (`FirebaseAuthRepository.kt:159-201`), which is necessary for the claim used by Rules to become current.

## 3. Actual Firestore Schema

### 3.1 Relationship overview

```text
Firebase Auth UID
└── users/{uid}                              owner path; no enforced document schema
    ├── categories/{categoryId}
    ├── expenses/{expenseId}
    ├── incomes/{incomeId}                   optional link -> receivables/{receivableId}
    ├── receivables/{receivableId}
    ├── recurringExpenses/{recurringId}
    ├── recurringOccurrences/{occurrenceId}  link -> recurringExpenses + expenses
    ├── goals/{goalId}
    │   ├── contributions/{contributionId}
    │   └── reversalClaims/{originalId}
    └── fcmRegistrations/{sha256(identifier)}
```

### 3.2 Collection and field inventory

| Exact path | Fields written by the app | Relationships and ownership | Read/query pattern |
|---|---|---|---|
| `users/{uid}` | No production Firestore profile write was found. Display name is stored in Firebase Auth. | Owner-only path; unverified owner allowed; no schema. | No production query found. |
| `users/{uid}/categories/{categoryId}` | `id: string`, `name: string`, `defaultSpendingClass: string`, `archived: bool` (`FirestoreCategoryRepository.kt:93-105`, `132-139`). | `id` must equal document ID. Used by Expenses and Recurring Expenses. | Whole-collection real-time listener; client-side list handling. |
| `users/{uid}/expenses/{expenseId}` | `id`, `userId`, `categoryId`, `categoryNameSnapshot`, `spendingClass`, `paymentChannel`, `originalAmountDecimal`, `originalCurrency`, `rateLkrPerUnitDecimal`, `baseAmountMinor: int64`, `rateSource`, `rateDate`, `occurredDate`, nullable `note`, nullable `channelLabel`, nullable `recurringExpenseId`, nullable `recurringOccurrenceId` (`FirestoreExpenseRepository.kt:122-140`). | `userId` is required to equal path UID by Rules. Recurring fields optionally link to a template and occurrence. | Whole-collection real-time listener, date filtering and sorting performed in Kotlin (`FirestoreExpenseRepository.kt:41-66`). |
| `users/{uid}/incomes/{incomeId}` | `schemaVersion: int64`, `id`, `source`, `entryKind`, `money: map`, `occurredDate`; optional `pnlDirection`, `projectReference`, `receivableId`, `assetSymbol`, `cryptoRealizedDetails: map`, `note` (`IncomeFirestoreMapper.kt:36-105`). `money` contains original amount/currency, LKR-per-unit rate, LKR minor units, rate source, and rate date. | `id` equals document ID. `receivableId`, if present, must reference an active same-owner Receivable with matching currency. | Date-range real-time query: `occurredDate >= start`, `< end`, ordered by `occurredDate` then document ID (`IncomeFirestoreObserver.kt:202-218`). |
| `users/{uid}/receivables/{receivableId}` | `schemaVersion`, `id`, `title`, `projectReference`, `expectedOriginalDecimal`, `currency`, `expectedDate`, `archived` (`ReceivableFirestoreMapper.kt:15-32`). | `id` equals document ID; currency cannot change after creation. Settlement is derived from linked Income documents, not stored as a mutable balance. | Ordered real-time listener by `expectedDate`, then document ID (`FirestoreReceivableRepository.kt:102-118`). |
| `users/{uid}/goals/{goalId}` | `schemaVersion`, `id`, `name`, `targetAmountMinor: int64`, `startDate`, `targetDate`, `archived` (`GoalFirestoreMapper.kt:29-60`). | Parent for immutable Contribution ledger. Goals are archived, not deleted. | Ordered by document ID real-time listener (`GoalFirestoreObserver.kt:170-179`). |
| `users/{uid}/goals/{goalId}/contributions/{contributionId}` | `schemaVersion`, `id`, `goalId`, `amountMinor: int64`, `kind`, `occurredDate`; optional `reversesId`, `note` (`GoalFirestoreMapper.kt:39-48`, `86-98`). | Create-only. Opening/allocation amounts are positive. Reversal amounts are negative and exactly negate one original entry. | Ordered by `occurredDate`, then document ID real-time listener (`GoalFirestoreObserver.kt:181-193`). |
| `users/{uid}/goals/{goalId}/reversalClaims/{originalId}` | `schemaVersion`, `originalContributionId`, `reversalContributionId` (`FirestoreGoalRepository.kt:269-280`). | One create-only claim per original contribution prevents a duplicate reversal. Written atomically with the reversal contribution. | Client may get one claim; cannot list. No normal UI listener found. |
| `users/{uid}/recurringExpenses/{recurringId}` | `id`, `userId`, `title`, `categoryId`, `expectedAmountMinor`, `frequency`, `intervalCount`, `anchorDate`, `startDate`, nullable `endDate`, `status`, `paymentChannel`, nullable `channelLabel`, `reminderEnabled`, `reminderLeadDays`, `reminderTimeMinutes` (`FirestoreRecurringRepository.kt:243-260`). | Requires an existing same-owner Category. Templates are paused/cancelled rather than deleted. | Whole-collection real-time listener; sorted by title in Kotlin. |
| `users/{uid}/recurringOccurrences/{occurrenceId}` | `id`, `userId`, `recurringExpenseId`, `dueDate`, `status`, nullable `expenseId`, `actionAt: server timestamp` (`FirestoreRecurringRepository.kt:156-166`, `196-206`). | ID is `{recurringId}_{dueDate}`. Only `CONFIRMED` or `SKIPPED` is stored; pending dates are derived. Confirmed creation must be atomic with a linked Expense. | Whole-collection real-time listener; sorted by due date in Kotlin. |
| `users/{uid}/fcmRegistrations/{registrationId}` | `schemaVersion`, `userId`, `installationId`, `platform`, `updatedAt: server timestamp` (`PushTokenRepository.kt:110-130`). | Document ID is the SHA-256 of the identifier supplied by the messaging/installation flow. Client reads/lists are denied; a trusted backend would require Admin SDK access. | No client read query. |

### 3.3 Multi-currency and crypto representation

- Supported currencies are `LKR` and `USD` (`core/model/Money.kt:5-8`).
- Money snapshots preserve `originalAmountDecimal`, `originalCurrency`, `rateLkrPerUnitDecimal`, converted `baseAmountMinor`, `rateSource`, and `rateDate` (`core/model/Money.kt:17-24`). This supports historical-rate preservation rather than recalculating old entries with a current rate.
- Rate sources are `LKR_FIXED`, `MANUAL_TRANSFER`, `MANUAL_REFERENCE`, and `EXCHANGE_RATE_API_REFERENCE` (`core/model/Money.kt:10-15`).
- Income stores Money as a nested map; Expense stores the same concepts as top-level fields.
- Crypto gains/losses store an absolute result magnitude plus `pnlDirection`. Structured records additionally retain proceeds, cost basis, and fees. The application derives a signed calculation for presentation, but the persisted `baseAmountMinor` is non-negative.
- Receivables store their expected amount in original currency. Paid/settled amounts are derived from linked Income records; no confirmed bank balance is stored or verified.

### 3.4 Index audit

`firestore.indexes.json` declares exactly two collection-scope manual indexes, both for `expenses`:

1. `occurredDate ASC, id ASC` (`firestore.indexes.json:3-16`).
2. `categoryId ASC, occurredDate DESC` (`firestore.indexes.json:17-30`).

Neither index matches a current repository query because the Expense repository listens to the complete owner collection and filters/sorts in Kotlin (`FirestoreExpenseRepository.kt:41-66`). They are therefore unused by the inspected implementation.

The current Income, Receivable, and Contribution queries each order/range on one application field and then document ID in the same ascending direction. Firestore automatically indexes ordinary fields and adds `__name__` ordering in the direction of the final indexed field. No missing manual index was identified from static query inspection. If production returns an index-required error, use the exact generated link and add only the required definition after review. The local emulator passing a query does not prove a production index exists.

Official reference: [Cloud Firestore index overview](https://firebase.google.com/docs/firestore/query-data/index-overview).

## 4. Authentication and Ownership Findings

### 4.1 Firebase initialization and project selection

- The Google Services Gradle plugin is applied at project and app level (`build.gradle.kts:2-9`; `app/build.gradle.kts:1-8`).
- Firebase libraries use BoM `34.19.0` with Auth, Firestore, Messaging, and Installations (`gradle/libs.versions.toml:16-17`, `45-49`; `app/build.gradle.kts:71-76`).
- The default Firebase app is initialized through the normal Android Google Services/Firebase startup path. Production code uses `FirebaseAuth.getInstance()` and `FirebaseFirestore.getInstance()` (`AuthModule.kt:39-44`; `ExpenseModule.kt:28-37`).
- The shared configuration and the inspected debug APK point to Firebase project `se3092-personal-finance` and package `com.example.personalfinanceapp`. No API key, OAuth client identifier, token, or credential is reproduced in this audit.
- The existing debug APK contains the generated web-client resource required by the Google credential flow. The flow reads `default_web_client_id` and exchanges the returned ID token with Firebase Auth (`GoogleCredentialSignIn.kt:23-54`; `FirebaseAuthRepository.kt:81-109`). Live Google-provider enablement and SHA certificate registration remain unverified.
- No product flavors exist. Debug and release share the same `applicationId` and `google-services.json`. The only debug manifest changes are local-network permission, cleartext traffic, and a test activity (`app/src/debug/AndroidManifest.xml:1-10`).
- Calls to `useEmulator` occur only under `app/src/androidTest`; none occur in `app/src/main`. A build from the checked-in main source therefore targets live Firebase services unless altered externally.

### 4.2 Authentication flows

- Email/password login and registration use Firebase Auth (`FirebaseAuthRepository.kt:41-79`).
- Google sign-in uses Credential Manager and Firebase `GoogleAuthProvider` (`GoogleCredentialSignIn.kt:23-75`; `FirebaseAuthRepository.kt:81-109`).
- Password reset uses Firebase Auth (`FirebaseAuthRepository.kt:204-214`).
- Registration stores the display name in Firebase Auth (`FirebaseAuthRepository.kt:111-132`).
- Password users who are unverified are routed to a verification-only screen; verified or non-password-provider users enter the authenticated application (`AuthRepository.kt:6-15`; `AuthViewModel.kt:1083-1099`; `AppNavHost.kt:204-230`).
- Verification email resend and “check status” are implemented. A successful check reloads the user and forces an ID-token refresh before reconciling navigation (`AuthViewModel.kt:515-590`; `FirebaseAuthRepository.kt:159-201`).

### 4.3 Server enforcement versus client enforcement

Client navigation improves user experience but is not the security boundary. The Firestore Rules token claim blocks unverified users even if they bypass navigation. Conversely, client-side validators such as `IncomeValidator` and `ExpenseFirestoreMapper` do not protect Firestore from a modified client unless the same constraints exist in Rules. This is the source of Findings F-01 and F-04.

### 4.4 Cross-account and account-switching behavior

- Every production repository constructs paths from the current Firebase UID.
- The application cancels the previous account's listener and emits an empty value before observing the next account (`AccountScopedData.kt:13-33`; equivalent explicit logic exists in Income and Goal observers).
- Sign-out attempts to delete the owner-scoped FCM registration, clears credential-provider state, and still signs out if advisory cleanup fails (`AuthSessionCleanup.kt:17-35`; `FirebaseAuthFlows.kt:19-33`).
- Firestore's persistent cache is **not** cleared on production sign-out. `clearPersistence()` occurs only in Android emulator tests. Previous-account records can remain encrypted/sandboxed in app storage and in Firestore's local cache, although current UI flows are cleared and new queries use the new UID path.
- Pending writes are not explicitly cancelled at sign-out. They retain their original document path and may wait for a suitable authenticated/network state. The existing Android instrumentation test is intended to verify that an Account A pending write is not redirected to Account B, but it was not executed in this audit because no Android emulator/device was connected.

### 4.5 Credential exposure review

- `app/google-services.json` is tracked. Firebase documents this client configuration as containing unique but non-secret project identifiers; it is not a service-account credential. See [Understand Firebase for Android](https://firebase.google.com/docs/android/learn-more).
- No service-account JSON fields, private-key blocks, `FIREBASE_TOKEN`, or Application Default Credential path was found outside ignored dependencies/build outputs.
- API keys and client identifiers were inspected only for configuration consistency and are intentionally omitted here.
- Console verification should confirm API-key Android application restrictions and SHA certificate restrictions. No such restriction can be proven from this repository.

## 5. Security Test Results

### 5.1 Executed suite

Successful command: repository script `npm.cmd run test:rules`, with the Firebase CLI config store redirected to a task-specific temporary directory because the sandbox cannot access the user-level config file.

The script used:

```text
firebase emulators:exec --config firebase.json \
  --project demo-personal-finance-income-rules --only firestore \
  "node --test --test-concurrency=1 test/firestore.rules.test.js"
```

The test code loaded the checked-in `firestore.rules` directly and used `@firebase/rules-unit-testing` authenticated contexts with explicit `email_verified` claims (`test/firestore.rules.test.js:27-79`).

Results on 10 November 2026:

- Test cases: **29**
- Passed: **29**
- Failed: **0**
- Skipped/cancelled/todo: **0**
- Allowed-operation assertions: **72**
- Denied-operation assertions: **124**
- Total authorization assertions: **196**
- Node-reported duration: **6.652 seconds**
- Firebase project mode: demo project, local Firestore emulator on `127.0.0.1:8080`

Two earlier launch attempts did not execute tests: PowerShell blocked `npm.ps1`, then the Firebase CLI could not access its user config store. These were environment launch failures, not test failures. The successful run used `npm.cmd` and temporary CLI config.

Covered scenarios include anonymous Income denial; unverified-user restrictions; owner and cross-owner Income access; exact Income/Money schema; currency/rate combinations; crypto shapes; Receivable ownership/link rules; owner update validation; the real Income observer query; basic Expense ownership; FCM ownership/write-only behavior; Goals and nested Contribution isolation; immutable contribution reversals; Recurring Expense/Occurrence creation; atomic confirmed occurrence creation; and fallback denial.

### 5.2 Important missing rules tests

The passing suite does not cover every required risk. Add tests after this read-only audit for:

1. Expense exact field set, missing fields, `id == expenseId`, positive decimal amount, positive and consistent minor units, currency/rate source/date combinations, Category existence, unknown fields, and supported optional-field types.
2. Expense owner-field forgery on update, not only cross-account path denial.
3. Mutation or deletion of an Expense linked to a confirmed recurring occurrence.
4. Every UI-exposed `PaymentChannel`, especially `TRANSFER`.
5. Anonymous and unverified read/update/delete across every collection, not only representative creates/reads.
6. Cross-account read/create/update/delete for Categories, Receivables, Recurring Occurrences, and Reversal Claims.
7. Recurring template forged `userId`, missing/archived Category, invalid calendar dates, end date before start date, and arbitrary occurrence dates not produced by the schedule.
8. Goal/Contribution schema-version type, note length, invalid calendar dates, cross-account reversal claims, and orphan claim attempts.
9. Structured crypto arithmetic mismatch and direction mismatch, with an explicit documented decision if Rules cannot enforce the calculation.
10. Parent `/users/{uid}` anonymous/cross-owner denial and a schema test if the document becomes used.
11. FCM `updatedAt` not using a server timestamp and registration-document abuse limits/identifier consistency.

### 5.3 Existing Android emulator tests not executed

`FirestoreOfflineBehaviorEmulatorTest.kt` contains three relevant tests:

- `onlineRecordsRemainCorrectInCacheOfflineAndServerErrorsAreNotEmptyData`
- `offlineCreatesStayPendingThenSynchronizeOnceWithoutChangingAmounts`
- `pendingOfflineWriteCannotMoveFromSignedOutAccountAToAccountB`

They require a running Android Emulator plus local Auth and Firestore emulators (`FirestoreOfflineBehaviorEmulatorTest.kt:49-75`, `284-307`). `adb devices -l` reported no connected device, so these tests were **NOT EXECUTED** and no pass claim is made. Other repository Android emulator tests for Income, Goal, and Recurring repositories were likewise not run.

### 5.4 What the emulator result does not prove

The result proves only that the checked-in rules behaved as asserted in the covered local scenarios. Firebase explicitly positions the Local Emulator Suite as a Rules testing tool; it does not establish that the same source is deployed. See [Test Cloud Firestore Security Rules](https://firebase.google.com/docs/firestore/security/test-rules-emulator) and [Manage and deploy Firebase Security Rules](https://firebase.google.com/docs/rules/manage-deploy).

## 6. Technical Report Corrections

Direct comparison is unavailable, so the table does **not** pretend that these statements currently appear on a particular page. Each row gives exact, verified replacement text suitable for the relevant report section. Replace only a conflicting or overbroad statement; preserve any existing correct explanation.

| Report Page | Existing Statement | Evidence | Status | Exact Correction |
|---|---|---|---|---|
| N/A — report unavailable | Unavailable for inspection | `app/google-services.json`; Gradle configuration; inspected debug APK | **UNVERIFIED** | “The Android client is configured for Firebase project `se3092-personal-finance` and package `com.example.personalfinanceapp`. The checked-in main source uses the default live Firebase instances and contains no production emulator routing. The final submitted APK and live Console project settings must still be verified separately.” |
| N/A | Unavailable for inspection | `FirebaseAuthRepository.kt:41-214`; `GoogleCredentialSignIn.kt:23-80` | **UNVERIFIED** | “Authentication supports Firebase email/password sign-in and registration, password reset, and Google credential sign-in. Provider enablement, authorized domains, and signing-certificate fingerprints are Console configuration and were not proven from source alone.” |
| N/A | Unavailable for inspection | `firestore.rules:10-15`, `438-539` | **UNVERIFIED** | “Firestore Rules require an authenticated UID matching the `/users/{uid}` path and a token with `email_verified == true` for all financial subcollections. The parent user document remains owner-only without verification for bootstrap purposes, and FCM registration deletion remains owner-only without verification for sign-out cleanup.” |
| N/A | Unavailable for inspection | `AuthViewModel.kt:515-590`; `FirebaseAuthRepository.kt:159-201` | **UNVERIFIED** | “The client routes unverified password users to an email-verification screen. After the user follows the email link, the app reloads the Firebase user and force-refreshes the ID token so that Firestore can evaluate the updated `email_verified` claim. This UI gate supplements, but does not replace, server-side Rules.” |
| N/A | Unavailable for inspection | Actual collection inventory in Section 3 | **UNVERIFIED** | “Financial data is stored under the authenticated UID in `categories`, `expenses`, `incomes`, `receivables`, `recurringExpenses`, `recurringOccurrences`, and `goals`. Goal documents contain nested `contributions` and `reversalClaims`. Push registrations use `fcmRegistrations`; no other FCM collection name is used by the application.” |
| N/A | Unavailable for inspection | `Money.kt:17-24`; Income and Expense mappers | **UNVERIFIED** | “Each money snapshot preserves the original decimal amount and currency, the LKR-per-unit exchange rate, the applied rate source and date, and the converted LKR minor-unit amount. Historical records are not recalculated using the current exchange rate.” |
| N/A | Unavailable for inspection | `CryptoRealizedCalculator.kt:35-50`; `IncomeValidator.kt:98-118` | **UNVERIFIED** | “A realised cryptocurrency result stores a non-negative magnitude in Money and records the sign separately as `GAIN`, `LOSS`, or `BREAK_EVEN`. Structured records retain sale proceeds, cost basis, and fees. The Kotlin client validates the arithmetic; the present Firestore Rules validate the shape but do not recompute the result.” |
| N/A | Unavailable for inspection | Repository listeners listed in Section 3 | **UNVERIFIED** | “The dashboard and feature screens consume Firestore snapshot listeners. Income uses a server-side date-range query; Receivables and Contributions use ordered queries; Expenses, Categories, Recurring Expenses, and Occurrences listen to owner collections and perform some filtering or sorting in Kotlin.” |
| N/A | Unavailable for inspection | `ExpenseModule.kt:28-40`; official Firestore offline documentation | **UNVERIFIED** | “The Android app uses a 100 MiB persistent Firestore cache. Cached documents can be read while offline, snapshot listeners can emit cached data, and non-transaction writes can remain pending until connectivity returns. This is partial offline support, not a complete offline-first transaction system.” |
| N/A | Unavailable for inspection | Transaction calls across Income, Goal, Receivable, and Recurring repositories | **UNVERIFIED** | “Firestore transactions require network connectivity. Income deletion, goal archiving and contribution/reversal writes, receivable save/archive operations, and recurring status/confirm/skip operations are not guaranteed to complete offline.” |
| N/A | Unavailable for inspection | `AppDatabase.kt:6-8`; `DatabaseModule.kt:20-25` | **UNVERIFIED** | “Room stores only local reminder scheduling records. Core financial records are stored in Cloud Firestore and may be present in Firestore’s own local cache; they are not mirrored into Room.” |
| N/A | Unavailable for inspection | `AccountScopedData.kt:13-33`; no production `clearPersistence()` | **UNVERIFIED** | “On account change, observable UI state is cleared before a listener for the new UID is attached. Firestore’s persistent cache is not cleared on sign-out, so the design relies on UID-scoped paths, Rules, listener cancellation, and UI clearing rather than deleting the local cache.” |
| N/A | Unavailable for inspection | `PushTokenRepository.kt:110-130`; `firestore.rules:442-450` | **UNVERIFIED** | “Push registrations are stored at `users/{uid}/fcmRegistrations/{registrationId}`. Clients may create or update their own verified registration and may delete it during sign-out, but cannot read or list registrations. End-to-end push sending requires a trusted backend/Admin SDK and is not proven by the Android repository alone.” |
| N/A | Unavailable for inspection | `firestore.indexes.json`; query inspection | **UNVERIFIED** | “The repository declares two manual Expense indexes, but the current Expense repository does not issue matching server queries. Current Income, Receivable, and Contribution query shapes appear supportable by automatic field indexes plus document-name ordering. Deployed index state remains unverified.” |
| N/A | Unavailable for inspection | Local test result in Section 5 | **UNVERIFIED** | “The checked-in Firestore Rules passed 29 local emulator tests containing 196 authorization assertions (72 allowed and 124 denied). These tests validate only the checked-in Rules and covered scenarios; they do not prove production deployment and do not cover all Expense integrity constraints.” |
| N/A | Unavailable for inspection | No production rules export or Console access | **UNVERIFIED** | “The production Firestore Rules deployment was not verified. The published Rules must be compared directly in Firebase Console with the checked-in `firestore.rules` before submission; no deployment should be inferred from local tests.” |

### Diagram corrections to apply if needed

- The architecture diagram should show Firebase Auth as the source of UID and verification state.
- Every Firestore feature repository should point to a UID-scoped child collection, not to top-level shared financial collections.
- Room should connect only to reminder scheduling, not to Income, Expense, Goal, Receivable, Category, or recurring financial storage.
- Firestore's persistent local cache should be shown as SDK-managed caching, not as a Room database.
- FCM should show `fcmRegistrations` under the user and a separate trusted sender/backend. The Android client alone is not the sender.
- Exchange-rate retrieval is separate from the persisted historical Money snapshot. The snapshot contains the applied rate/date/source.

## 7. Missing Technical Report Content

Because the report is unavailable, absence cannot be confirmed. The following content must be present before claiming the Firebase section is complete:

1. The exact Firebase project and Android package, with secrets omitted and the final APK separately verified.
2. A clear distinction between client navigation checks and server-side `email_verified` enforcement.
3. The complete UID-scoped schema, including `fcmRegistrations`, nested Contributions, Reversal Claims, Recurring Occurrences, and Receivables.
4. The Expense Rules limitation and the required exact-schema remediation; do not claim full financial validation in the current state.
5. The recurring confirmed-occurrence/Expense atomic creation design and the current post-creation link-integrity gap.
6. The actual cryptocurrency representation: magnitude plus direction, with client-only arithmetic verification.
7. The complete historical Money snapshot fields and the fact that only LKR and USD are supported.
8. The actual server query patterns and the fact that the two declared Expense indexes are currently unused.
9. Offline limits: persistent cache, pending non-transaction writes, transactions failing offline, and no production cache clearing on sign-out.
10. The exact Room responsibility: reminder scheduling only.
11. FCM client registration security, the trusted-backend requirement, and the exact `fcmRegistrations` path.
12. The exact emulator test result and missing coverage, without equating local tests with production deployment.
13. A production verification record: Console project, Authentication providers, published Rules comparison, deployed indexes, and final APK project identifier.
14. A statement that no confirmed bank balance or bank-account integration exists; Receivable settlement is derived from user-entered linked Income.
15. An explanation of concurrent writes: plain document writes use Firestore synchronization behavior, while application transactions retry online; no custom merge/version-conflict UI is implemented.

## 8. Offline Caching and Synchronization Findings

### Confirmed behavior

- Firestore uses explicit persistent disk cache with a 100 MiB target (`ExpenseModule.kt:28-40`). Android also enables offline persistence by default, but this project makes the cache choice explicit.
- Snapshot listeners provide real-time updates and can emit cached snapshots.
- Direct `set` operations for Income, Expense, Goal, and Recurring Expense can enter the local pending-write queue. Their returned Tasks do not necessarily complete while offline.
- `updatedAt` for FCM registrations and `actionAt` for recurring occurrences use server timestamps (`PushTokenRepository.kt:110-116`; `FirestoreRecurringRepository.kt:156-166`, `196-206`). Transaction dates and rate dates are client-created ISO strings.

### Limitations

- Firestore transactions fail while offline. This affects Income deletion; Goal archiving and Contributions/Reversals; Receivable create/update/archive; Recurring status, confirmation, and skip operations. See [Transactions and batched writes](https://firebase.google.com/docs/firestore/manage-data/transactions).
- Category creation/update performs prerequisite collection reads, so cold-cache offline completion is not guaranteed.
- A listener showing cached data is not proof that the server accepted pending writes. The production UI does not consistently expose `hasPendingWrites` metadata.
- The local cache is not cleared at sign-out. UI-level account scoping is implemented, but device-level cache retention and Android backup policy require a separate privacy review.
- `android:allowBackup="true"` is enabled and the backup/data-extraction files remain templates without explicit exclusions (`AndroidManifest.xml:9-18`; `backup_rules.xml:1-13`; `data_extraction_rules.xml:1-18`). Whether Firestore/Room/preferences files are actually included must be verified on a device and tightened if the assignment requires local financial-data minimization.
- Firestore documents its synchronization behavior as last-write-wins for multiple offline changes to the same document. The application does not add version fields, server-time preconditions, or a conflict-resolution UI, so the report should not imply a deterministic business conflict policy beyond the SDK behavior. See [Access data offline](https://firebase.google.com/docs/firestore/manage-data/enable-offline).

## 9. Manual Firebase Console Checks

Perform these checks without editing or publishing anything:

1. **Confirm the project.** Open Firebase Console and select `se3092-personal-finance`. In Project settings > General, confirm an Android app with package `com.example.personalfinanceapp` exists.
2. **Confirm Authentication providers.** Open Build > Authentication > Sign-in method. Verify Email/Password and Google are enabled. Check authorized domains and support email. Do not expose OAuth credentials in screenshots or the report.
3. **Confirm signing certificates.** In Project settings > Android app, verify the SHA-1 and SHA-256 fingerprints for the actual submission signing key. Google sign-in may fail even when the source is correct if these are missing.
4. **Compare published Rules.** Open Build > Firestore Database > Rules. Copy the currently published text to a temporary local file and run a read-only comparison such as `git diff --no-index -- firestore.rules published-firestore.rules`. Confirm every line, not only the visible collection matches. Do **not** click Publish during verification.
5. **Record deployment evidence.** Capture the project name, Rules release/publish timestamp, and exact comparison result. Local test output is supporting evidence, not deployment evidence.
6. **Compare indexes.** Open Firestore Database > Indexes. Compare deployed manual indexes and field exemptions with `firestore.indexes.json`. Record build status for each index. Do not claim the two Expense indexes are used by current code.
7. **Verify data paths.** In Firestore Data, inspect only your own test account and confirm the exact child collections listed in Section 3, especially `fcmRegistrations` rather than an older token collection name.
8. **Verify Auth claims.** With a disposable email/password account, confirm financial reads/writes fail before verification and succeed only after verification plus token refresh/re-login. Confirm profile/bootstrap and sign-out cleanup still work.
9. **Verify final APK.** Inspect the exact APK that will be submitted, not merely the existing debug APK. Confirm package name and the generated `project_id` resource with Android build tools. Do not print or submit API-key values. Also confirm the APK is not an instrumentation APK and does not contain test-only emulator routing.
10. **Run device emulator tests.** Start local Auth and Firestore emulators, connect an Android Emulator, then execute the existing Income, Goal, Recurring, and Offline instrumentation tests. Preserve exact pass/fail output.
11. **Verify FCM end-to-end separately.** Confirm a registration appears under the correct UID after verification, disappears or becomes unusable after sign-out/disable, and can be used by the trusted sender. The repository contains no trusted sender implementation.
12. **Review API-key restrictions.** In Google Cloud Console > APIs & Services > Credentials, verify the Android API key is restricted to the intended package and signing certificate and only the required APIs. Do not paste the key into the report.

## 10. Final Assignment Compliance Review

The official SE3092 rubric was unavailable, so this section uses only the Firebase focus areas supplied in the task and does not assign or predict a mark.

### Implementation defects that could reduce marks

- Incomplete Expense Rules validation (Finding F-01).
- Confirmed recurring occurrence can be detached from its Expense after creation (Finding F-02).
- Recurring UI exposes `TRANSFER`, which Rules reject (Finding F-03).
- Structured crypto arithmetic is not server-validated; this must be disclosed even if accepted as a Rules-language limitation.
- Production Rules, indexes, Auth providers, and final APK configuration are not evidenced.
- Android persistent-cache/sign-out/backup privacy is not fully documented or device-verified.

### Documentation defects that could reduce marks

- Claiming deployed Rules or indexes based only on repository files or emulator tests.
- Claiming complete offline-first support or offline Firestore transactions.
- Claiming that Room stores financial records.
- Using an outdated FCM collection name.
- Calling crypto loss amounts signed when sign is actually stored separately.
- Claiming all Expense fields are validated by Rules.
- Claiming confirmed bank balances or external bank verification.
- Omitting the exact historical exchange-rate snapshot fields.
- Omitting the distinction between automatic indexes and the two unused declared Expense indexes.
- Presenting UI email verification as the sole enforcement mechanism.

### Evidence that supports assignment readiness after remediation

- UID-scoped model and default-deny fallback.
- Server-side verified-email claim for financial subcollections.
- Strong Income, Receivable, Goal, Contribution, Reversal Claim, and Recurring Occurrence shapes.
- Immutable contribution ledger and atomic reversal claim.
- Atomic initial recurring confirmation.
- Historical multi-currency snapshots.
- Real-time listeners and explicit persistent cache.
- A substantial local Rules suite that currently passes 29/29 tests.

## 11. Prioritized Action Plan

No action below was performed during this investigation.

### P0 — Security vulnerabilities and assignment-critical defects

1. Replace the partial Expense validator with an exact schema and server-verifiable money/currency/rate/date constraints; require `id == expenseId` and an existing Category.
2. Prevent update/delete of an Expense linked to a confirmed recurring occurrence, or implement an atomic correction/reversal model.
3. Resolve the `PaymentChannel.TRANSFER` mismatch between Kotlin/UI and Rules.
4. Add rules tests that demonstrate all three fixes and all valid application operations.
5. Re-run the complete local Rules suite and Android emulator integration tests.
6. Only after review, deploy deliberately and verify the published Rules. Do not overwrite Console Rules without first comparing them.

### P1 — Incorrect technical documentation

1. Apply the exact replacement text in Section 6 wherever the report is broader or contradictory.
2. Update Firestore/MVVM/FCM/Room diagrams to the actual paths and responsibilities.
3. State the Expense and recurring-link limitations until fixed and verified.
4. State that production Rules and indexes are unverified until Console evidence is recorded.

### P2 — Missing verification or test coverage

1. Execute the existing Android emulator tests with a connected emulator and local Auth/Firestore emulators.
2. Verify Auth providers, signing fingerprints, Rules release, indexes, API restrictions, and final APK in Console/tooling.
3. Add the missing rules cases listed in Section 5.2.
4. Test account switching with cached data and pending writes across process restart, not only within one running test.
5. Review Android backup contents and local-cache privacy on a physical or emulator device.

### P3 — Optional improvements

1. Remove unused Expense composite indexes if production query plans confirm they are unnecessary, reducing index maintenance and avoiding misleading documentation.
2. Add pending-write/offline state indicators to relevant screens.
3. Add bounded lengths for notes and labels and explicit integer checks for every schema version.
4. Add App Check and abuse controls if the application moves beyond coursework; App Check supplements but does not replace Auth and Rules.
5. Store a sanitized submission evidence bundle containing test summaries, Rules diff result, index list, and final APK identifiers—never keys or tokens.

## Final verdict

FinClarity's checked-in Firebase implementation has strong account isolation and a sound overall UID-scoped architecture, but the Firestore Rules are not yet complete enough for an unqualified security or assignment-readiness claim. The highest-priority issues are Expense integrity, recurring-confirmation link durability, and the `TRANSFER` enum mismatch. Production deployment and the v1.4 report remain unverified because neither Console state nor the document was available.
