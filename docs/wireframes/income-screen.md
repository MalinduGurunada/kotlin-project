# Income List and Add/Edit Wireframes

**SE3092 Personal Finance App** | **M03** | **26 October 2026**

*Status: implemented. This document describes the current Income list, editor, money, exchange-rate, persistence, and ownership behaviour.*

## Purpose and scope

The Income feature lists the signed-in user's income for a selected month and supports Add, Edit, and Delete workflows. The editor accepts LKR or USD, supports automatic and manual USD/LKR rates, and preserves the selected conversion as historical `Money` data.

The implementation is defined by [`IncomeListScreen`](../../app/src/main/java/com/example/personalfinanceapp/feature/income/ui/IncomeListScreen.kt), [`IncomeEditorScreen`](../../app/src/main/java/com/example/personalfinanceapp/feature/income/ui/IncomeEditorScreen.kt), [`IncomeViewModel`](../../app/src/main/java/com/example/personalfinanceapp/feature/income/ui/IncomeViewModel.kt), the money utilities, and the owner-bound Firestore income repository.

## Current implementation boundary

| Area | Implemented behaviour |
| --- | --- |
| Income list | Observes one half-open monthly date range, renders loading/empty/error/success states, shows net LKR income, and supports previous/next month navigation. |
| Add/Edit | Uses one editor with mode-specific title and save label. Add creates a stable draft ID; Edit preserves the selected record ID and immutable original record. |
| Delete | Uses a confirmation dialog, disables duplicate confirmation while deleting, and retains safe retryable error text. |
| Money | Preserves original amount/currency, LKR-per-unit rate, converted LKR minor units, rate source, and rate date. |
| USD rates | Supports automatic API reference rates plus manual actual-transfer and manual-reference rates, including refresh, retry, and manual fallback. |
| Persistence | Uses a schema-versioned Firestore mapper and UID-scoped observation, save, and delete paths. |
| Ownership | Observation snapshots carry their authenticated owner. Edit and Delete validate the snapshot owner and exact record; writes use owner-bound repository methods. |

## Screen layouts

### Income list

```text
+------------------------------------------------------+
| [Back]  Income                                       |
+------------------------------------------------------+
| [Previous]          October 2026             [Next]  |
|                                                      |
| Net income                                           |
| LKR 250,000.00                                      |
|                                                      |
| Salary                                               |
| Oct 5, 2026                    LKR 150,000.00         |
|                                      [Edit] [Delete] |
|                                                      |
| AdSense                                              |
| Oct 20, 2026                          USD 100.00      |
|                                      [Edit] [Delete] |
|                                                      |
|                                         [Add Income] |
+------------------------------------------------------+
```

Each row shows the original amount and currency, source, transaction date, and any stored project, asset, or note text. Rows do not show converted LKR, exchange-rate source, or rate date; the stored LKR values are used only for the net summary. The list also shows loading, empty, and recoverable error states. Delete opens a confirmation dialog with source, original amount, and transaction date. A pending deletion cannot be submitted or cancelled twice.

### Add/Edit editor

```text
+------------------------------------------------------+
| [Back]  Add Income / Edit Income                     |
+------------------------------------------------------+
| Currency *                 ( LKR ) ( USD )            |
| Income source *            [ Salary              v ] |
| Amount *                   [ 0.00                  ] |
|                                                      |
| [USD only] Rate mode       ( Automatic ) ( Manual )  |
| Automatic: [Refresh/Retry reference rate]            |
|             [Enter a manual rate] on lookup failure  |
| Manual:    LKR per USD [                         ]    |
|            Type (Actual transfer) (Reference)        |
|            [Rate date: YYYY-MM-DD]                   |
|                                                      |
| Selected rate / converted LKR / source / rate date   |
| [Transaction date: YYYY-MM-DD]                       |
| [Freelance] Project reference (optional)             |
| [Crypto] Realized result (Gain) (Loss)                |
| [Crypto] Asset symbol (optional)                     |
| Note (optional)                                      |
|                                                      |
|                         [Cancel] [Save Income/Changes]|
+------------------------------------------------------+
```

The editor scrolls vertically. Back and Cancel close an unsaved editor immediately; there is no dirty-form discard-confirmation dialog. Back, Cancel, fields, and Save are disabled while a save is active.

## Fields and model mapping

