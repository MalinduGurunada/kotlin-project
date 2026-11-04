# M09 Jira integration verification

Verified on 2026-11-04 (Asia/Colombo) for
`feat/malindu/m09-integration-testing`.

## Final status

| Jira subtask | Result | Decision |
| --- | --- | --- |
| PBD-57 — verify salary, freelance, AdSense, crypto, and expense workflows | Existing workflow evidence was mapped and one missing M06-to-dashboard/expense integration test was added and passed. Android UI execution is **BLOCKED** because no device is attached. | **PASS automated; BLOCKED device smoke test** |
| PBD-58 — verify calculations, ViewModels, and account isolation | Exact money, realised-crypto, monthly/YTD dashboard, expense split, balance, ViewModel, owner-switch, and Firestore authorization evidence is present and green. | **PASS** |
| PBD-59 — document expected/actual results, commits, and evidence | This record identifies the tested commits, environment, expected and actual results, reused evidence, new execution, and remaining checks. | **PASS** |

**M09 Jira recommendation: CONDITIONAL.** The defined automated verification and
documentation are complete, but M09 should not be described as unconditional device-level
end-to-end verification while the PBD-57 Android checks below remain BLOCKED. It can be marked
Done only if device UI execution is explicitly non-gating for the Jira issue; otherwise run the
documented checks first.

## Tested revision and environment

| Item | Value |
| --- | --- |
| Repository | `D:\PBD Assignment\Finane App\kotlin-project` |
| Branch | `feat/malindu/m09-integration-testing` |
| Tested base commit | `567bd3cb66ef8759ffc1184188c78b0c6f9b0a18` (`567bd3c`, merge of `dev` into M09) |
| M09 evidence commit | `03a3dde` |
| M06 implementation commit | `75f8979` (included in M09 by `567bd3c`) |
| New focused test | Uncommitted working-tree change based on `567bd3c`; no commit was created by this verification |
| JVM environment | Gradle wrapper 9.6; local JDK; `app:testDebugUnitTest` |
| Firebase evidence environment | Local Firestore emulator only; demo project `demo-personal-finance-income-rules` |
| Android device | `adb devices -l` returned no attached device on 2026-11-04 |
| Production Firebase | Not used for destructive testing |

The pre-existing modifications to `app/google-services.json` and `.idea/misc.xml`, and the
untracked `adb` and `gradlew_out.txt`, were preserved and not changed as M09 work.

## PBD-57 workflow evidence

| Workflow | Expected result | Executable evidence and actual result |
| --- | --- | --- |
| Salary | Save a receipt with exact historical money data; expose it in income and dashboard totals. | `IncomeFormViewModelTest.valid salary creates a receipt with optional note`, mapper/repository tests, and the new dashboard integration test. **PASS** |
| Freelance | Save a receipt and project/receivable metadata without changing the exact money snapshot. | `IncomeFormViewModelTest.valid freelance creates a receipt with project reference`, mapper tests, receivable settlement tests, and the new dashboard integration test. **PASS** |
| AdSense | Save an AdSense receipt with a valid exact money snapshot and include it in totals. | `IncomeFormViewModelTest.valid AdSense creates an LKR receipt`, mapper tests, and the new dashboard integration test. **PASS** |
| Crypto | Derive realised gain/loss from proceeds, basis, and fees; store a positive magnitude plus direction; count only the signed net result in monthly/YTD totals. | `CryptoRealizedCalculatorTest`, `IncomeFormViewModelTest`, `IncomeValidatorTest`, `IncomeFirestoreMapperTest`, `IncomeViewModelTest`, local rules evidence, and the new structured-record dashboard integration test. **PASS** |
| Expense | Create, edit, delete, classify, and aggregate expenses without changing historical exchange-rate snapshots; subtract expenses once from available balance. | `ExpenseViewModelTest`, `FakeExpenseRepositoryTest`, editor presentation tests, account-scoped repository/rules evidence, and the new committed/discretionary dashboard integration test. **PASS** |

The new test uses all four income sources, an M06 structured crypto gain, an M06 structured
crypto loss, and committed and discretionary expenses in one deterministic dashboard flow. It
verifies five income records are counted (not gross crypto proceeds), signed income is 152,500
minor units, monthly and YTD crypto net is 22,500, expenses are 50,500, the split is 60/40,
and available balance is 102,000.

## PBD-58 calculation, ViewModel, and isolation evidence

