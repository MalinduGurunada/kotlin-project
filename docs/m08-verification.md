# M08 verification - PBD-53, PBD-54, PBD-55

Verified on 2026-11-03 using an API 37 Pixel 8 Pro AVD and local Firebase Auth
(`127.0.0.1:9099`) and Firestore (`127.0.0.1:8080`) emulators. The tests use the
isolated project `demo-personal-finance-income-rules`; no production Firebase data was
used.

## Status and evidence

| Work item | Status | Evidence |
| --- | --- | --- |
| PBD-53 | **Automated PASS; manual checks remain** | Existing `FirestoreIncomeRepositoryEmulatorTest`: **5/5 PASS**. New `FirestoreOfflineBehaviorEmulatorTest`: cache/offline/server-error and pending-create scenarios **2/2 PASS**. |
| PBD-54 | **Automated PASS; process-restart scenario BLOCKED/manual** | Account A pending-write/account B isolation scenario **1/1 PASS**. B had neither a cached nor server document before or after A reauthentication. The test accepts pending-write failure, success, or continued pending state and does not require success after A reauthenticates. |
| PBD-55 | **PASS** | This reproducible verification record was added. |

Focused M08 total: **8/8 automated emulator tests passed** (the previously verified
5 income tests plus 3 offline-behaviour tests). The final affected-class run was **3/3
PASS**, 0 skipped and 0 failed. `connectedDebugAndroidTest` also compiled and packaged
the debug app and test APK successfully, so no separate debug build was needed.

Final focused command:

```powershell
.\gradlew app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.personalfinanceapp.feature.offline.FirestoreOfflineBehaviorEmulatorTest" --no-configuration-cache
```

## Verified behaviour

- Online income and expense values remain identical in the offline cache, including the
  calculated balance.
- A genuinely empty cache query is distinguishable from a server read that fails with
  `UNAVAILABLE` while offline.
- Offline income and expense creates stay pending locally, synchronize once after
  reconnect, remain idempotent on retry, and preserve their minor-unit amounts.
- A write queued under account A keeps A's document path. It never appears in account
  B's repository values, cache, or emulator-backed server collection. The test does not
  assume when the SDK resolves the write or whether it succeeds after reauthentication.
- Existing income CRUD, realtime observation, account switching, malformed-document
  rules, and offline transactional-delete checks remain covered by the 5-test income
  emulator suite.

## Confirmed defects

### Fixed test defect: uncached document lookup was treated as a snapshot

1. Queue A's write offline, sign out, and register B.
2. Read B's never-cached document with `Source.CACHE`.
3. Firestore throws `FirebaseFirestoreException` for a cache miss rather than returning
   a non-existent `DocumentSnapshot`.

Expected: a cache miss proves the A document is absent from B's cache. Actual before the
fix: the assertion failed before checking account isolation. The test now accepts the
cache-miss exception as absence and still requires B's server collection to be empty.

No application defect was confirmed. Initial 0-test runs caused by locked emulator
credential storage and transient low-memory killing were environment failures and are
not counted as product or test results.

## Manual checks still required

These are intentionally not labelled automated PASS:

1. **Actual process restart - BLOCKED/manual:** with local emulators and a test account,
   cache data online, disconnect networking, terminate the app process normally, relaunch
   it while still offline, and verify cached totals/records and offline messaging. Do not
   substitute `FirebaseApp.delete()` for process termination.
2. **Pending-write restart - manual:** queue a create while offline, terminate and relaunch
   the app, reconnect, and verify the record is either safely synchronized for its
   original owner or surfaces a recoverable failure; it must never appear under another
   account.
3. **UI state - manual:** verify cached, empty, error, pending, and reconnected states are
   accurately labelled and that the UI never claims a cloud save while the write is
   pending.

Use only local Auth/Firestore emulators and disposable test accounts for these steps.
