# Add Income Screen Wireframe

**SE3092 Personal Finance App** | **M02** | **18 October 2026**

*Status: approved screen specification. The screen and its ViewModel are not implemented in the current branch.*

## Purpose and scope

This document specifies the Add Income form for M02 and records the boundary of the planned M03 automatic USD/LKR exchange-rate feature. It is a UI and behaviour specification, not evidence that the screen, Firebase persistence, or online rate lookup exists.

The specification is based on the implemented [`Money`](../../app/src/main/java/com/example/personalfinanceapp/core/model/Money.kt) and [`Income`](../../app/src/main/java/com/example/personalfinanceapp/core/model/Income.kt) models, the implemented [`MoneyValidator`](../../app/src/main/java/com/example/personalfinanceapp/core/money/MoneyValidator.kt) and [`MoneyConverter`](../../app/src/main/java/com/example/personalfinanceapp/core/money/MoneyConverter.kt), and the current [`IncomeRepository`](../../app/src/main/java/com/example/personalfinanceapp/feature/income/IncomeRepository.kt) contract. It follows the [shared screen conventions](../screen-conventions.md), [architecture](../architecture.md), and proposed [Firestore schema](../firestore-schema.md).

## Current implementation boundary

| Area | Verified status in the current branch |
| --- | --- |
| Money model | Implemented. Preserves original amount/currency, LKR-per-unit rate, converted LKR minor units, rate source, and rate date. |
| Money validation and conversion | Implemented and unit tested for positive amounts, LKR conversion, manually rated USD conversion, precision, rounding, overflow, and too-small results. |
| Income model | Implemented with source, entry kind, money snapshot, transaction date, and optional P&L, project, receivable, asset, and note fields. |
| Income repository | Contract and in-memory fake implemented. The fake can observe, save/update, and delete income, but validates only that a saved income ID is non-blank. |
| Add Income UI and ViewModel | Not present. This document defines the intended screen only. |
| Firebase income persistence | Not present. The Firestore income structure is a proposed schema only. |
| Automatic USD/LKR lookup | Not present and outside M02. It is planned for M03. |

## Screen layout

Conditional areas are marked in the wireframe and are described below.

```text
+------------------------------------------------------+
| [Back]  Add Income                                   |  1
+------------------------------------------------------+
| Income source *                                      |  2
| [ Salary ] [ Freelance ] [ AdSense ] [ Crypto ]      |
|                                                      |
| [Crypto only] Result *                               |  3
| [ Profit ] [ Loss ]                                  |
|                                                      |
| [Freelance only] Project reference (optional)        |  4
| [ e.g. Website redesign                         ]    |
|                                                      |
| Income amount *                                      |  5
| [ 0.00                                           ]    |
|                                                      |
| Currency *                                           |  6
| [ LKR ] [ USD ]                                      |
|                                                      |
| [USD only] Exchange rate (LKR per USD) *              |  7
| [ 0.000000                                       ]    |
| M02: manually entered rate                           |
| M03: fetched rate, timestamp, and manual alternative |
|                                                      |
| Estimated amount in LKR                              |  8
| LKR --                                               |
|                                                      |
| Transaction date *                                   |  9
| [ YYYY-MM-DD                                    ] [v]|
|                                                      |
| Notes (optional)                                     | 10
| [                                                  ] |
| [                                                  ] |
|                                                      |
| [                  Save Income                    ]   | 11
+------------------------------------------------------+
```

The form must be vertically scrollable, remain usable above the software keyboard, and hide bottom navigation while open. The back action follows the shared form convention: an untouched form closes; a changed form asks whether to discard changes or keep editing.

## Fields and model mapping

