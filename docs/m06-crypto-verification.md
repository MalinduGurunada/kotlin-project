# M06 Realised Crypto Gains and Losses Verification

Date: 2026-11-03  
Branch: `feat/malindu/m06-crypto-gains-losses`  
Base: `0aee0c6` (`dev` and the M06 branch pointed to the same commit before implementation)

## Scope and assumptions

Complete Jira acceptance-criteria text was not present in the repository and no Jira connection was
available. Implementation therefore follows the supplied PBD-45, PBD-46, and PBD-47 requirements
with these explicit assumptions:

- realised result = sale proceeds - original cost basis - fees;
- proceeds, basis, and fees share the selected transaction currency and historical rate snapshot;
- zero is stored as `BREAK_EVEN`;
- existing Crypto documents without component details remain readable, valid, and editable;
- only the net realised result participates in income totals; sale proceeds and withdrawals are not
  additional receipts;
- one stable draft/document ID makes an identical retry an overwrite of the same owner-scoped
  document, not a duplicate.

M09 is safely committed at `03a3dde`, one commit ahead of the M06 base. It changes only
`NavigationPolicyTest.kt` and `docs/m09-integration-verification.md`, so M06 correctly starts from
`dev` without modifying or depending on the M09 branch.

## PBD-45 — recording realised results

Implemented:

- `CryptoRealizedDetails` records original-currency cost basis, sale proceeds, and fees.
- The editor derives gain, loss, or break-even instead of asking the user to enter a signed result.
- LKR and USD results retain the existing historical exchange-rate snapshot pattern.
- Firestore mapper and rules accept the new exact nested map while preserving legacy documents.
- Firebase ownership remains `users/{uid}/incomes/{incomeId}` and owner-bound repository methods are
  unchanged.
- Gross proceeds are metadata only; the single Income record stores the derived net magnitude.

## PBD-46 — monthly and year-to-date results

Implemented:

- A shared signed-amount function adds gains, subtracts losses, and contributes zero for break-even.
- `CryptoResultCalculator` applies inclusive-start/exclusive-end date bounds and exact arithmetic.
- Income monthly totals, dashboard monthly income, analytics source totals, dashboard monthly Crypto,
  and dashboard YTD Crypto use net signed values.
- Dashboard YTD observation starts on January 1 and ends after the current date.
- The Overview screen displays monthly and YTD realised-Crypto net results.

## PBD-47 — verification outcomes

| Verification | Expected | Actual |
| --- | --- | --- |
| Focused income/dashboard tests | Profit, loss, zero, fees, multiple records, date bounds, mapping, validation, stable-ID retry, persistence gateway, auth switching, UID isolation, ViewModel and dashboard pass | PASS — 259 tests |
| Firestore rules emulator | New/legacy schema, break-even, UID isolation, same-ID retry, malformed input pass | PASS — 28 tests; 69 allowed + 114 denied assertions |
| Debug source compilation | Kotlin/Hilt integration compiles | PASS — `compileDebugKotlin` |
| Android emulator repository suite | Production repository persistence, retry, realtime/account isolation | Added/compiled test coverage; runtime requires an Android Emulator plus local Auth and Firestore emulators |
| Compose editor instrumentation | New Crypto fields and errors render | Updated test coverage; runtime requires an Android Emulator |
| Debug APK | `assembleDebug` | PASS |

Existing focused tests also verify duplicate submission suppression and retry reuse of the same stable
income ID. Firestore rules tests run only against project `demo-personal-finance-income-rules`; no
production Firebase data is accessed.

## Remaining manual checks

- On an Android Emulator, start Auth and Firestore emulators and run
  `FirestoreIncomeRepositoryEmulatorTest`.
- Enter LKR and USD Crypto gain/loss/break-even examples and visually verify the conversion preview,
  list row, Overview monthly/YTD card, and edit flow.
- Confirm a sale-related bank withdrawal is not entered again as Income; only the realised Crypto
  record should affect income totals.

## Limitations

- The model intentionally does not implement holdings, lots, unrealised gains, trading, exchange
  imports, or portfolio tracking.
- Firestore rules validate schema, ownership, zero-result constraints, and supported types. Exact
  decimal arithmetic is enforced by the app validator because Firestore Rules cannot reliably
  recompute the decimal result.
- Legacy Crypto records retain their previously entered result and direction because their original
  proceeds, basis, and fees cannot be reconstructed.
