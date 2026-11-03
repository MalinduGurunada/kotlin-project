# M09 integration and regression verification

Verified on 2026-11-03 for branch `feat/malindu/m09-integration-testing`.

## Scope and requirements evidence

No repository document, commit, or available Jira export defines official M09 acceptance
criteria. The available Jira-linked evidence is limited to earlier work:

| Evidence | Scope |
| --- | --- |
| `docs/firebase-manual-verification.md` | PBD-38 Google authentication; PBD-49, PBD-50, and PBD-51 FCM and notification navigation |
| `docs/m08-verification.md` | PBD-53, PBD-54, and PBD-55 offline cache, pending writes, account isolation, and M08 verification |
| Git history on this branch | Prior Firebase, goal, recurring, and M08 regression work; no M09 Jira key or acceptance criteria found |

The ten scenarios requested for this task are therefore the **proposed M09 integration
scope**, not invented Jira acceptance criteria. Jira completion must be checked against the
actual M09 issue before closure.

## Test environment

| Item | Value |
| --- | --- |
| Date | 2026-11-03 (Asia/Colombo) |
| Worktree | `D:\PBD Assignment\Finane App\kotlin-project` |
| Branch | `feat/malindu/m09-integration-testing` |
| Android build | Debug, compile/target SDK 37, minimum SDK 26 |
| JVM regression | Gradle 9.6 wrapper, local JDK, `app:testDebugUnitTest` |
| Rule regression | Firebase CLI 15.34.1, Firestore emulator 1.22.0, demo project `demo-personal-finance-income-rules` |
| Android device state | No ADB device or AVD connected during M09 verification |
| Production Firebase | Not used for destructive testing |

The requested `D:\PBD Assignment\Finane App\finclarity-m09` directory was not present.
`git worktree list` confirmed that the requested branch is checked out in the worktree above,
while the separate Firebase reconciliation worktree remains untouched.

Pre-existing local `app/google-services.json`, `adb`, and `gradlew_out.txt` changes were not
modified or included as M09 work. Firebase configuration contents are not reproduced here.

## Integration scenarios

