# FinClarity feature audit

> **Historical snapshot:** This document audits `e0eda37` only. Before publication, remote `main` was fetched and the checkout advanced by 128 commits to `e2b4883`. These findings and original validation results are not an assessment of the newer application. See [README.md](README.md) before using this document.

Audit date: 5 November 2026 (Asia/Colombo).

Repository: `C:\Users\Himasha\AndroidStudioProjects\kotlin-project`.
Branch: `main`; audited commit: `e0eda37202132c8826d1c1d1ce762812bdc86a92` (16 October 2026).
With the user's approval, local main was fast-forwarded by nine commits to the existing `origin/main` snapshot. No fetch/pull was performed; this does not assert the current remote server has no newer commits. Work on dev or feature branches is outside this audit.

The task's `D:\PBD Assignment\Finane App\kotlin-project` path was unavailable because this environment has no D: drive. The initially open `PBDassignmnet` folder is a different project and is not the audited app.

SRS: `C:\Users\Himasha\Downloads\FinClarity_SRS (1).docx`, version 1.0, 15 October 2026. SHA-256: `C7F0275A732A0216FC7F129BF85FA2367D0AE067DA12C0C22889B161F61BA803`.
The SRS was read as requirements/data, including FR-01–FR-58, all 16 NFR rows, sections 1–7, appendices A–F and glossary. The pasted task supplied the audit instructions. The source SRS was not edited.

This is static implementation verification plus local build/unit-test evidence. No app was opened on a device, no manual result was recorded, no Firebase operation was executed, no notification was sent, and no real financial data was read or changed. Runtime behaviour and deployed Firebase settings remain unverified.

## Finding and status rules

The audited app has only **Login Screen → Go to Home → Home Screen**. The button does not authenticate. All finance repositories are in-memory fake implementations, and none is connected to the app UI. There are no feature ViewModels, Hilt repository bindings, Firestore calls, Room database/DAOs, settings or background workers on this commit.

- **IMPLEMENTED:** all required behaviour is connected through the real app and production data path, as far as source inspection can establish. This still would not prove a manual pass.
- **PARTIAL:** executable repository behaviour covers a subset, but the remaining behaviour or connection is absent. Here every partial entry is **repository-only and unavailable by tapping the app**.
- **MISSING:** no executable implementation of the requirement's defining behaviour. A class, enum, dependency, optional field, design paragraph or placeholder screen alone is not sufficient.
- **UNVERIFIED:** evidence is insufficient to establish implementation scope. Missing UI/code is directly observable here, so it is classified MISSING/PARTIAL rather than hidden under UNVERIFIED. Deployed rules and all manual results remain separately unverified.

These counts assess implementation scope, not successful runtime tests. PARTIAL does not mean that half of the app works on a phone.

## Coverage by priority

| Priority | IMPLEMENTED | PARTIAL (repository only) | MISSING | UNVERIFIED | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Must | 0 | 10 | 23 | 0 | 33 |
| Should | 0 | 3 | 18 | 0 | 21 |
| Could | 0 | 1 | 3 | 0 | 4 |
| **All** | **0** | **14** | **44** | **0** | **58** |

## Evidence index

All links below refer to the audited checkout. They will describe a different version if you later change branches.

- **NAV** — [app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt): AppNavHost: startDestination=Login (line 21), Login Screen (29), Go to Home (36), Home Screen (47); only two destinations.
- **ROUTES** — [app/src/main/java/com/example/personalfinanceapp/navigation/Routes.kt](../../app/src/main/java/com/example/personalfinanceapp/navigation/Routes.kt): Only serializable Login and Home routes.
- **BUILD** — [app/build.gradle.kts](../../app/build.gradle.kts): Compose/Hilt/Firebase declarations, minSdk 26, target/compile SDK 37; no Room or WorkManager dependency.
- **MANIFEST** — [app/src/main/AndroidManifest.xml](../../app/src/main/AndroidManifest.xml): Single exported launcher MainActivity, FinanceApplication, no app-declared notification permission/service/receiver.
- **TREE** — [README.md](../../README.md): Corroborating foundation-stage status; absence findings checked against all 28 main Kotlin files and the actual navigation graph, not inferred from README alone.
- **SCHEMA** — [docs/firestore-schema.md](../../docs/firestore-schema.md): Verified Firebase status and Proposed Schema; no deployed-infrastructure claim.
- **REPOS** — [docs/architecture.md](../../docs/architecture.md): Repository contracts/fakes corroborated by the six actual feature packages; no ViewModel, DI bindings or production data source.
- **MONEY** — [app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt): CurrencyCode LKR/USD; decimal-string original/rate and Long baseAmountMinor; no conversion function.
- **INCOME** — [app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt): Fixed source/kind/direction enums, Money/date/note and optional project/receivable/asset fields.
- **EXPENSE** — [app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt): Category snapshot, class, channel, Money, LocalDate, note/channelLabel; lacks dedicated merchant, instant and recurring origin.
- **CATEGORY** — [app/src/main/java/com/example/personalfinanceapp/core/model/Category.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Category.kt): COMMITTED/DISCRETIONARY and defaultSpendingClass/archived.
- **GOAL** — [app/src/main/java/com/example/personalfinanceapp/core/model/Goal.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Goal.kt): Name, targetAmountMinor, startDate/targetDate and archived.
- **CONTRIBUTION** — [app/src/main/java/com/example/personalfinanceapp/core/model/Contribution.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Contribution.kt): OPENING/ALLOCATION/REVERSAL, amountMinor, goal/date and reversesId.
- **RECEIVABLE** — [app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt): Expected amount/currency/date and project text reference; no invoice/milestone lifecycle.
- **RECURRING** — [app/src/main/java/com/example/personalfinanceapp/core/model/RecurringExpense.kt](../../app/src/main/java/com/example/personalfinanceapp/core/model/RecurringExpense.kt): WEEKLY/MONTHLY/YEARLY templates with interval, dates, active and reminder flags.
- **FI** — [app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt): observeIncomes (20), saveIncome (42), deleteIncome (72); memory list, date/ID ordering, ID-only save validation.
- **FE** — [app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt): observeExpenses, saveExpense, deleteExpense; memory list with date filtering and ID-only save validation.
- **FC** — [app/src/main/java/com/example/personalfinanceapp/feature/category/FakeCategoryRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/category/FakeCategoryRepository.kt): Three seed categories; addCategory, renameCategory and setCategoryArchived; no reclassification method.
- **FG** — [app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt): saveGoal, setGoalArchived, addContribution and observations; duplicate/reversal checks, all in memory.
- **FR** — [app/src/main/java/com/example/personalfinanceapp/feature/receivable/FakeReceivableRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/receivable/FakeReceivableRepository.kt): saveReceivable validates positive BigDecimal/text; setReceivableArchived; memory only.
- **FRE** — [app/src/main/java/com/example/personalfinanceapp/feature/recurring/FakeRecurringRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/recurring/FakeRecurringRepository.kt): saveRecurringExpense validates a template; setRecurringExpenseActive; no generation/delivery.
- **THEME** — [app/src/main/java/com/example/personalfinanceapp/ui/theme/Theme.kt](../../app/src/main/java/com/example/personalfinanceapp/ui/theme/Theme.kt): isSystemInDarkTheme; dynamic colours on API >=31; static light/dark palettes otherwise.