| No. | UI element | Required behaviour | Model mapping and status |
| --- | --- | --- | --- |
| 1 | Title and back navigation | Display **Add Income** and an accessible back action. | Navigation concern; no `Income` field. Not implemented. |
| 2 | Income source | Offer Salary, Freelance, AdSense, and Crypto as a single-choice selection. | Maps exactly to `IncomeSource.SALARY`, `FREELANCE`, `ADSENSE`, and `CRYPTO`. Enum implemented; UI not implemented. |
| 3 | Profit or Loss | Display only when Crypto is selected. Exactly one option is required for the approved realised-P&L flow. | Profit maps to `PnlDirection.GAIN`; Loss maps to `PnlDirection.LOSS`; `entryKind` maps to `IncomeEntryKind.REALIZED_PNL`. Fields and enums implemented; cross-field validation is not implemented. |
| 4 | Project reference | Display for Freelance. It is optional; omit it from the model when blank. | Maps to nullable `Income.projectReference`. The model supports it. The fake repository does not validate or normalize it. |
| 5 | Income amount | Accept a positive decimal amount in the selected original currency. Loss entries still use a positive magnitude. | Stored through `Money.originalAmountDecimal`; validation is implemented. |
| 6 | Currency | Offer LKR or USD as a single-choice selection. | Maps exactly to `CurrencyCode.LKR` or `CurrencyCode.USD`. Enum implemented. |
| 7 | Exchange rate | Display only for USD. In M02 it is a required manual LKR-per-USD value. | Stored in `Money.rateLkrPerUnitDecimal`. `MoneyConverter.fromUsd` accepts only `MANUAL_TRANSFER` or `MANUAL_REFERENCE`; see the open decision below. |
| 8 | Estimated converted LKR amount | Read-only result calculated from valid amount and rate input. Show a neutral placeholder until calculation is possible. | Derived from `Money.baseAmountMinor`, which stores LKR minor units. Calculation is implemented; presentation is not. |
| 9 | Transaction date | Require a valid calendar date selected by the user. | Maps to `Income.occurredDate`. For LKR, `MoneyConverter.fromLkr` also uses this date as `Money.rateDate`. |
| 10 | Notes | Optional multiline text. | Maps to nullable `Income.note`. No maximum length or normalization rule is implemented or approved. |
| 11 | Save Income | Validate the complete form, prevent duplicate submission, build one `Income`, and request persistence. | Calls `IncomeRepository.saveIncome` in a future ViewModel. No income UI/ViewModel or production repository currently exists. |

`Income.id` is required by the repository but is not a user-facing field. The save flow must supply a stable, non-blank ID. `receivableId` and `assetSymbol` exist in the model but are not fields in this approved Add Income wireframe. Adding receivable linking or a crypto asset selector requires a separate UI requirement.

## Conditional behaviour

| Condition | Screen behaviour | Stored meaning |
| --- | --- | --- |
| Currency is LKR | Hide the exchange-rate input. Calculate using a fixed rate of `1`. | `originalCurrency = LKR`, `rateLkrPerUnitDecimal = "1"`, `rateSource = LKR_FIXED`, and `rateDate = occurredDate`. |
| Currency is USD | Show and require the exchange-rate input. Recalculate the estimate when both amount and rate are valid. | Preserve the USD amount, selected manual rate source, rate value, rate date, and calculated LKR minor units. |
| Source is Crypto | Show Profit and Loss. For this approved flow, use `REALIZED_PNL`; require Profit/Gain or Loss. | `pnlDirection = GAIN` or `LOSS`. The entered amount remains positive; direction carries the financial sign. |
| Source is not Crypto | Hide Profit and Loss. | Use `entryKind = RECEIPT` and `pnlDirection = null`. |
| Source is Freelance | Show optional Project reference. | Store a non-blank value in `projectReference`; otherwise store `null`. |
| Source is not Freelance | Hide Project reference. | Store `projectReference = null` for this screen flow. |
| Amount or required USD rate is invalid | Do not show a misleading estimate and do not save. | No `Money` or `Income` record is produced until validation succeeds. |

The models can represent combinations beyond this wireframe. The mappings above are screen rules, not validations currently enforced by the `Income` constructor or fake repository.

## Input validation

Validation should run after relevant field interaction and again on Save. Show a field-specific, understandable message, preserve the user's input, and move focus to the first invalid field when helpful. Raw exceptions and Firebase error codes must not be displayed.

| Input | Verified rule |
| --- | --- |
| Source | Required by this screen specification. No model or repository validator currently enforces it because the Kotlin enum is non-null once an `Income` is constructed. |
| Amount | Trim surrounding whitespace; require digits with an optional decimal point and one or two fractional digits; require a value greater than zero. Commas, signs, letters, and scientific notation are rejected. |
| Currency | Required by this screen specification. Only LKR and USD exist in `CurrencyCode`. |
| USD rate | Trim surrounding whitespace; require digits with an optional decimal point and one to six fractional digits; require a value greater than zero. Commas, signs, letters, and scientific notation are rejected. |
| Rate source | LKR must use `LKR_FIXED`. USD conversion currently accepts only `MANUAL_TRANSFER` or `MANUAL_REFERENCE`; `LKR_FIXED` is rejected for USD. |
| Converted amount | The calculated LKR minor-unit value must fit exactly in a `Long` and be greater than zero. A conversion that rounds to zero minor units is rejected. |
| Crypto result | Required when Crypto is selected. Use a positive amount plus `GAIN`/`LOSS`; do not encode a loss as a negative amount. |
| Transaction date | Required and represented by `LocalDate`. The repository contains no rule about future dates, so this document does not prohibit them. |
| Project reference | Optional for Freelance. No approved length or character limit exists. If supplied to the proposed Firestore shape, it should be non-blank. |
| Notes | Optional. No approved length limit exists in current Kotlin validation. |
| Income ID | Must be stable and non-blank. It is generated or supplied by the data layer, not typed by the user. |