| # | Scenario | Expected result | Actual result and evidence | Status | Affected modules |
| --- | --- | --- | --- | --- | --- |
| 1 | Authentication and session restoration | Startup resolves to login, registration recovery, or the authenticated overview without exposing a previous account. | Auth state, interrupted registration, stale async completion, startup routing, and account replacement are covered by `AuthViewModelTest`, `AccountScopedDataTest`, and `NavigationPolicyTest`. Full app-process restoration remains manual. | **PASS automated; BLOCKED manual restart** | `feature/auth`, `navigation` |
| 2 | Income and expense management | CRUD uses exact minor-unit money, UID-scoped records, category snapshots, and dashboard-visible data. | Income and expense repositories, validation, ViewModels, presentation, dashboard aggregation, and Firestore authorization passed JVM/rule regression. Prior income emulator CRUD coverage remains 5/5 PASS. | **PASS** | `feature/income`, `feature/expense`, `core/money`, `feature/dashboard` |
| 3 | Categories and budgets | Implemented category flows remain account scoped; a budget flow is tested only if it exists. | Category repository/ViewModel tests passed. No budget model, repository, screen, route, or test exists on this branch. Budget behavior is excluded as not implemented rather than reported as PASS. | **PASS categories; N/A budgets** | `feature/category`; no budget module |
| 4 | Goals and receivables | Goal progress uses the contribution ledger; receivable settlement derives from linked freelance income without cross-account leakage. | Goal repository/ViewModel/dashboard tests and receivable settlement, mapping, ViewModel account-switch, and Firestore rule tests passed. The dashboard goal view is read-only and the README marks full goal-management UI partial. | **PASS core integration; BLOCKED full goal UI scope** | `feature/goal`, `feature/receivable`, `feature/income`, `feature/dashboard` |
| 5 | Recurring commitments and reminders | Commitment schedules create deterministic occurrences; confirmation atomically creates one linked expense; reminders are owner checked and cleared on logout. | Schedule, repository, ViewModel, reminder scheduler/delivery policy, and Firestore atomicity rules passed. Device alarm/notification delivery was not rerun because no device was connected. | **PASS automated; BLOCKED device delivery** | `feature/recurring`, `feature/reminder`, `feature/expense`, `feature/auth` |
| 6 | Dashboard calculations | Current-month income, realized crypto loss, expenses, committed/discretionary split, activity, and goal progress remain consistent. | `DashboardViewModelTest`, `IncomeSpendingCalculatorTest`, and `ActivityPresentationTest` passed. Exact goal arithmetic and overflow rejection are covered. | **PASS** | `feature/dashboard`, `feature/income`, `feature/expense`, `feature/goal` |
| 7 | Firebase Cloud Messaging | Token registration is owner scoped, preferences control registration/delivery, cross-account payloads are ignored, and notification navigation waits for auth. | Eight JVM notification tests and Firestore FCM rule cases passed. Prior main real-device flows are preserved as reported project evidence; no fresh M09 device delivery run was possible. | **PASS automated/prior device evidence; BLOCKED fresh device rerun** | `feature/notification`, `feature/auth`, `navigation`, Firestore rules |
| 8 | Firestore offline caching | Cached values and pending writes retain exact data and never move between accounts. | Preserved M08 evidence is 8/8 PASS on an API 37 AVD and local Auth/Firestore emulators. The three documented real process/UI restart checks remain manual and were not relabelled automated. | **PASS prior emulator evidence; BLOCKED manual restart** | `feature/income`, `feature/expense`, `feature/offline`, `feature/auth` |
| 9 | Logout and account switching | Token/reminder cleanup runs, old account data clears before observing the new owner, and navigation resets. | Auth, account-scoped flow, receivable account-switch, FCM ownership, reminder owner policy, and navigation policy tests passed. The local Firestore rule suite denied cross-user access. | **PASS automated; manual device confirmation remains** | `feature/auth`, all Firestore repositories, `feature/notification`, `feature/recurring`, `navigation` |
| 10 | Navigation and error handling | Every exposed destination maps to a typed route; auth and notification transitions replace unsafe back stacks; recoverable failures remain visible. | Typed-route serialization and navigation policy passed. The incomplete management-route assertion was corrected and rerun. Compose/instrumented UI tests exist but were not run without a device. | **PASS JVM; BLOCKED instrumented UI rerun** | `navigation`, auth/dashboard/management screens |

## Existing regression coverage

The following existing tests already cover the integration boundaries:

- Authentication/session: `AuthViewModelTest`, `AccountScopedDataTest`,
  `AuthComposeUiTest`, and `AuthNavigationAndUiTest`.
- Income/expense/category: repository, mapper, validator, ViewModel, presentation, Compose,
  and `FirestoreIncomeRepositoryEmulatorTest` suites.
- Goals/receivables/recurring: fake and Firestore repository tests, ViewModel tests,
  settlement/schedule/reminder tests, and the goal/recurring emulator classes.
- Dashboard: `DashboardViewModelTest`, `IncomeSpendingCalculatorTest`,
  `ActivityPresentationTest`, and `DashboardComposeUiTest`.
- Notifications: `FinanceNotificationTest` and `FinanceNotificationInstrumentedTest`.
- Offline/account isolation: `FirestoreOfflineBehaviorEmulatorTest` plus account-scoped
  unit tests.
- Navigation: `NavigationPolicyTest`, `RoutesSerializationTest`, and auth navigation UI tests.
- Firestore authorization/invariants: `test/firestore.rules.test.js`.

## Tests executed

| Command/evidence | Result |
| --- | --- |
| `./gradlew app:testDebugUnitTest --no-configuration-cache` | **509 PASS, 0 failed, 0 skipped** across 46 JVM suites |
| Focused `NavigationPolicyTest` after the coverage correction | **7 PASS, 0 failed** |
| `npm.cmd run test:rules` | **26 PASS, 0 failed**; 173 rule assertions (62 allowed, 111 denied) against the isolated local Firestore emulator |
| `./gradlew app:assembleDebug --no-configuration-cache` | **PASS** |
| Preserved M08 Android emulator evidence | **8 PASS, 0 failed**; not rerun in M09 because no device/AVD was connected |