| Requirement | Expected result | Actual evidence |
| --- | --- | --- |
| Exact financial calculations | LKR/USD snapshots use minor units and deterministic rounding; realised losses subtract; expenses subtract once; invalid/overflowing values fail safely. | `MoneyConverterTest`, `MoneyValidatorTest`, `CryptoRealizedCalculatorTest`, `IncomeValidatorTest`, `IncomeViewModelTest`, `ExpenseViewModelTest`, `DashboardViewModelTest`, and `IncomeSpendingCalculatorTest`. **PASS** |
| Dashboard integration | Current-month income, monthly/YTD crypto, expense totals, committed/discretionary split, counts, activity, and balance agree. | Existing salary/loss/expense dashboard regression plus new all-source structured-crypto/expense test: **8/8 dashboard tests PASS**. |
| ViewModels | Draft/save/edit/delete/retry/error behavior remains stable for income and expense, and dashboard state reflects repository emissions. | M09 JVM regression and M06 focused evidence cover `IncomeFormViewModelTest`, `IncomeViewModelTest`, `ExpenseViewModelTest`, and `DashboardViewModelTest`. **PASS** |
| Account isolation | Logout/account switch clears old state before observing the new owner; stale edit/delete/save completions cannot target another account; cross-owner Firestore access is denied. | `AccountScopedDataTest`, auth tests, income form/list account-switch tests, receivable switch tests, repository owner binding, and local Firestore authorization assertions. **PASS** |

## Evidence reused without unnecessary reruns

These suites were not repeated because the supplied evidence is already tied to commits now
contained in `567bd3c`:

| Evidence | Exact result | Commit/scope |
| --- | --- | --- |
| M09 JVM regression | **509 passed, 0 failed, 0 skipped** | `03a3dde` |
| M09 navigation regression | **7 passed, 0 failed** | `03a3dde` |
| M09 Firestore rules | **26 passed, 0 failed; 173 assertions** (62 allowed, 111 denied) | `03a3dde`, isolated local emulator |
| M09 debug APK | **PASS** | `03a3dde` |
| M06 focused income/dashboard | **259 passed, 0 failed** | `75f8979` |
| M06 Firestore rules emulator | **28 passed, 0 failed; 183 authorization assertions** (69 allowed, 114 denied) | `75f8979`, isolated local emulator |
| M06 compilation/APK | `compileDebugKotlin` and `assembleDebug` **PASS** | `75f8979` |

Counts above overlap and must not be added together. No new database code or rule was changed in
this final verification, so rerunning the local Firebase suites would not add coverage.

## New test and current execution

Added `DashboardViewModelTest.structuredCryptoResultsIntegrateWithAllIncomeSourcesAndExpenseClasses`.

Command:

```powershell
.\gradlew.bat app:testDebugUnitTest --tests "com.example.personalfinanceapp.feature.dashboard.DashboardViewModelTest" --no-configuration-cache
```

Actual result from the JUnit XML and Gradle run:

- **8 tests passed, 0 failed, 0 errors, 0 skipped**.
- This is **1 new integration test plus 7 existing dashboard regression tests**.
- Gradle result: **BUILD SUCCESSFUL**.

The first sandboxed wrapper attempt could not access the Gradle distribution and did not execute
tests. The approved rerun above is the recorded result.

## Defects

No application defect was discovered in this final audit, so no production code was changed.

The earlier M09 audit found one test-coverage defect: recurring destinations were absent from the
navigation regression assertion. Commit `03a3dde` corrected the assertions; the focused navigation
suite passed 7/7. Runtime navigation code was already correct.

## BLOCKED Android checks and exact instructions

No device was attached, so Android instrumented execution was not attempted. When an emulator is
available, first verify it appears in:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
```

Run the UI suites (they exercise the income editor/list, expense UI, and dashboard presentation):

```powershell
.\gradlew.bat app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.personalfinanceapp.feature.income.ui.IncomeEditorScreenTest" --no-configuration-cache
.\gradlew.bat app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.personalfinanceapp.feature.income.ui.IncomeListScreenTest" --no-configuration-cache
.\gradlew.bat app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.personalfinanceapp.feature.expense.ExpenseComposeUiTest" --no-configuration-cache
.\gradlew.bat app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.personalfinanceapp.feature.dashboard.DashboardComposeUiTest" --no-configuration-cache
```

For production repository persistence, start only disposable local Firebase emulators in one
terminal, then run the owner-scoped income repository suite in another:

```powershell
npx.cmd firebase emulators:start --config firebase.json --project demo-personal-finance-income-rules --only auth,firestore
.\gradlew.bat app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.personalfinanceapp.feature.income.FirestoreIncomeRepositoryEmulatorTest" --no-configuration-cache
```

The repository emulator suite fails closed unless `10.0.2.2:9099` and `10.0.2.2:8080` are
reachable. Do not use the installed app for destructive manual financial-record tests unless its
debug Firebase instances are explicitly wired to those local endpoints; the production app modules
do not call `useEmulator`.

## Files modified by this final verification

- `app/src/test/java/com/example/personalfinanceapp/feature/dashboard/DashboardViewModelTest.kt`
- `docs/m09-integration-verification.md`

No commit, push, merge, deployment, or production Firebase write was performed.