The implemented amount error messages are **Amount cannot be empty**, **Enter a valid amount with up to 2 decimal places**, and **Amount must be greater than zero**. The implemented rate errors are **Enter a valid exchange rate with up to 6 decimal places** and **Exchange rate must be greater than zero**. The UI may use equivalent user-friendly wording but must not weaken these rules.

## Screen states

These are target behaviours for a future screen; none is implemented for income in the current branch.

| State | Expected behaviour |
| --- | --- |
| Default / ready | Render the base fields and explicit required/optional labels. Hide source-specific fields until their condition applies. The models do not define initial source, currency, or date values, so defaults must be approved during UI implementation rather than presented as existing behaviour. Do not silently save an unselected required value. |
| Loading | M02 has no online exchange-rate loading. In M03, when an automatic USD lookup is active, keep the form visible, show progress beside the rate, and disable Save until a valid fetched or manual rate is available. |
| Invalid input | Show inline field errors, retain every entered value, suppress an invalid estimate, and do not call the repository. |
| Error | For rate-service or save failures, keep the completed form visible, show a short message, and provide Retry or manual-rate action as appropriate. Do not expose implementation details. |
| Offline | M02 manual calculation can continue without a rate service. M03 must offer manual rate entry when lookup is unavailable. Persistence must show an accurate offline or pending state and must not claim cloud confirmation while a write remains pending. Firebase income persistence and pending-write behaviour are not yet implemented. |
| Saving | Keep values visible, replace or accompany the button label with **Saving**, disable repeated submissions and navigation actions that would duplicate the write, and retain the form if saving fails. |
| Success | Show a brief accessible confirmation, return to the relevant income/activity view, and display the saved record using its stored historical values. |

## Financial calculation and historical-rate rules

1. Parse money with decimal arithmetic. Do not use `Double` or `Float`.
2. For LKR, use `rate = 1`, `rateSource = LKR_FIXED`, and the transaction date as `rateDate`.
3. For USD, calculate `amount x LKR-per-USD rate x 100` and round once to the nearest LKR minor unit using `RoundingMode.HALF_UP`.
4. Store the rounded result in `baseAmountMinor`. Reject overflow and results that round to zero minor units.
5. Preserve `originalAmountDecimal`, `originalCurrency`, `rateLkrPerUnitDecimal`, `baseAmountMinor`, `rateSource`, and `rateDate` together as the transaction's `Money` snapshot.
6. Treat the displayed LKR value as an estimate before saving and as the stored historical conversion after saving.
7. Do not recalculate an existing transaction because a newer exchange rate becomes available. Historical reports must use the stored `baseAmountMinor` and rate metadata.
8. For a Crypto loss, the converter still produces a positive money magnitude. Reporting logic must use `pnlDirection = LOSS` to interpret the direction; it must not require a negative amount that the validator rejects.

Example: USD `125.50` at LKR `305.25` produces `3,830,888` LKR minor units, displayed as LKR `38,308.88`, because `125.50 x 305.25 x 100 = 3,830,887.5` and the implemented policy rounds half up.

## Relationship to `Money` and `Income`

The form produces one `Income` record containing one immutable-at-save-time `Money` snapshot:

```text
Add Income form
  source ------------------------------> Income.source
  Profit/Loss (Crypto) ----------------> Income.pnlDirection
  derived receipt/P&L kind ------------> Income.entryKind
  transaction date --------------------> Income.occurredDate
  project reference (Freelance) -------> Income.projectReference?
  notes --------------------------------> Income.note?
  amount + currency + rate metadata ----> Income.money
                                            |- originalAmountDecimal
                                            |- originalCurrency
                                            |- rateLkrPerUnitDecimal
                                            |- baseAmountMinor
                                            |- rateSource
                                            `- rateDate