| UI element | Behaviour | Model mapping |
| --- | --- | --- |
| Mode/title | **Add Income** for a new draft; **Edit Income** for an observed record. | `IncomeEditorMode.ADD` or `EDIT`; Edit retains `Income.id`. |
| Currency | LKR or USD. Add defaults to LKR. | `Money.originalCurrency`. |
| Income source | Salary, Freelance, AdSense, or Crypto. Add defaults to Salary. | `Income.source`. |
| Amount | Positive original-currency amount with at most two fractional digits. | `Money.originalAmountDecimal`. |
| Transaction date | Required calendar date; Add defaults to the current date. | `Income.occurredDate`; also the LKR `rateDate`. |
| USD rate mode | Automatic API reference or Manual. | Selects how the USD `Money` snapshot is prepared. |
| Manual rate | Positive LKR-per-USD rate with at most six fractional digits. | `Money.rateLkrPerUnitDecimal`. |
| Manual rate type | Actual transfer or Reference. | `MANUAL_TRANSFER` or `MANUAL_REFERENCE`. |
| Manual rate date | Required for manual USD; follows the transaction date until explicitly selected. | `Money.rateDate`. |
| Conversion preview | Displays selected rate, converted LKR amount, rate source, and stored rate date. | Derived from the candidate `Money` snapshot. |
| Project reference | Optional and visible only for Freelance. | Trimmed to `Income.projectReference`, or `null` when blank/not applicable. |
| Realized result | Gain or Loss, required only for Crypto. | `Income.entryKind = REALIZED_PNL` and `Income.pnlDirection`. |
| Asset symbol | Optional and visible only for Crypto. | Trimmed to `Income.assetSymbol`, or `null`. |
| Note | Optional multiline text. | Trimmed to `Income.note`, or `null`. |

`Income.id` and the owner ID are not user-editable. Add generates one UUID when the draft opens and reuses it across retries. Edit uses the selected record ID and preserves an untouched `receivableId`.

## Conditional behaviour

| Condition | Screen behaviour | Stored meaning |
| --- | --- | --- |
| Currency is LKR | Hide USD controls; use a fixed rate without network access. | Rate `1`, source `LKR_FIXED`, rate date equal to transaction date. |
| Currency is USD with Automatic selected | Request a validated recent USD/LKR quote; show loading, refresh/retry, safe errors, and manual fallback. | Source `EXCHANGE_RATE_API_REFERENCE`; preserve quote rate and applicable date. |
| Currency is USD with Manual selected | Show rate, rate type, and rate-date controls. | Source `MANUAL_TRANSFER` or `MANUAL_REFERENCE`; preserve the selected date. |
| Source is Crypto | Show Gain/Loss and optional asset symbol. | Positive money magnitude with `REALIZED_PNL`; `pnlDirection` carries the sign. |
| Source is Freelance | Show optional project reference. | Non-blank trimmed value or `null`. |
| Source is Salary or AdSense | No source-specific metadata fields. | `RECEIPT`, with project/asset/P&L fields cleared. |
| Existing USD record opens | Hydrate the stored rate snapshot without a network request. | Untouched money remains byte-for-byte equivalent at the model level. |
| Existing USD amount changes | Recalculate with the preserved historical rate, source, and rate date. | The record is not silently repriced. |
| User explicitly refreshes an API-rated edit | Request and validate a new quote. | A successful refresh replaces the candidate snapshot only when the user saves. |

The editor's conversion preview displays `Money.rateDate`, which is the applicable date persisted with the transaction. Income list rows do not display rate metadata. The UI does **not** display or persist the provider's full `providerUpdatedAt` timestamp.

## Input validation and errors

Validation occurs while rate/amount inputs change and again on Save. Invalid input stays visible and no repository write occurs.

| Input | Implemented rule |
| --- | --- |
| Amount | Trim whitespace; digits with optional one or two fractional digits; greater than zero. Commas, signs, letters, and scientific notation are rejected. |
| USD rate | Trim whitespace; digits with optional one to six fractional digits; greater than zero. |
| Rate source | LKR requires `LKR_FIXED`; USD accepts `MANUAL_TRANSFER`, `MANUAL_REFERENCE`, or `EXCHANGE_RATE_API_REFERENCE`. |
| Converted amount | Exact decimal calculation must fit a `Long` and round to a positive LKR minor-unit value. |
| Transaction date | Required `LocalDate`. No rule prohibits a future date. |
| Crypto result | Gain or Loss is required; negative amount input is not used. |
| Income ID | Stable and non-blank; Firestore-compatible IDs are enforced at the persistence boundary. |
| Snapshot consistency | `IncomeValidator` recalculates money and rejects mismatched stored minor units, invalid LKR rate/date/source, and invalid entry-kind/P&L combinations. |

Automatic-rate failures are mapped to safe network, timeout, rate-limit, stale-quote, or unavailable messages. Save failures retain the complete form and show a generic safe message. Raw Firebase and provider details are not displayed.

## Screen and operation states

| State | Implemented behaviour |
| --- | --- |
| List loading | Clears prior financial values and shows progress. |
| List empty | Shows a zero total and no-records message. |
| List error | Shows a safe message and Retry. |
| Editor ready | Add uses Salary/LKR/current-date defaults; Edit hydrates the observed record. |
| Rate loading | Keeps the form visible, shows progress, and disables Save until valid money is available. |
| Invalid input | Shows field/form errors and prevents persistence. |
| Saving | Keeps values visible, shows **Saving**, and blocks duplicate submission/navigation. |
| Save failure | Retains all input and stable ID for retry. |
| Save success | Closes the editor and shows an Add/Edit success message. |
| Deleting | Keeps the confirmation visible and blocks duplicate confirmation/cancellation. |
| Delete failure | Retains the selected record and shows a safe missing/permission/network message. |

