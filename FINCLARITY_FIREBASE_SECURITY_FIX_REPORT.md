# FinClarity Firebase Security Fix Report

Date: 2026-11-10  
Scope: local source, Firebase Emulator, Android JVM unit tests, and debug APK build  
Production status: **not deployed and not verified**

## Executive result

The confirmed Expense validation, recurring-occurrence integrity, and
`PaymentChannel.TRANSFER` mismatches from
`FINCLARITY_FIREBASE_SECURITY_AND_REPORT_AUDIT.md` have been corrected locally.

- The complete Firebase Emulator rules suite passes: **31/31 tests**, with
  **85 allowed + 153 denied = 238 rule assertions**.
- `\.\gradlew.bat testDebugUnitTest` passes: **591 tests in 53 suites**, with
  **0 failures, 0 errors, and 0 skipped**.
- `\.\gradlew.bat assembleDebug` passes and produces a debug APK.
- No device or Android instrumentation tests were performed.
- No rules were deployed, no live Firebase resource was changed, no migration
  was changed, no production data was modified, and no commit or push was made.

## 1. Confirmed root causes

### Expense validation

The old `isValidExpenseData` accepted only a small subset of the actual Expense
contract. It did not require an exact field set or matching document ID, and it
did not validate the stored currency/rate snapshot, positive decimal shape,
rate source, rate date, category existence, or most optional-field types. It
also allowed extra fields. Because Firestore evaluates partial updates against
the complete resulting `request.resource.data`, the missing complete-document
validation affected both full writes and partial updates.

### Confirmed recurring occurrence integrity

Recurring occurrence documents were immutable and a confirmed occurrence had
to point to a linked Expense when created. However, the Expense match granted
every verified owner unconditional delete access and allowed updates that could
remove or forge the two recurring linkage fields. That left an immutable
confirmed occurrence pointing to a missing or detached financial record.

The Expense editor also reconstructed edited Expense objects without copying
`recurringExpenseId` and `recurringOccurrenceId`, so an ordinary edit of a
confirmed recurring payment would have attempted to detach it.

### `PaymentChannel.TRANSFER`

Kotlin defines `CASH`, `CARD`, `BANK`, `TRANSFER`, and `OTHER`. The recurring
commitment editor exposes all enum entries, the recurring repository persists
the enum name, and activity presentation handles `TRANSFER`. It is therefore a
genuinely supported payment classification. The rules allowed the other four
values but omitted `TRANSFER`, causing valid recurring templates and confirmed
expenses to be rejected. This classification does not enable or imply transfer
reconciliation.

## 2. Exact files changed

| File | Change |
|---|---|
| `firestore.rules` | Added exact Expense schema/type/value validation, category and recurring-template checks, immutable recurring linkage, safe delete policy, and `TRANSFER` in both Expense and recurring-template channel allowlists. |
| `test/firestore.rules.test.js` | Expanded the Emulator suite with Expense schema, ownership, currency/rate, category, partial-update, payment-channel, and recurring-integrity positive/negative cases. |
| `app/src/main/java/com/example/personalfinanceapp/feature/expense/ui/ExpenseViewModel.kt` | Preserves recurring linkage on edits and blocks confirmed-recurring deletion in the UI with an explicit message. |
| `app/src/test/java/com/example/personalfinanceapp/feature/expense/ExpenseViewModelTest.kt` | Added regressions for preserving recurring links during edits and rejecting linked deletion before a repository write. |
| `FINCLARITY_FIREBASE_SECURITY_FIX_REPORT.md` | This report. |

No Expense model, Firestore mapper, repository, migration, Firebase project
configuration, or index file required a change. Those paths were inspected to
derive and verify the stored contract.

## 3. Before/after Security Rules behavior