## Every functional requirement

For **every** requirement below, the access path is unavailable: Open PersonalFinanceApp → tap **Go to Home** → **Home Screen**, which contains no financial controls. The per-row access statement repeats this explicitly so a model/repository is never mistaken for a phone feature.

### FR-01 — Firebase email registration

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow a new user to register with email and password via Firebase Authentication.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [BUILD](../../app/build.gradle.kts).

**Present behaviour:** Firebase Auth is a declared dependency only.

**Incomplete or absent:** No registration form, Firebase create-user call, validation, or account lifecycle.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-02 — Login, logout and persistent session

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow login and logout, and shall keep the session persistent so the user is not asked to log in on every launch.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [ROUTES](../../app/src/main/java/com/example/personalfinanceapp/navigation/Routes.kt).

**Present behaviour:** The button changes the route from Login to Home without credentials.

**Incomplete or absent:** No authentication, logout, current-user observation or session gate. Navigation state is not an authenticated session.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-03 — Password reset email

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall provide password reset by email.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [BUILD](../../app/build.gradle.kts).

**Present behaviour:** No password-reset behaviour.

**Incomplete or absent:** No email field, reset action, Firebase reset call or confirmation message.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-04 — Google Sign-In

**Priority:** Could. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall offer Google Sign-In as an alternative to email/password.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [BUILD](../../app/build.gradle.kts).

**Present behaviour:** No Google sign-in behaviour.

**Incomplete or absent:** No credential provider, Google button or Firebase credential exchange.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-05 — Server-enforced user isolation

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall isolate each user's data so that an authenticated user can read and write only their own records (enforced by Firestore Security Rules, not only by client code).

**Evidence:** [TREE](../../README.md), [REPOS](../../docs/architecture.md), [SCHEMA](../../docs/firestore-schema.md).

**Present behaviour:** Repository interfaces have no user identity and their fakes store process-local lists.

**Incomplete or absent:** No UID-scoped Firebase access or submitted firestore.rules. Deployed rules were not inspected; their state is UNVERIFIED, not presumed open.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-06 — Skippable onboarding

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall run a skippable onboarding (target: under two minutes) capturing: base currency (LKR), savings goal (name, target amount, deadline, amount already saved), expected salary and pay day, and the user's own estimate of monthly income as a range (e.g. LKR 160,000-210,000).

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [ROUTES](../../app/src/main/java/com/example/personalfinanceapp/navigation/Routes.kt).

**Present behaviour:** No onboarding route.

**Incomplete or absent:** No base-currency, goal, initial saving, salary/payday or income-estimate setup; no skip action or completion state.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-07 — Delete account and all data

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow the user to delete their account and all associated data.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [TREE](../../README.md).

**Present behaviour:** No account deletion behaviour.

**Incomplete or absent:** No reauthentication, account deletion, associated-data cleanup or confirmation UI.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-08 — Define configurable income sources

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user define multiple income sources, each with a type (Salary, Freelance, Ad Revenue, Trading, Other), a default currency, a cadence (fixed monthly date, irregular, or milestone-based) and an optional expected amount or range.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** IncomeSource is a fixed enum: SALARY, FREELANCE, ADSENSE and CRYPTO.

**Incomplete or absent:** No source definition or editing, Other source, default currency, cadence or expected-range configuration. An enum is not a source manager.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-09 — Record, edit and delete income

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user record an income receipt against a source with amount, currency, date received and optional note, and shall edit or delete it later.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt).

**Present behaviour:** saveIncome inserts or replaces by ID; deleteIncome removes an existing ID; observeIncomes filters dates. Source, date, money and note are retained in memory.