## Financial calculation and historical-rate rules

1. Parse amounts and rates with `BigDecimal`; never use `Double` or `Float` for financial calculation.
2. Amounts allow at most two fractional digits; USD rates allow at most six.
3. For LKR, use rate `1`, `LKR_FIXED`, and the transaction date as `rateDate`.
4. For USD, calculate `amount × LKR-per-USD rate × 100` and round once to an LKR minor unit with `RoundingMode.HALF_UP`.
5. Store the result in `baseAmountMinor`; reject overflow and values that round to zero.
6. Preserve `originalAmountDecimal`, `originalCurrency`, `rateLkrPerUnitDecimal`, `baseAmountMinor`, `rateSource`, and `rateDate` as one historical snapshot.
7. The Income list's net total uses stored `baseAmountMinor`; it does not reprice old records from a newer quote.
8. Crypto losses retain a positive money magnitude and are subtracted according to `pnlDirection = LOSS`.

Example: USD `125.50` at LKR `305.25` produces `3,830,888` LKR minor units because `125.50 × 305.25 × 100 = 3,830,887.5` and the midpoint rounds up. USD `1.00` at `100.004999` produces `10,000` minor units because `10,000.4999` is below the midpoint.

## Ownership and persistence

Income data is stored at `users/{uid}/incomes/{incomeId}` with mapper schema version `1`.

- Observation snapshots include the UID whose listener produced them and copy their record list.
- The ViewModel accepts only snapshots owned by the current authenticated UID.
- Edit and Delete callbacks carry the immutable observation owner.
- Opening Edit or Delete requires a successful current snapshot, matching callback/snapshot/authentication owners, and exact record membership.
- Drafts retain their verified owner. Saves call `saveIncomeForOwner`; deletes call `deleteIncomeForOwner`.
- The Firestore repository rechecks the current authenticated UID against the expected owner before constructing the write path.
- Authentication generation checks clear active editor/delete state and suppress stale operation completions after account changes.

## M02 and M03 responsibilities

| Responsibility | M02 result | M03 result |
| --- | --- | --- |
| Positive amount validation | Implemented | Reused in the editor and persistence validation. |
| LKR conversion and minor units | Implemented | Reused without weakening. |
| Manual USD conversion | Implemented | Exposed with explicit transfer/reference source and rate date. |
| `HALF_UP` rounding and overflow protection | Implemented and tested | Reused by automatic/manual editor flows. |
| Historical `Money` snapshot | Implemented | Preserved through editing, mapping, Firestore, list totals, and retries. |
| Income UI | Wireframe defined | List, Add/Edit, Delete, states, and navigation implemented. |
| Automatic USD/LKR reference | Planned | Remote provider, validation, loading, refresh, retry, and fallback implemented. |
| Owner-bound persistence | Planned | Authenticated observation/save/delete and security rules implemented and tested locally. |

## Resolved decisions and current limitations

- Manual rate type is explicit: **Actual transfer** maps to `MANUAL_TRANSFER`; **Reference** maps to `MANUAL_REFERENCE`.
- Automatic quotes map to `EXCHANGE_RATE_API_REFERENCE`.
- Manual rate date is an explicit editor field. Automatic snapshots use the validated quote's applicable date.
- The provider's full update timestamp is used for quote validation but is not part of `Money` and is not displayed by the Income editor; the stored/displayed historical field is `rateDate`.
- Future transaction dates and note/project/asset length limits are not currently prohibited by the domain validator.
- Back and Cancel do not prompt to discard dirty input.
- Local source and tests do not prove that Firebase rules or indexes have been deployed to a remote project.

## Acceptance checklist

- [x] Income list covers loading, empty, error/retry, monthly navigation, totals, Add, Edit, and Delete.
- [x] Add/Edit covers LKR/USD, every income source, conditional fields, validation, saving, errors, and stable IDs.
- [x] Automatic USD rates support loading, refresh, retry, and manual fallback.
- [x] Manual USD rates capture value, truthful source, and explicit rate date.
- [x] Financial conversion uses exact decimals, `HALF_UP`, positive minor units, and overflow rejection.
- [x] Historical LKR/USD snapshots survive mapping, persistence, editing, and retries without implicit repricing.
- [x] Owner-bound snapshots and writes prevent stale cross-account Edit/Delete actions.
- [x] Documentation distinguishes stored `rateDate` from the provider update timestamp.
- [x] Documentation does not claim a dirty-form discard confirmation that the UI does not implement.