| Operation | Before | After |
|---|---|---|
| Create an Expense with missing or extra fields | Often accepted | Denied; the 17-field repository shape is exact. |
| Use a document `id` different from the path ID | Accepted | Denied. |
| Forge `userId` | Denied only through the limited owner equality check | Denied with explicit string/type/path-owner validation; updates are also covered. |
| Use a missing category | Accepted | Denied; the category must exist under the same user. |
| Store zero, malformed, or incorrectly typed amounts | Zero base amount and weak decimal values could pass | Denied; original amount/rate must be positive decimal strings and base minor units must be a positive integer. |
| Store unsupported currency/rate-source combinations | Accepted | Denied; LKR requires `LKR_FIXED`, a precision-safe unit rate, and matching transaction/rate dates; USD requires a supported manual/API source and positive rate. |
| Use malformed date text | Any string accepted for Expense dates | Denied unless it matches the existing ISO `YYYY-MM-DD` rule contract. |
| Partial update that creates an invalid resulting document | Could pass limited checks | Denied because the complete resulting document must pass the exact contract. |
| Use `TRANSFER` | Denied | Accepted for Expense and recurring templates, matching Kotlin. |
| Attach only one recurring linkage field | Accepted | Denied; both fields must be null or both must be non-empty strings. |
| Forge/change/remove linkage on an existing linked Expense | Accepted | Denied; both linkage fields are immutable after creation. |
| Edit non-link fields on a confirmed recurring Expense | Accepted, but the app editor accidentally dropped links | Accepted; the app now preserves the links and rules allow the valid edit. |
| Delete an ordinary Expense | Accepted | Still accepted for the verified owner. |
| Delete an Expense linked to a confirmed occurrence | Accepted, leaving a dangling occurrence | Denied. The app now explains that the record may be edited but not deleted. |
| Create a confirmed occurrence without its deterministic linked Expense | Denied | Still denied. |
| Create a consistent linked Expense and confirmed occurrence in the app transaction | Accepted | Still accepted and now covered by stronger tests. |

Reads and all financial writes still require an authenticated owner with a
fresh token containing `email_verified == true`. Cross-user access remains
denied.

## 4. New positive and negative security tests

### Positive cases

- Exact app-generated LKR Expense creation.
- Valid partial update of an ordinary Expense.
- All Kotlin payment channels: `CASH`, `CARD`, `BANK`, `TRANSFER`, and `OTHER`.
- Recurring template creation using `TRANSFER`.
- Atomic app workflow that creates a deterministic linked Expense and confirmed
  recurring occurrence.
- Edit of a confirmed-linked Expense while preserving its linkage, including a
  payment-channel change to `TRANSFER`.
- Ordinary Expense deletion.
- Existing verified-owner reads, queries, authentication restrictions, and
  user isolation.

### Negative cases

- Missing required Expense field and unexpected extra field.
- Forged owner and mismatched path/document ID.
- Forged ownership, ID, extra-field, and zero-base partial updates.
- Zero or incorrectly typed original amount.
- Zero base minor amount.
- Unsupported currency, invalid LKR rate, and invalid LKR rate source.
- Malformed date and blank category snapshot.
- Missing same-user category.
- One-sided recurring linkage.
- Invalid payment channels including `WIRE`, `BANK_TRANSFER`, lowercase
  `transfer`, and blank.
- Removing or changing either link on a confirmed recurring Expense.
- Deleting the linked Expense or immutable confirmed occurrence.
- Confirming an occurrence with an Expense linked to a different recurring
  template.
- Existing cross-user and unverified-email attempts.

## 5. Exact verification results

### Firebase Emulator rules

Command:

```powershell
npm.cmd run test:rules
```

Final result:

```text
tests 31
pass 31
fail 0
RULE_ASSERTIONS allowed=85 denied=153 total=238
Script exited successfully (code 0)
```

The Firebase CLI could not fetch its optional MOTD because external network
access was restricted. This was explicitly reported as non-fatal and did not
affect the local demo-project Firestore Emulator run.

### Android JVM unit tests

Required command:

```powershell
.\gradlew.bat testDebugUnitTest
```

The first sandboxed attempt exited 1 because the wrapper needed to download the
pinned Gradle 9.6.0 distribution and network access was denied. After download
permission was granted, the authoritative final run completed with:

```text
BUILD SUCCESSFUL in 11s
33 actionable tasks: 6 executed, 27 up-to-date
```

Parsed JUnit XML totals:

```text
Suites:   53
Tests:    591
Failures: 0
Errors:   0
Skipped:  0
```

### Debug APK

Required command:

```powershell
.\gradlew.bat assembleDebug
```

The first sandboxed attempt had the same Gradle distribution network denial.
The authoritative final run completed with:

```text
BUILD SUCCESSFUL in 1s
42 actionable tasks: 3 executed, 39 up-to-date
```

Artifact:

```text
app/build/outputs/apk/debug/app-debug.apk
Size: 21,540,602 bytes
SHA-256: B68BB725DEEFF3ED7CCC9FA9C14B24092E5D1F9387BE8F03F11FE018F0159180
```

No device, emulator-device, instrumentation, or production smoke tests were
performed.

## 6. Compatibility issues with existing Firestore documents

The new exact write contract matches every current write made by
`FirestoreExpenseRepository` and `FirestoreRecurringRepository`. Existing data
may need cleanup before deployment:

1. Legacy Expense documents missing any of `id`, `userId`, money snapshot
   fields, `note`, `channelLabel`, `recurringExpenseId`, or
   `recurringOccurrenceId` remain readable, but an update is denied until the
   complete current shape is written. Current repositories always write the
   nullable fields explicitly as null.
2. Existing Expenses whose category document no longer exists cannot be
   updated until the category reference is repaired. New writes also require a
   same-user category.
3. Existing zero/negative/malformed amounts, unsupported currencies/rate
   sources, invalid dates, extra fields, blank snapshots, or IDs not matching
   the document path cannot be updated under the new rules.
4. A legacy ordinary Expense with omitted recurring fields can still be
   deleted. Any document carrying a non-null recurring link cannot be deleted.
5. A linked Expense whose corresponding confirmed occurrence or recurring
   template is missing cannot be updated until the data is repaired.
6. `TRANSFER` documents that were already inserted by privileged tooling now
   match the application contract; future client writes using `TRANSFER` are
   accepted.

Offline ordinary Expense recording remains supported because the app writes
the full current shape to Firestore's local queue. On synchronization, the
referenced category must already exist (or its queued creation must reach the
server first), the user must still own the path, and the refreshed Auth token
must carry `email_verified == true`.

## 7. Remaining vulnerabilities and limitations

- Firestore Rules cannot reproduce Kotlin `BigDecimal` multiplication and
  half-up rounding from arbitrary decimal strings. The rules validate positive
  shapes, currencies, rates, and base minor-unit type, while the Android mapper
  still checks exact `original × rate × 100 == baseAmountMinor`. A malicious
  owner can therefore store a self-owned but arithmetically inconsistent
  snapshot that the app will reject when reading. A trusted backend or a
  server-verifiable integer money representation is required to close this.
- The ISO date helper is syntactic; values such as an impossible calendar day
  can match `YYYY-MM-DD`. A server timestamp or numeric date representation is
  needed for semantic calendar validation in rules.
- Rules can validate a recurring-tagged Expense against an existing recurring
  template and deterministic Expense ID, but cannot require both sides of the
  two-document write from both directions without a circular `getAfter`
  evaluation. The security boundary is on the confirmed occurrence: it cannot
  be created unless the linked Expense exists and matches, and after
  confirmation the link cannot be changed or deleted. A custom client could
  create an unconfirmed orphan recurring-tagged Expense; it cannot forge a
  confirmed occurrence without satisfying the reciprocal occurrence rule.
- There is no reversal/correction ledger for confirmed recurring payments.
  Deletion is intentionally denied. If product requirements need cancellation
  after confirmation, add a first-class immutable reversal document and a
  corresponding UI flow rather than re-enabling deletion.
- The broader parent `/users/{userId}` document still permits arbitrary
  owner-written fields, as documented in the audit. It was outside this fix's
  financial subcollection scope.
- Emulator success proves the checked-in rules, not the rules currently
  deployed to production. App Check, production IAM/Admin SDK behavior, live
  Auth provider configuration, and production data compatibility were not
  verified here.

## 8. Existing workflow support in the updated APK

- **Ordinary Expense create/edit/delete:** supported.
- **Offline ordinary Expense recording:** supported, subject to the existing
  category and verified-token synchronization requirements above.
- **Confirmed recurring payment creation:** supported by the existing Firestore
  transaction and proven by the Emulator test.
- **Editing a confirmed recurring Expense:** supported; financial/display
  fields may change while its recurring linkage remains intact.
- **Deleting a confirmed recurring Expense:** intentionally no longer
  supported because it corrupts the immutable occurrence history. The APK
  blocks the request before the repository call and shows an explicit message.