**Incomplete or absent:** No income screen, ViewModel, injected repository or durable Firebase storage. Only the ID is validated by the fake.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-10 — Historical foreign-income conversion

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** For any non-LKR income the system shall capture and persist the original amount, original currency, exchange rate applied and the resulting LKR equivalent. The LKR equivalent shall be calculated at entry time and shall not silently change if later rates change (e.g. AdSense is converted by the processor at transfer time).

**Evidence:** [MONEY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt).

**Present behaviour:** Saving an Income retains the caller-supplied Money fields, including original amount/currency, applied rate and LKR minor amount, in memory.

**Incomplete or absent:** No entry-time calculation, agreement between the fields, persistence across restart or rate-change workflow. Only LKR and USD are modelled; USDT/ETH currencies are absent.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-11 — Prefill last used exchange rate

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall pre-fill the exchange rate with the most recent rate the user used for that currency, and allow it to be overridden.

**Evidence:** [MONEY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** A rate can be represented in Money.

**Incomplete or absent:** No last-used-rate store, prefill, override UI or rate-selection policy.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-12 — Separate pending from received income

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall distinguish expected income from received income. Only received income counts toward actual totals; expected-but-not-received income shall be displayed separately as 'pending'.

**Evidence:** [RECEIVABLE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt), [FR](../../app/src/main/java/com/example/personalfinanceapp/feature/receivable/FakeReceivableRepository.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt).

**Present behaviour:** Receivables and received incomes use separate in-memory repositories; saving a receivable does not insert an income.

**Incomplete or absent:** No pending display, totals calculation, receipt confirmation or settlement workflow. Separation alone does not prove actual totals exclude pending values.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-13 — Payday salary confirmation prompt

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** On the configured pay day (the 25th) the system shall prompt the user to confirm the salary, pre-filled with the expected amount, so the actual deposit (LKR 120,000-140,000 depending on bonus) can be recorded or corrected in one tap.

**Evidence:** [TREE](../../README.md), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** No salary prompt behaviour.

**Incomplete or absent:** No payday setting, scheduler, expected-salary prefill or one-tap deposit confirmation.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-14 — Month-start AdSense prompt

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** At the start of each month the system shall prompt the user to record the previous month's ad revenue payout (USD amount and converted LKR amount).

**Evidence:** [TREE](../../README.md), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** ADSENSE exists only as an income source value.

**Incomplete or absent:** No month-start scheduling, notification, previous-month attribution or payout entry.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-15 — True monthly income with source breakdown

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall calculate 'true monthly income' as the sum of LKR equivalents of all received income across all sources for the selected month, with a per-source breakdown, in a single place and without the user visiting any other app.

**Evidence:** [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** A fake repository can return incomes within a date range.

**Incomplete or absent:** No aggregation, crypto loss sign handling, per-source breakdown or single income-summary screen.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-16 — Freelance projects and milestones

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user create a freelance project with client name, project title, agreed total (LKR 20,000-90,000 range supported), currency, and one or more milestones each with an amount and due date.

**Evidence:** [RECEIVABLE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt), [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt).

**Present behaviour:** Receivable has title/projectReference/expected amount/date; Income has an optional projectReference.

**Incomplete or absent:** No client/project entity, agreed total, milestone list, milestone amount/due-date management or project screen. A text project reference is insufficient.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-17 — Link payments to project and milestone

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** Each freelance payment shall be linked to a project and milestone so the user can always see which payment belongs to which project.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [RECEIVABLE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt).

**Present behaviour:** Income can carry projectReference and receivableId supplied by a caller.

**Incomplete or absent:** No milestone reference, reconciliation or relationship validation; no payment-to-project UI.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-18 — Invoice balances and payment status

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall show for every project the amount invoiced, amount received and outstanding balance, with a status (Not invoiced / Invoiced / Partially paid / Paid).

**Evidence:** [RECEIVABLE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt), [FR](../../app/src/main/java/com/example/personalfinanceapp/feature/receivable/FakeReceivableRepository.kt).

**Present behaviour:** A fake can save or archive expected payments.

**Incomplete or absent:** No invoiced/received/outstanding calculation, invoice lifecycle or status screen.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-19 — Overdue invoice follow-up

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall flag invoices unpaid beyond a configurable number of days (default 7) and send a follow-up reminder.

**Evidence:** [TREE](../../README.md), [RECEIVABLE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Receivable.kt).

**Present behaviour:** An expected date can be stored on a Receivable.

**Incomplete or absent:** No configurable overdue threshold, invoice flagging, notification channel or reminder delivery.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-20 — Record realised crypto gain/loss

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user record a trading result (asset such as USDT/ETH/other, realised gain or loss amount in original currency, rate, LKR equivalent, date). Losses shall be stored as negative values.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [MONEY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt).

**Present behaviour:** Income can retain REALIZED_PNL, GAIN/LOSS, assetSymbol and caller-supplied Money/date through fake saveIncome.

**Incomplete or absent:** No trading-entry UI, signed-result calculator, validation that losses are negative, persistent trade ledger or USDT/ETH currency values. pnlDirection alone does not enforce the stored sign.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-21 — Crypto YTD/month/asset net position

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall maintain a running crypto net position: year-to-date net, net per month and net per asset, so the user can answer 'what are my total crypto earnings this year?'.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Date/source/asset fields exist.

**Incomplete or absent:** No net-position calculator or monthly, annual or per-asset result view.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-22 — Crypto loss reduces income analysis

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** Trading results shall appear as their own source in income analysis, and a net-negative month shall reduce, not hide inside, total income.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** GAIN/LOSS can be represented.

**Incomplete or absent:** No income-analysis computation. A loss marker is not proof that it reduces totals.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-23 — Persistent three-tap expense quick-add

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall provide a quick-add expense flow reachable from any main screen (persistent action button) in which the user enters amount first, picks a category chip, and saves - completing in no more than three taps after the amount is typed.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** The only app button is Go to Home.

**Incomplete or absent:** No persistent expense action, amount-first editor, category chips or save action. The specified tap budget cannot be measured.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-24 — Complete expense fields

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** An expense record shall capture: amount, currency (default LKR), category, payment method/account (card, cash, secondary account, other), date and time (default now, editable), optional merchant/payee, optional note, and whether it came from a recurring rule.

**Evidence:** [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt).

**Present behaviour:** Fake saveExpense retains Money, category ID/name snapshot, spending class, CASH/CARD/BANK/OTHER channel, date, optional note and channelLabel.

**Incomplete or absent:** No expense entry UI or persistence; no time-of-day, dedicated merchant/payee, account entity/secondary-account reference or recurring-rule origin. channelLabel is not verified merchant memory.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-25 — Default/custom categories and retroactive classification

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall provide default categories pre-classified as Committed or Discretionary (Appendix E) and allow the user to create categories and change any category's classification; reclassification shall apply retroactively to all analysis.

**Evidence:** [CATEGORY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Category.kt), [FC](../../app/src/main/java/com/example/personalfinanceapp/feature/category/FakeCategoryRepository.kt).

**Present behaviour:** Fake categories start as Food/Transport discretionary and Bills committed. Create, trimmed/case-insensitive name checks, rename and archive work in memory.

**Incomplete or absent:** Appendix E defaults are absent; no classification-update method, retroactive analytics, category UI or Firebase storage. Expense snapshots would require an explicit effective-classification policy.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-26 — Merchant suggestions and category memory

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall suggest merchants as the user types, remember the category last used for each merchant (e.g. PickMe -> Transport), and surface the most-used categories first.

**Evidence:** [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt), [FC](../../app/src/main/java/com/example/personalfinanceapp/feature/category/FakeCategoryRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** No merchant behaviour.

**Incomplete or absent:** No merchant field/service, suggestions, remembered category or usage-based category ordering.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-27 — Offline expense entry and automatic sync

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** Expense entry shall work fully offline; entries made offline shall sync automatically when connectivity returns.

**Evidence:** [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt), [TREE](../../README.md), [BUILD](../../app/build.gradle.kts).

**Present behaviour:** The fake uses memory and requires no network if called by a test.

**Incomplete or absent:** No expense UI, local durable store, Room, Firestore operations, write queue or reconnect sync. Memory-only execution is not offline persistence.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-28 — Expense edit/delete with undo

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow editing and deleting expenses, with an undo option immediately after a save or delete.

**Evidence:** [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt).

**Present behaviour:** saveExpense replaces the same ID; deleteExpense removes the existing record and rejects a missing ID.

**Incomplete or absent:** No editing/deletion screen, undo buffer/snackbar or persistent writes. Both save-undo and delete-undo are absent.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-29 — Back-date and gentle catch-up view

**Priority:** Should. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow back-dated entries and provide a 'catch up' view listing recent days with no entries; missing days shall never be presented as failure.

**Evidence:** [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt).

**Present behaviour:** Caller-supplied LocalDate is retained without a today-only restriction; date queries can return past entries.

**Incomplete or absent:** No date picker, missing-days/catch-up view or user-facing gentle completeness feedback.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-30 — Transfers and unaccounted cash

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall treat cash withdrawals and inter-account transfers as transfers between accounts, not as expenses, so spending is not double counted; cash that has been withdrawn but not itemised shall be shown as 'unaccounted cash'.

**Evidence:** [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt), [TREE](../../README.md).

**Present behaviour:** PaymentChannel includes CASH and BANK.

**Incomplete or absent:** No account balances, transfer entity, withdrawal treatment or unaccounted-cash computation. A payment channel is not an account ledger.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-31 — Foreign-currency expense conversion

**Priority:** Should. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall support foreign-currency expenses (e.g. USD subscriptions) using the same original-amount / rate / LKR-equivalent model as income.

**Evidence:** [MONEY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt), [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt).

**Present behaviour:** An expense can retain caller-supplied USD/LKR Money fields in memory.

**Incomplete or absent:** No expense currency/rate editor, converter, field consistency checks or durable historical-value guarantee.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-32 — Transaction search and all filters

**Priority:** Should. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow searching and filtering transactions by date range, category, merchant, payment method, source and amount.

**Evidence:** [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Fake income and expense queries implement inclusive start/exclusive end date filtering and deterministic date/ID ordering.

**Incomplete or absent:** No transaction screen, search box or category/merchant/payment-method/source/amount filters; no UI date selector.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-33 — Recurring costs with pending confirmations

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The user shall be able to define recurring committed costs (for example rent LKR 34,000, gym membership, utilities) with amount, day of month and category. The system shall generate each occurrence as a pending item the user confirms in one tap or adjusts (needed for fluctuating utilities).

**Evidence:** [RECURRING](../../app/src/main/java/com/example/personalfinanceapp/core/model/RecurringExpense.kt), [FRE](../../app/src/main/java/com/example/personalfinanceapp/feature/recurring/FakeRecurringRepository.kt).

**Present behaviour:** Fake saveRecurringExpense inserts/updates templates and validates amount, interval, dates and reminder lead time; active state is editable in memory.

**Incomplete or absent:** No occurrence generator, due-item records, adjustment/confirmation action or creation of an expense; no recurring screen or persistence.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-34 — Subscription register and cancel intent

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall maintain a subscription register (name, amount, currency, renewal date, account) showing the total monthly subscription cost, and allow flagging a subscription 'intend to cancel'.

**Evidence:** [RECURRING](../../app/src/main/java/com/example/personalfinanceapp/core/model/RecurringExpense.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Generic recurring templates exist.

**Incomplete or absent:** No subscription currency/account, renewal register, monthly subscription total or intend-to-cancel flag.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-35 — Subscription renewal reminders

**Priority:** Could. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall remind the user before a flagged or any subscription renews and show how long it has been flagged.

**Evidence:** [RECURRING](../../app/src/main/java/com/example/personalfinanceapp/core/model/RecurringExpense.kt), [TREE](../../README.md).

**Present behaviour:** Templates carry reminderEnabled/reminderLeadDays fields.

**Incomplete or absent:** No subscription renewal calculation, flagged duration, background scheduler or notification delivery.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-36 — Create goal with initial savings

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user create a savings goal with name, target amount, deadline and initial saved amount (initially MacBook Pro M4, LKR 490,000, with LKR 11,200 already saved).

**Evidence:** [GOAL](../../app/src/main/java/com/example/personalfinanceapp/core/model/Goal.kt), [CONTRIBUTION](../../app/src/main/java/com/example/personalfinanceapp/core/model/Contribution.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt).

**Present behaviour:** saveGoal inserts/updates name, positive target and dates; a separate OPENING contribution can be appended after the goal exists.

**Incomplete or absent:** No goal form, initial-savings field or atomic goal/opening write; no ViewModel, injected persistence or phone route.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-37 — Required monthly saving recalculation

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall calculate and display the required monthly saving to reach the goal by the deadline, recalculated automatically as time passes and contributions are made (e.g. (490,000 - 11,200) / 12 = LKR 39,900 per month at start).

**Evidence:** [GOAL](../../app/src/main/java/com/example/personalfinanceapp/core/model/Goal.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Target dates and contributions exist as inputs.

**Incomplete or absent:** No months-remaining policy, required-saving calculation, automatic time recalculation or displayed monthly amount.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-38 — Contributions and goal progress

**Priority:** Must. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user record contributions to the goal and shall display amount saved, amount remaining and percentage complete.

**Evidence:** [CONTRIBUTION](../../app/src/main/java/com/example/personalfinanceapp/core/model/Contribution.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt).

**Present behaviour:** addContribution appends records, rejects changed duplicate IDs, handles identical retries and validates full once-only reversals; observeContributions filters by goal.

**Incomplete or absent:** No contribution screen or saved/remaining/percentage calculator/display; no durable storage. The repository test sums values itself.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-39 — Actual saving versus required and status

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall compute the actual monthly saving (received income minus expenses) and compare it with the required amount, showing a status of Ahead, On track or Behind and the shortfall or surplus in LKR.

**Evidence:** [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Repositories exist independently.

**Incomplete or absent:** No income-minus-expense calculation, required comparison, Ahead/On track/Behind status or shortfall/surplus display.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-40 — Projected goal completion date

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall project a completion date at the current saving rate and show how it differs from the deadline.

**Evidence:** [GOAL](../../app/src/main/java/com/example/personalfinanceapp/core/model/Goal.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt).

**Present behaviour:** Dates and target can be stored.

**Incomplete or absent:** No saving-rate model, zero/negative-rate policy, completion projection or deadline difference.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-41 — Spending expressed in goal terms

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall express spending in goal terms, for example 'LKR 24,000 on coffee and dining this month = 4.9% of your MacBook' and how many days the goal is delayed.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt).

**Present behaviour:** No spend-in-goal-terms behaviour.

**Incomplete or absent:** No goal percentage/delay calculation or category/merchant insight UI.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-42 — What-if goal deadlines

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall offer what-if deadlines (for example 12, 15 or 18 months) showing the required monthly saving for each, so an unrealistic target can be reshaped instead of abandoned.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt).

**Present behaviour:** No what-if behaviour.

**Incomplete or absent:** No 12/15/18-month alternatives or comparison calculation.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-43 — Pay-yourself-first contribution prompt

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** When income is recorded the system shall prompt the user to set aside part of it for the goal, pre-filled with the current required monthly amount (pay-yourself-first).

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt).

**Present behaviour:** Income and goal repositories are separate.

**Incomplete or absent:** No post-income prompt, monthly requirement prefill or contribution handoff.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-44 — Goal edits preserve history and celebrate milestones

**Priority:** Could. **Status:** PARTIAL. **Verification:** code only; no manual pass.

**SRS requirement:** The user shall be able to edit the goal target or deadline without losing contribution history; the system shall celebrate 25%, 50% and 75% milestones.

**Evidence:** [FG](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt), [CONTRIBUTION](../../app/src/main/java/com/example/personalfinanceapp/core/model/Contribution.kt).

**Present behaviour:** Replacing a goal with the same ID does not clear the separate contribution list; positive target/date checks apply.

**Incomplete or absent:** No target/deadline editing UI, durable history or 25/50/75 percent celebration. There is no milestone-crossing calculation.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-45 — Live current-month dashboard

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The dashboard shall be the launch screen and shall display, for the current month and updated in real time through Firestore listeners: received income, total spent, net saved, goal progress with required-versus-actual saving, committed-versus-discretionary split, and the most recent transactions.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [ROUTES](../../app/src/main/java/com/example/personalfinanceapp/navigation/Routes.kt), [TREE](../../README.md).

**Present behaviour:** Launch goes to Login; Home shows only Home Screen.

**Incomplete or absent:** No totals, net saving, required/actual goal strip, split or recent transactions; no Firestore listeners.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-46 — Committed/discretionary chart and income shares

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall present a committed-versus-discretionary view (chart plus plain-language labels such as 'Locked in' and 'Your choice'), shown both as a share of spending and as a share of received income.

**Evidence:** [CATEGORY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Category.kt), [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** A spending-class enum and expense field exist.

**Incomplete or absent:** No aggregation, chart, Locked in/Your choice labels, spending shares or income shares.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-47 — Category breakdown with transaction drill-down

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall present a category breakdown for the selected period with drill-down to the underlying transactions.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt).

**Present behaviour:** Expenses can carry category identifiers.

**Incomplete or absent:** No breakdown, selected-period UI, category chart or drill-down route.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-48 — Income sources and multi-month trend

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall present income by source for the selected month and a multi-month trend, so variability across salary, freelance, ad revenue and trading is visible.

**Evidence:** [INCOME](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Source enum and date filter are available to code.

**Incomplete or absent:** No source totals, multi-month trend, chart or income analysis screen.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-49 — Top discretionary category and merchant leaks

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall highlight 'where it leaked': the top discretionary categories and merchants of the month with their totals.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [EXPENSE](../../app/src/main/java/com/example/personalfinanceapp/core/model/Expense.kt).

**Present behaviour:** Spending class can be retained on an expense.

**Incomplete or absent:** No ranking, category totals, merchant model or leak insight view.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-50 — Six-month comparison

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall show a month-over-month comparison (income, spending, saving rate) for up to the previous six months.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [TREE](../../README.md).

**Present behaviour:** No comparison behaviour.

**Incomplete or absent:** No monthly summary calculator or previous-six-month income/spending/saving-rate view.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-51 — Estimated-versus-measured income reality check

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall provide an income reality check comparing the user's onboarding estimate with measured average monthly income, and display the difference.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [TREE](../../README.md).

**Present behaviour:** No estimate/profile data model or entry flow.

**Incomplete or absent:** No onboarding estimate, measured average, comparison or difference display.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-52 — Income stability and safe planning amount

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall provide an income stability indicator: trailing three-month average, minimum and maximum, and a conservative 'safe to plan with' amount for goal planning.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt).

**Present behaviour:** No stability behaviour.

**Incomplete or absent:** No trailing three-month average/minimum/maximum, conservative planning policy or goal integration.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-53 — Consistent global period selector

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall provide a period selector (this month default, previous months, custom range) that applies consistently to every view.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [FI](../../app/src/main/java/com/example/personalfinanceapp/feature/income/FakeIncomeRepository.kt), [FE](../../app/src/main/java/com/example/personalfinanceapp/feature/expense/FakeExpenseRepository.kt).

**Present behaviour:** Repository date-range parameters exist independently.

**Incomplete or absent:** No shared selected-period state or current/previous/custom selector applied to every view.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-54 — Meaningful empty states with calls to action

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** Every list and chart shall have a meaningful empty state with a call to action instead of a blank or broken screen.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt).

**Present behaviour:** Home Screen is placeholder text; Login has a navigation button.

**Incomplete or absent:** No financial lists/charts exist to provide empty states. A generic Home label is not the required feature-specific empty state.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-55 — One optional daily capture reminder

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall send at most one optional daily capture reminder at a user-chosen time, suppressed if an entry has already been made that day, with a quick-add action in the notification.

**Evidence:** [TREE](../../README.md), [BUILD](../../app/build.gradle.kts), [MANIFEST](../../app/src/main/AndroidManifest.xml).

**Present behaviour:** No notification behaviour.

**Incomplete or absent:** No user-time setting, daily-entry suppression, quick-add notification action, permission flow or scheduler.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-56 — Currencies, rates, categories, reminders and payday settings

**Priority:** Should. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall let the user manage supported currencies, default exchange rates, base currency display, categories, reminder times and the pay day.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [MONEY](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt), [FC](../../app/src/main/java/com/example/personalfinanceapp/feature/category/FakeCategoryRepository.kt).

**Present behaviour:** LKR/USD enums and fake category operations are foundations only.

**Incomplete or absent:** No Settings route, supported-currency management, default-rate store, display-base preference, reminder-time setting or payday setting.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-57 — Real-time Firestore sync and concurrent edits

**Priority:** Must. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall synchronise data with Firestore in real time and resolve concurrent edits deterministically (last write wins at document level using server timestamps).

**Evidence:** [REPOS](../../docs/architecture.md), [SCHEMA](../../docs/firestore-schema.md), [TREE](../../README.md).

**Present behaviour:** Mutex serialises writes within each fake instance; Flows emit memory changes.

**Incomplete or absent:** No Firestore listeners/writes, server timestamps, cross-device conflict policy, persistence or account switching. Mutex does not resolve cloud edits.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

### FR-58 — Period CSV export

**Priority:** Could. **Status:** MISSING. **Verification:** code only; no manual pass.

**SRS requirement:** The system shall allow export of transactions for a selected period as CSV.

**Evidence:** [NAV](../../app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt), [TREE](../../README.md).

**Present behaviour:** No export behaviour.

**Incomplete or absent:** No CSV generation, period export action, file creation or Android sharing/save flow.

**Access:** No feature route or control. Open app → Go to Home → Home Screen; stop here.

## All non-functional requirements (SRS section 4)

| NFR category | Status / evidence limit | Assessment and remaining verification |
| --- | --- | --- |
| Usability — friction | MISSING | No transaction editor exists, so neither ten seconds/three taps nor first transaction within two minutes is achievable. Time the real flow only after integration. |
| Usability — emotional safety | MISSING | No partial-month, completeness or catch-up UI. Absence of streak counters alone does not satisfy the requirement. |
| Usability — plain language | MISSING | Only Login Screen/Home Screen labels exist. No plain-language financial chart or five-second comprehension test is possible. |
| Usability — time to insight | MISSING | Seven days of use cannot reveal income, split or goal progress because there are no entry or insight screens. |
| Performance | UNVERIFIED; prerequisite dashboard MISSING | No cold-start/navigation/refresh timing was measured on a device. An APK build is not a benchmark. Required dashboard/cache does not exist; thresholds remain <3 s, <300 ms and <2 s respectively. |
| Offline-first reliability | MISSING | No Room dependency/entity/DAO/database and no Firestore reads/writes or sync queue. Fakes lose state on re-instantiation and are not connected to the app. They are neither a Firestore cache nor a durable offline store. |
| Robustness / error handling | PARTIAL, code foundations only | Fake repositories return Result failures for some invalid IDs/names/amounts/dates; invalid date query ranges throw require exceptions. No ViewModel/UI handles those failures; no offline banner/retry, goal division policy or invalid-input UI. No-crash runtime guarantee is unverified. |
| Data accuracy and integrity | PARTIAL, representation only | Money uses String decimal fields and Long baseAmountMinor; receivable validation uses BigDecimal. No converter/rounding/overflow policy, common aggregation path or enforced original×rate=LKR invariant. Income/expense fake saves validate only ID, allowing inconsistent caller-supplied money. Crypto loss markers are not normalised into signed totals. |
| Security | MISSING in app/submitted rules; deployed state UNVERIFIED | Go to Home needs no credentials. Firebase SDK dependencies/configuration are not an Auth gate. No submitted rules, field validation or UID data scoping. No credential disclosure is inferred from standard Firebase configuration. No live service or network traffic was inspected. |
| Privacy | MISSING; deployed state UNVERIFIED | No user identity boundary, user-controlled full deletion or financial-data storage implementation. allowBackup=true and template backup/extraction files exist; assess actual persisted sensitive data and backup policy when storage is added. No claim is made about cloud data exposure or third-party processing. |
| Scalability and cost | PARTIAL in fakes; cloud behaviour UNVERIFIED | Income/expense observations accept bounded date ranges but scan complete in-memory lists. No 50-record paging, Firestore queries/indexes or cost/load measurements. A proposed schema is not deployed aggregation or pagination. |
| Compatibility | PARTIAL configuration; runtime UNVERIFIED | minSdk=26 and target/compileSdk=37 meet the stated numeric bounds. Debug APK builds. Theme selects light/dark and API>=31 dynamic colours. No API 26/current-device, 5–7 inch or device-matrix run was performed. |
| Maintainability and architecture | PARTIAL | Kotlin/Compose/Material 3, one MainActivity, typed Navigation Compose, Hilt application/activity annotations, Flow contracts and unit-testable fakes exist. No ViewModels, immutable UI state layer, repository injection modules or complete UI→VM→Repository→Firebase chain. Do not describe the branch as complete MVVM. |
| Accessibility and localisation | PARTIAL component foundation; runtime UNVERIFIED | Material 3 Button/Text and sp typography are used; supportsRtl=true. No chart semantics, currency formatting or date UI to assess. Touch sizes, contrast, TalkBack, font scaling and rotation require MT-05–MT-10 device checks. |
| Battery and notification etiquette | MISSING | No WorkManager dependency/workers, channel creation, scheduler, per-type controls, context permission request or delivery cap. A RecurringExpense reminder flag does not deliver reminders. |
| Testability and demonstrability | PARTIAL local tests; core demo MISSING | 57 local tests pass; a debug APK builds. No demonstrated live Firebase connection/seeded demo data and no end-to-end finance flow. The single instrumentation test only checks the package name and was not run. |

## SRS sections and appendices reviewed

| SRS location | Result |
| --- | --- |
| §§1–2.1 purpose/perspective | Treat as intended product; the current branch is a foundation, not the described working finance app. Room-mediated Firestore reads/writes are absent. |
| §2.2 product functions | Income, goals, recurring templates and receivables have some fake repository operations; every described financial phone flow is absent. |
| §§2.3–2.4 users/constraints | No user account identity exists in the UI. Kotlin/Compose/Hilt shell and SDK bounds exist; MVVM, offline reliability, rules and live Firebase submission are incomplete. |
| §3 FR-01–FR-58 | All 58 audited individually above, including Must/Should/Could. |
| §4 NFRs | All 16 categories assessed above; timing and runtime claims remain unmeasured. |
| §5 external interfaces | Required bottom bar/screens, Firebase calls/listeners, Room, WorkManager, channels and deployed rules are absent. Login/Home are prototypes only. |
| §6 assumptions | LKR/USD enum is narrower than multi-currency discussion; primary/additional goals are only code foundations; no payday, user estimate or settings. No bank import or live exchange integration exists. |
| Appendix A | Scenario-to-FR traceability is design rationale, not completed-feature evidence; retain mappings and add this audit status. |
| Appendix B | Stated remedies for abandoned tools are intended; quick-add, merchant memory, transfer handling and goal rewards are unavailable here. |
| Appendix C | All UC-01–UC-16 are absent as end-to-end app use cases. UC-04/08/09/10/11/16 have some repository foundations only; the Home button does not satisfy UC-01. |
| Appendix D | Worked arithmetic is a target, not implemented calculation. 478,800/12=39,900; after a month and 20,000 contribution, 458,800/11≈41,709.09. No calculator displays these. |
| Appendix E | Only fake Food/Transport/Bills seeds exist, not the 12 specified categories; no retroactive reclassification. |
| Appendix F | Some Kotlin entities exist. UserProfile, configurable IncomeSource entity, project/milestone/invoice, account/transfer, subscription and MonthlySummary implementation are absent. Dates use LocalDate, not expense times. |
| Glossary | Definitions describe the intended product. In particular Room cache, true monthly income, crypto net position and reality check are not implemented services on this commit. |

## Additional implementation inventory

No additional usable financial phone feature was found. The complete reachable UI consists of the prototype route button, the two labels and the system-driven theme. Manual tests for every reachable behaviour are MT-01–MT-10 in the guide.

| Investigated feature | Actual evidence | Access / testing | SRS treatment |
| --- | --- | --- | --- |
| Multiple goals | FakeGoalRepository holds multiple IDs and filters contributions by goal; no UI | No route; MT-16 and MT-20 stop at Home | Clarify FR-36/38/44 and §6 optional-additional-goals statement; no duplicate new ID |
| Contributions/reversals | Append-only fake records, idempotent identical retry, once-only full negative reversal | No route; MT-16/20 blocked; FakeGoalRepositoryTest exercises repository code | FR-38 already covers contributions; propose FR-59 for reversal integrity only |
| Goal/category archival and recurring deactivation | setGoalArchived, setCategoryArchived, setRecurringExpenseActive in fakes | No route; MT-12/15/16 blocked | Extend FR-25/33/44 rather than duplicate them |
| Weekly/yearly recurring intervals | Template enum and positive interval validation | No route; MT-15 blocked | Extend FR-33; occurrences/notifications still missing |
| Separate receivables | Positive-decimal/text-validated fake expected payments with archive state | No route; MT-11/17 blocked | Update FR-12/16–18 scope accurately; this is not a complete invoice system |
| Monthly/quarterly summaries | Not present | No route; MT-17/19 blocked | Retain FR-45/48/50; do not propose a new implemented quarterly feature |
| Charts/spending breakdowns | Not present | No route; MT-17/19 blocked | Existing FR-46–50 cover these |
| Crypto gains/losses | Fields and generic fake save only; no calculation or ledger screen | No route; MT-11/18 blocked | Keep FR-20–22 pending |
| Exchange-rate options/conversion | LKR/USD Money fields and rate-source enum only; no converter or API | No route; MT-11/18 blocked | Existing FR-10/11/31/56 cover these |
| Firebase account management | Dependencies/config only, no calls | No route; MT-21–31 blocked | FR-01–07/57 pending |
| Extra dashboard calculations | None | Home is text only | No new ID |
| System light/dark and Android 12+ dynamic colours | Theme.kt selects palettes from system state | Android Settings → Display, then reopen app; MT-05 | Clarify compatibility NFR; theme foundation is not a new finance FR |

## Local validation, separate from manual results

Executed from the audited repository, offline, without invoking device/instrumentation tests:

```powershell
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest
.\gradlew.bat --offline --no-daemon :app:assembleDebug
```

Both commands completed with exit code 0. Unit build: **BUILD SUCCESSFUL in 45s**. APK build: **BUILD SUCCESSFUL in 31s**. Packaging reported that `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` could not be stripped and were packaged as-is; the build still succeeded. A transitive library name is not proof of an app-integrated DataStore/Room feature.

| Existing local suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ExampleUnitTest (2+2) | 1 | 0 | 0 | 0 |
| FakeCategoryRepositoryTest | 6 | 0 | 0 | 0 |
| FakeExpenseRepositoryTest | 7 | 0 | 0 | 0 |
| FakeGoalRepositoryTest | 12 | 0 | 0 | 0 |
| FakeIncomeRepositoryTest | 7 | 0 | 0 | 0 |
| FakeReceivableRepositoryTest | 12 | 0 | 0 | 0 |
| FakeRecurringRepositoryTest | 12 | 0 | 0 | 0 |
| **Total** | **57** | **0** | **0** | **0** |

Results: [JUnit XML directory](../../app/build/test-results/testDebugUnitTest/), [HTML report](../../app/build/reports/tests/testDebugUnitTest/index.html).
APK: [app-debug.apk](../../app/build/outputs/apk/debug/app-debug.apk), package `com.example.personalfinanceapp`, version `1.0` (code 1), minimum SDK 26.
APK SHA-256: `1E87093EDC75E9BFBB46B4DC8E0D65CFB40A4C37FC0AEB6DE0DD7462FB66E9CD`.

These 57 tests verify fakes and one arithmetic sample; they do not test UI navigation, Firebase, Room, money conversion, summary calculations or device compatibility. No manual test is marked PASS.

## Change boundary

No audit edits were made to application sources, Firebase configuration/rules, migrations, existing SRS or existing project documentation. Switching/fast-forwarding main was explicitly requested/approved; this audit did not create commits, push or open a PR. The pre-existing `.idea/misc.xml` temurin-25 setting was preserved, with a backup at `C:\Users\Himasha\AppData\Local\Temp\FinClarity-misc-5155e3ac7ae44d14af98cab3879aa0b3.xml`. Builds created ignored build/cache artifacts. The four new audit documents are the only intended new project files.