The first sandboxed Gradle and Firebase CLI attempts were environment/permission failures
before tests ran; successful reruns above are the recorded results. No production Firebase
record was created, updated, or deleted.

## Confirmed defects and fixes

### M09-T01: recurring management routes were missing from the navigation regression assertion

**Cause:** `NavigationPolicyTest.managementNavigationKeepsEveryFeatureAccessible` was not
updated when Recurring Commitments and Due Items were added to `ManagementDestination`.
Runtime routing was already correct, but the regression test's claim of covering every
management destination was inaccurate.

**Smallest safe fix:** add assertions mapping `RECURRING` and `DUE_ITEMS` to their respective
`AppNavigationTarget` values. No production code or UI was changed.

**Verification:** the focused navigation suite passed 7/7, the debug app packaged, and the
full pre-fix JVM run had already passed 509/509. The added assertions exercise the existing
production mapping.

No application runtime defect was confirmed during this audit.

## Genuine integration gaps and scope limitations

1. **Official M09 Jira acceptance criteria are unavailable.** The proposed scope above must
   be reconciled with the actual Jira issue before it is closed.
2. **Budgets are not implemented.** Categories are implemented and verified; no budget code
   or acceptance evidence exists on this branch.
3. **Goal management is partial.** Goal persistence, contribution ledger, progress, and a
   dashboard view exist, but the README explicitly marks full goal-management UI incomplete.
4. **Advanced income/spending analytics are not routed.** A calculator and Compose screen
   exist, but `AppNavHost` has no destination for the analytics screen. The wired Overview
   calculations are tested and passed.
5. **Transaction-detail actions are dormant/incomplete.** The typed destination exists, but
   its edit callback is empty and delete only returns to the previous screen. It is not part
   of the currently exposed expense edit flow and is not treated as an M09 defect without an
   acceptance criterion requiring it.
6. **Fresh Android instrumentation is unavailable in this run.** No ADB device was attached;
   therefore existing Compose, Firebase Android emulator, alarm, Google Sign-In, and FCM
   device tests were not rerun.

These gaps were documented rather than filled with unrelated feature or UI work.

## Remaining manual tests

The M08 status is preserved exactly: these checks are not automated PASS.

1. **Actual process restart - BLOCKED/manual:** cache data online with local emulators,
   disconnect networking, terminate the app process normally, relaunch offline, and verify
   cached totals/records and offline messaging.
2. **Pending-write restart - manual:** queue a create offline, terminate and relaunch,
   reconnect, and verify it synchronizes only for its original owner or exposes a recoverable
   failure.
3. **Offline UI states - manual:** verify cached, empty, error, pending, and reconnected labels
   and ensure the UI never claims a cloud save while a write is pending.
4. Repeat Google Sign-In cancellation, network recovery, logout/account switching, and data
   isolation on a Google Play-enabled device using the PBD-38 checklist.
5. Repeat FCM foreground/background/terminated navigation, permission denial, preference
   unregister/register, logout cleanup, and cross-account payload rejection using the
   PBD-49/PBD-50/PBD-51 checklist.
6. On a device backed only by disposable emulator accounts, smoke-test category-to-expense,
   receivable-to-income, recurring-confirmation-to-expense, goal progress, dashboard totals,
   back navigation, and visible recovery errors.

Use local Firebase emulators for every manual test that creates or deletes financial records.
Do not use production Firebase for destructive testing.

## M09 readiness

**Automated regression readiness: PASS.** JVM, Firestore rule, focused navigation, and debug
build verification are green, and the single confirmed test defect is corrected.

**Jira completion readiness: BLOCKED/conditional.** Do not close M09 until the official Jira
acceptance criteria are obtained and mapped to this record, and any required device/process
restart evidence above is completed. If Jira defines M09 as automated integration/regression
audit plus documentation only, the codebase is ready for review; it is not yet safe to claim
unconditional end-to-end device completion.