- **Recurring template creation/status updates:** supported, including
  `TRANSFER`; templates and occurrence records remain non-deletable.
- **User isolation and verified-email restrictions:** retained.
- **Income, receivable, savings goal, contribution, FCM, and unrelated tested
  rule workflows:** retained by the complete passing rules and Android unit
  suites.

The updated debug APK compiles, but runtime behavior on a physical/virtual
Android device was not tested.

## 9. Required manual Firebase Console checks before deployment

Perform these checks in the intended production Firebase project. Do not rely
on the local demo-project Emulator result as proof of production state.

1. **Confirm the target project.** In Firebase Console > Project settings >
   General, verify the project ID and Android package/application ID match the
   checked-in `app/google-services.json` and the intended release environment.
2. **Back up the deployed rules.** Open Firestore Database > Rules, copy the
   complete currently deployed rules and note their publish timestamp/version.
   Compare that copy against local `firestore.rules` so unrelated production
   rules are not overwritten.
3. **Confirm Authentication configuration.** Open Authentication > Sign-in
   method and verify Email/Password is enabled. In Authentication > Users,
   inspect test accounts for the expected email-verification state. Sign out
   and sign back in (or force token refresh) before testing a newly verified
   account so the ID token contains the fresh claim.
4. **Audit every existing Expense shape before publishing.** In Firestore
   Database > Data, inspect `users/{uid}/expenses/{expenseId}` documents and
   identify any document that lacks the exact 17 fields listed in the rules,
   has extra fields, has `id != expenseId`, has `userId != uid`, uses wrong
   types, has non-positive/malformed money values, or uses unsupported
   currency/rate-source/date combinations. Use a read-only Admin SDK audit or
   Firestore export if the collection is too large for exhaustive Console
   inspection. Do not delete production data.
5. **Check category references.** For every Expense category ID, verify
   `users/{uid}/categories/{categoryId}` exists under the same UID. Archive
   status does not block an existing reference; missing documents do.
6. **Check recurring referential integrity.** For every confirmed
   `users/{uid}/recurringOccurrences/{occurrenceId}`, verify:
   - `id == occurrenceId`;
   - `expenseId == "recurring_" + occurrenceId`;
   - the referenced Expense exists under the same UID;
   - the Expense's `recurringOccurrenceId == occurrenceId`;
   - both documents have the same `recurringExpenseId`;
   - the recurring template exists.
7. **Check legacy linked Expenses.** Find Expenses with only one non-null
   recurring linkage field, mismatched/dangling links, or a non-deterministic
   linked Expense ID. Repair them through an approved, backed-up data-correction
   process before rules publication; do not weaken the rules to accept them.
8. **Check payment-channel data.** Confirm stored channel values are exactly
   `CASH`, `CARD`, `BANK`, `TRANSFER`, or `OTHER`. Confirm that `TRANSFER` is
   being used only as a payment classification; this change does not add bank
   transfer reconciliation.
9. **Validate in a staging Firebase project.** Deploy the candidate rules only
   to staging, run this same Emulator suite, then use a staging verified user
   to create/edit/delete an ordinary Expense, record one offline and reconnect,
   create/confirm a `TRANSFER` recurring payment, edit its linked Expense, and
   verify linked deletion is refused. Repeat access attempts with an unverified
   user and a different UID.
10. **Review the Rules diff before production publication.** In Firestore
    Database > Rules, paste the reviewed candidate and inspect the diff. Do not
    click Publish until the legacy-data audit and staging checks pass and an
    authorized reviewer approves the change.
11. **After an authorized deployment, verify the deployed artifact.** Reopen
    the Rules tab, confirm the publish timestamp and text match the reviewed
    local file, then repeat the owner/cross-owner, verified/unverified, ordinary
    Expense, and confirmed-recurring smoke tests against production-safe test
    accounts. Until that step is completed, production security is unverified.

## Final assessment

The checked-in rules now enforce the current Expense contract, protect
confirmed recurring-occurrence references against detachment and deletion, and
align payment channels with Kotlin. All requested local automated verification
passes. Production security must remain reported as **unverified** until the
compatibility audit, authorized deployment, and post-deployment checks above
are completed.