```

The current fake repository does not validate that Crypto, `REALIZED_PNL`, and `pnlDirection` agree, nor that project references are limited to Freelance. The future ViewModel must enforce the screen mapping before it constructs and saves an `Income`; production persistence should also reject malformed data at its trust boundary.

## M02 and M03 responsibilities

| Responsibility | M02 | M03 |
| --- | --- | --- |
| Positive amount validation (up to 2 decimal places) | Implemented and tested | Reuse without weakening |
| LKR fixed-rate conversion | Implemented and tested | Reuse |
| Manual USD conversion (rate up to 6 decimal places) | Implemented and tested | Retain as override and fallback |
| `HALF_UP` conversion to LKR minor units | Implemented and tested | Reuse |
| Historical `Money` snapshot | Model and converter implemented | Preserve for automatic and manual rates |
| Add Income wireframe and behaviour specification | Defined by this document | Extend only for automatic-rate status and controls |
| Add Income Compose screen and ViewModel | Not present in the current branch | Not made complete by this documentation task |
| Online USD/LKR API integration | Out of scope; do not call an API | Planned |
| Fetched rate and update timestamp | Out of scope | Retrieve a recent reference rate and display its update timestamp |
| Manual alternative when online lookup is available | Manual entry is the only M02 path | Allow the user to replace the fetched rate with a manual rate |
| Service-unavailable behaviour | No service dependency | Provide a clear error/offline state and manual fallback |
| Repricing old transactions | Never | Never |

### Planned M03 automatic-rate flow

When USD is selected in M03:

1. Request a recent USD/LKR reference rate from the approved exchange-rate service.
2. Show an in-place loading state while keeping the rest of the form usable.
3. Display the fetched LKR-per-USD rate and the service's update timestamp.
4. Let the user choose manual entry instead of the fetched value.
5. If the service fails or the device is offline, explain the problem and provide the manual input immediately.
6. Calculate and display the estimated LKR amount using the currently selected valid rate.
7. On Save, preserve the exact selected rate, its source, its applicable date, and the resulting LKR minor amount with the transaction.
8. Never update a saved transaction from a later lookup.

This flow is planned, not implemented. No API provider, endpoint, freshness window, caching policy, retry policy, or network client has been selected in the repository.

## Open decisions and known inconsistencies

- The approved M02 controls include one manual exchange-rate input but no control that distinguishes `MANUAL_TRANSFER` from `MANUAL_REFERENCE`. `MoneyConverter.fromUsd` requires one of them. The team must either add a source choice or approve a deterministic mapping before implementing the screen; the UI must not guess silently.
- `RateSource` currently contains only `LKR_FIXED`, `MANUAL_TRANSFER`, and `MANUAL_REFERENCE`. It has no value that truthfully identifies an automatically fetched API rate. M03 needs a reviewed model and persistence decision before automatic rates can be stored. This M02 documentation task does not modify `RateSource`.
- `Money.rateDate` is a `LocalDate`, while M03 must display a fetched update timestamp. The service timestamp may be transient display metadata, or M03 may require a reviewed persistence change. The current model cannot preserve a full timestamp.
- The USD converter accepts a separate `rateDate`, but the approved M02 layout has no rate-date field. If the manual rate's date is not the transaction date, the UI needs an additional approved rule or field. Until then, the implementation must not claim that a separately dated rate was captured.
- No rule currently decides whether future transaction dates are allowed, and no note/project-reference length limits are defined.
- Existing architecture and Firestore documents still describe the M02 converter and some integration work as unfinished. The Kotlin source and unit tests on this branch are the authoritative evidence that money validation/conversion is now implemented; those documents were intentionally not changed by this task.

## Acceptance checklist

### Documentation acceptance

- [x] Shows the approved title, back action, source choices, amount, currency, conditional USD rate, LKR estimate, date, notes, and Save action.
- [x] Documents conditional Crypto Profit/Loss and optional Freelance project reference behaviour.
- [x] Uses terminology that maps to the current Kotlin enums and fields.
- [x] Separates implemented domain logic from the unimplemented screen and persistence layers.
- [x] Separates M02 manual conversion from M03 automatic lookup.
- [x] Records exact implemented amount, rate, rounding, overflow, and historical-snapshot rules.
- [x] Covers default, loading, invalid, error, offline, saving, and success states.
- [x] Identifies unresolved requirements without presenting them as implemented behaviour.

### Future M02 screen implementation acceptance

- [ ] Source choices map exactly to `IncomeSource` values.
- [ ] LKR hides rate input and uses the fixed rate/source/date rules.
- [ ] USD requires a valid manual rate and an approved manual `RateSource` mapping.
- [ ] Amount, rate, converted-minor-unit, and overflow rules match the tested money utilities.
- [ ] Crypto requires Profit/Gain or Loss and stores a positive amount with the correct direction.
- [ ] Freelance alone exposes the optional project reference in this flow.
- [ ] Invalid input remains visible with field-specific messages and cannot be saved.
- [ ] Saving prevents duplicate submissions; failure retains input; success shows the stored record.
- [ ] Accessibility, scrolling, keyboard, focus, and back/discard behaviour follow shared conventions.
- [ ] Historical money values are saved as a snapshot and are not repriced later.

### Future M03 acceptance

- [ ] A recent USD/LKR reference rate and update timestamp are displayed when lookup succeeds.
- [ ] The user can override the fetched rate with a valid manual rate.
- [ ] Offline and service failures provide a manual fallback.
- [ ] The estimate always reflects the currently selected rate.
- [ ] The selected rate, truthful source, applicable date, and LKR minor amount are preserved.
- [ ] Previously saved transactions are never recalculated from newer rates.
- [ ] The API rate-source and timestamp model gaps are resolved through reviewed design changes before implementation.
