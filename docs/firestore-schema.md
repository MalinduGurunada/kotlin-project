# Firestore Schema and Security

This document distinguishes the implemented M03 Income persistence contract from schema ideas for features that still use in-memory repositories. A checked-in rule or index file is repository evidence only; deployment is not asserted here.

For application behavior, see [Application Architecture](architecture.md) and the [Income wireframe](wireframes/income-screen.md).

## Verified Firebase status

| Item | State in this branch |
| --- | --- |
| Firebase Authentication | Implemented through `FirebaseAuthRepository` and Hilt |
| Cloud Firestore | Used by production Income and Expense repositories |
| Income path | Implemented at `users/{uid}/incomes/{incomeId}` |
| Income mapper | Explicit version-1 map conversion with exact fields and validation |
| Income observation | Auth-aware date-range snapshot listener with owner-tagged snapshots |
| Income writes/deletes | Stable document IDs and expected-owner checks before the UID-scoped path is used |
| `firestore.rules` | Checked in; includes owner checks and Income schema validation |
| Security-rule tests | Checked in at `test/firestore.rules.test.js` |
| `firestore.indexes.json` | Checked in; currently contains Expense indexes |
| Rules/index deployment | Not verifiable from this repository and not claimed |

## Document hierarchy

```text
users/{uid}
|-- incomes/{incomeId}          implemented
|-- expenses/{expenseId}        implemented repository/rules path
|-- categories/{categoryId}     proposed Firestore path
|-- receivables/{receivableId}  proposed Firestore path
|-- recurringExpenses/{id}      proposed Firestore path
`-- goals/{goalId}              proposed Firestore path
    `-- contributions/{id}      proposed Firestore path
```

The active Firebase Authentication UID is the ownership boundary. Income repository code does not accept an arbitrary UID as authority: owner-bound operations compare their expected owner with the current authenticated UID before writing below `users/{uid}`.

Preferences are intentionally omitted. UID-scoped Android DataStore remains the intended authority unless a separate cloud-synchronization design is approved.

## Shared storage conventions

### IDs

- The path `{uid}` is the authenticated Firebase UID.
- `{incomeId}` is the stable `Income.id`; the mapper requires the stored `id` to match the Firestore document ID.
- Add generates one UUID before saving. Edit and retry retain the existing ID.
- Income IDs must be non-blank valid Firestore document IDs and no longer than the Firestore UTF-8 limit enforced by the repository/mapper.

### Dates

Income date-only values are stored as zero-padded ISO `YYYY-MM-DD` strings. `IncomeFirestoreMapper` parses them to `LocalDate` and rejects malformed or impossible dates. Lexicographic ordering therefore matches calendar ordering.

### Money

Income stores `Money` as a nested map:

| Field | Firestore type | Contract |
| --- | --- | --- |
| `originalAmountDecimal` | string | Positive decimal with at most two fractional digits |
| `originalCurrency` | string | `LKR` or `USD` |
| `rateLkrPerUnitDecimal` | string | Positive decimal; USD supports at most six fractional digits |
| `baseAmountMinor` | integer | Positive LKR minor units stored as a signed 64-bit value |
| `rateSource` | string | `LKR_FIXED`, `MANUAL_TRANSFER`, `MANUAL_REFERENCE`, or `EXCHANGE_RATE_API_REFERENCE` |
| `rateDate` | string | Historical ISO date associated with the stored rate |

Financial values are never persisted as Firestore doubles. `MoneyConverter` uses `BigDecimal`, multiplies amount by the LKR-per-unit rate and `100`, rounds once to an integer minor unit with `HALF_UP`, and rejects `Long` overflow. For LKR, the rate is exactly `1`, the source is `LKR_FIXED`, and `rateDate` equals `occurredDate`.

For USD, the saved rate can represent an actual transfer (`MANUAL_TRANSFER`), a manually entered reference (`MANUAL_REFERENCE`), or the supported automatic provider (`EXCHANGE_RATE_API_REFERENCE`). The automatic provider's full update timestamps are used to validate freshness before selection. Income persists the quote's applicable `rateDate`; the editor preview displays it, list rows do not, and `providerUpdatedAt` is neither persisted nor displayed.

Stored original amount/currency, rate, source, rate date, and LKR minor result form a historical snapshot. Editing an existing record does not replace that snapshot merely because a newer quote exists. `IncomeValidator` recalculates the LKR minor value and requires exact agreement before mapping or writing.

## Implemented Income collection

### `users/{uid}/incomes/{incomeId}`

**Status: IMPLEMENTED.** The production Income repository, mapper, observer, rules, and rule tests use this path and schema.

**Purpose:** Confirmed Salary, Freelance, AdSense, and realised crypto profit/loss entries.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `schemaVersion` | integer | Yes | Exactly `1` |
| `id` | string | Yes | Must equal `{incomeId}` |
| `source` | string | Yes | `SALARY`, `FREELANCE`, `ADSENSE`, or `CRYPTO` |
| `entryKind` | string | Yes | `RECEIPT` or `REALIZED_PNL`, constrained by source |
| `money` | map | Yes | Exact six-field shape defined above |
| `occurredDate` | string | Yes | ISO date-only string |
| `pnlDirection` | string | Conditional | `GAIN` or `LOSS`; required for Crypto and absent for regular receipts |
| `projectReference` | string | Optional | Current UI exposes it for Freelance and stores blank input as absent |
| `receivableId` | string | Optional | Domain link; not entered by the current Income editor |
| `assetSymbol` | string | Optional | Optional Crypto asset symbol |
| `note` | string | Optional | User note |

The mapper accepts only the required fields plus the documented optional fields and rejects unknown fields. It validates schema version, types, supported enum names, ISO dates, document/path ID equality, and the domain/money contract.

Example:

```json
{
  "schemaVersion": 1,
  "id": "income-example-001",
  "source": "FREELANCE",
  "entryKind": "RECEIPT",
  "money": {
    "originalAmountDecimal": "125.50",
    "originalCurrency": "USD",
    "rateLkrPerUnitDecimal": "305.25",
    "baseAmountMinor": 3830888,
    "rateSource": "MANUAL_TRANSFER",
    "rateDate": "2026-10-16"
  },
  "occurredDate": "2026-10-16",
  "projectReference": "example-project",
  "note": "Settled invoice"
}
```

The stored minor amount follows exact decimal arithmetic: `125.50 * 305.25 * 100 = 3,830,887.5`, which is `3,830,888` after `HALF_UP` rounding.

## Income observation and operations

### Query

`IncomeFirestoreObserver` listens below the authenticated user's Income collection with:

- `occurredDate >= startDate`;
- `occurredDate < endDateExclusive`;
- ascending `occurredDate`; then
- ascending document ID for deterministic ties.

Authentication changes cancel the old listener. The observer emits `IncomeObservationSnapshot` values containing both the immutable owner and copied Income list; sign-out/account switching clears previous-owner data before a new owner's snapshot is exposed. Snapshot or mapping failures propagate as errors rather than being converted to an empty result.

The current `firestore.indexes.json` contains Expense composite indexes and no explicit Income composite. If Firestore reports that the implemented Income query requires an additional composite index, add the generated exact definition and review it before deployment; do not document an unverified deployed index.

### Save

`saveIncomeForOwner(expectedOwnerId, income)`:

1. Rejects a blank expected owner.
2. Validates the complete Income and serializes the exact version-1 map.
3. Reads the latest authenticated UID.
4. Rejects the operation if that UID differs from the form's owner.
5. Uses `set` on `users/{verifiedUid}/incomes/{stableIncomeId}`.

This makes retries idempotent with respect to the document ID and prevents an authentication switch from redirecting an existing draft to another account.

### Delete

`deleteIncomeForOwner(expectedOwnerId, incomeId)` performs the same blank-owner, latest-authentication, and path checks. Its Firestore transaction fails when the document no longer exists and otherwise deletes the stable document. The ViewModel additionally requires the callback owner and exact selected record to belong to the current successful snapshot before opening Delete confirmation.

### Edit ownership and history

The Edit callback carries the owner of the observation snapshot that rendered the row. `IncomeViewModel` opens a draft only if the current state is successful, snapshot owner is valid and matches authentication, callback owner matches the snapshot, and the complete selected `Income` equals a current snapshot record. The draft retains that verified owner for save. Matching IDs alone are insufficient, so a stale User A row cannot target User B's same-ID document.

## Firestore security

### Checked-in rules

`firestore.rules` requires a non-null authenticated user whose UID matches `{uid}`. Income reads are limited to that user's collection; create/update requires the exact supported field set, schema version 1, path/field ID agreement, allowed enums, positive typed money, valid rate-source/currency relationships, and source/entry-kind/P&L relationships. Delete is also owner-only. Unmatched paths are denied.

Rules provide the server-enforced ownership and shape boundary. The Android converter, validator, mapper, ViewModel, and repository enforce stronger application invariants such as exact `BigDecimal` recalculation, actual calendar-date parsing, current-snapshot membership, and owner continuity.

### Rule tests and deployment boundary

`test/firestore.rules.test.js` exercises authenticated owner access, unauthenticated/cross-user denial, Income field/type/enum/money constraints, supported rate sources including `EXCHANGE_RATE_API_REFERENCE`, update validation, and delete ownership. Local rule tests and checked-in files do not prove deployment to a Firebase project.

### Cross-feature security requirements and status

The general client security contract is:

- require a non-null Firebase Authentication identity and `request.auth.uid == uid` for every user-owned path;
- query below the authenticated user's path, because rules authorize a query as a whole rather than filtering returned documents;
- deny unmatched paths by default;
- validate allowed fields, Firestore types, enum values, ID/path agreement, date formats, string bounds, and numeric ranges on creates and updates;
- prevent cross-user reads, queries, writes, and forged ownership fields;
- preserve append-only history where required, including Goal contribution reversals; and
- keep privileged administration outside the Android client.

| Area | Repository/rules status |
| --- | --- |
| Income ownership and schema | **IMPLEMENTED** in repository, mapper, rules, and local rule tests |
| Expense ownership | **IMPLEMENTED** in repository path and rules; rule tests cover owner/cross-user access |
| Expense schema validation | **PARTIAL**; current rules validate selected required fields but do not enforce an exact field set or the full `Money` contract |
| Category, Receivable, Recurring, Goal, Contribution | **PROPOSED** Firestore schemas; unmatched paths remain denied and no Firebase repositories exist |
| Rule/index deployment | **UNVERIFIED**; source-controlled files do not prove remote deployment |

### Logout and switching

Navigation returns a signed-out user to Login. Income listeners are cancelled/switched, prior owner data is cleared, and the ViewModel's authentication generation invalidates owner-specific drafts, delete state, and stale async completions. Firestore local-cache policy and deployed-environment verification remain release concerns, but cached records are not treated as authority for a different UID.

## Implemented Expense collection

### `users/{uid}/expenses/{expenseId}`

**Status: IMPLEMENTED, with the limitations below.** `FirestoreExpenseRepository` is the Hilt production binding and `firestore.rules` authorizes this owner-scoped path. Unlike Income, Expense has no dedicated versioned mapper and uses a flat document shape.

**Purpose:** Confirmed expense transactions.

| Field | Firestore type | Written by current repository | Notes |
| --- | --- | --- | --- |
| `id` | string | Yes | Stable `Expense.id`; current rules do not require equality with `{expenseId}` |
| `userId` | string | Yes | Must equal `{uid}` under current create/update rules |
| `categoryId` | string | Yes | Category reference; current rules require non-blank |
| `categoryNameSnapshot` | string | Yes | Historical category label |
| `spendingClass` | string | Yes | `COMMITTED` or `DISCRETIONARY` |
| `paymentChannel` | string | Yes | `CASH`, `CARD`, `BANK`, or `OTHER` |
| `originalAmountDecimal` | string | Yes | Flat `Money.originalAmountDecimal` value |
| `originalCurrency` | string | Yes | `LKR` or `USD` in the Kotlin model |
| `rateLkrPerUnitDecimal` | string | Yes | Flat historical rate value |
| `baseAmountMinor` | integer | Yes | LKR minor units; current rules allow zero or positive values |
| `rateSource` | string | Yes | Stored enum name |
| `rateDate` | string | Yes | ISO date string written by the repository |
| `occurredDate` | string | Yes | ISO date string |
| `note` | string or null | Yes | Optional user note |
| `channelLabel` | string or null | Yes | Optional payment-channel detail |

**Relationships:** `categoryId` identifies the intended user-owned Category. `categoryNameSnapshot` and `spendingClass` preserve the historical classification even if that Category later changes.

**Current behavior and validation:** The repository scopes reads/writes to the current Firebase UID and rejects blank IDs on save. It listens to the entire Expense collection, maps documents permissively, drops records that cannot be parsed, and filters/sorts the requested date range in memory. Current rules verify owner, `userId`, selected required strings/enums, an integer non-negative `baseAmountMinor`, and string `occurredDate`; they do not validate the exact field set, complete rate/currency consistency, ISO calendar validity, or category existence. This is less strict than the Income persistence boundary and must not be documented as equivalent.

Example of the implemented flat shape:

```json
{
  "id": "expense-example-001",
  "userId": "example-auth-uid",
  "categoryId": "food",
  "categoryNameSnapshot": "Food",
  "spendingClass": "DISCRETIONARY",
  "paymentChannel": "CASH",
  "originalAmountDecimal": "1500.00",
  "originalCurrency": "LKR",
  "rateLkrPerUnitDecimal": "1",
  "baseAmountMinor": 150000,
  "rateSource": "LKR_FIXED",
  "rateDate": "2026-10-16",
  "occurredDate": "2026-10-16",
  "note": "Lunch",
  "channelLabel": null
}
```

## Proposed collections

The following sections preserve the model-aligned design for features without Firestore repositories. **PROPOSED** means no production mapper/repository or allow rule exists; the checked-in default deny therefore blocks these paths.

### `users/{uid}`

**Status: INFRASTRUCTURE PATH; application document shape UNVERIFIED.** The path is the parent ownership boundary. There is no Kotlin `User` model or repository, and current feature code does not require a root document to exist. Checked-in rules allow an owner to access their root document but do not enforce the illustrative fields below.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `uid` | string | Proposed | Would equal the document ID and authenticated UID |
| `createdAt` | timestamp | Proposed | Prefer a trusted server timestamp if introduced |

### `users/{uid}/categories/{categoryId}`

**Status: PROPOSED.** Only `FakeCategoryRepository` exists.

**Purpose:** User-defined spending categories and archive state.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | Yes | Must equal `{categoryId}` and be non-blank |
| `name` | string | Yes | Trimmed, non-blank display name |
| `defaultSpendingClass` | string | Yes | `COMMITTED` or `DISCRETIONARY` |
| `archived` | boolean | Yes | Prefer archive over deletion when referenced |

**Relationships:** Referenced by `expenses.categoryId` and proposed `recurringExpenses.categoryId`. Expense records preserve category-name and spending-class snapshots.

**Validation if implemented:** Enforce the exact fields/types, path/field ID agreement, valid enum, and a bounded non-blank name. The fake rejects case-insensitive duplicate names; rules cannot establish collection-wide uniqueness by scanning, so a production design would need a transaction/uniqueness document or explicitly best-effort UI validation.

```json
{
  "id": "food",
  "name": "Food",
  "defaultSpendingClass": "DISCRETIONARY",
  "archived": false
}
```

### `users/{uid}/receivables/{receivableId}`

**Status: PROPOSED.** Only `FakeReceivableRepository` exists.

**Purpose:** Money expected from a project or other source but not yet received.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | Yes | Must equal `{receivableId}` |
| `title` | string | Yes | Trimmed, non-blank title |
| `projectReference` | string | Yes | Trimmed, non-blank reference |
| `expectedOriginalDecimal` | string | Yes | Positive decimal string |
| `currency` | string | Yes | `LKR` or `USD` |
| `expectedDate` | string | Yes | ISO date-only string |
| `archived` | boolean | Yes | Archive/restore state |

**Relationships:** An Income may refer to a Receivable through `receivableId`. A Receivable has no `Money` conversion snapshot or LKR base amount and is not included in received-Income totals.

**Validation if implemented:** Preserve the fake repository's non-blank ID/title/project checks and positive `BigDecimal`-parseable amount, then add exact field/type, currency, date, length, and path/ID validation.

```json
{
  "id": "receivable-example-001",
  "title": "Example website project",
  "projectReference": "example-project",
  "expectedOriginalDecimal": "50000.00",
  "currency": "LKR",
  "expectedDate": "2026-10-30",
  "archived": false
}
```

### `users/{uid}/recurringExpenses/{recurringId}`

**Status: PROPOSED.** Only `FakeRecurringRepository` exists.

**Purpose:** Scheduling templates for expected recurring costs, not confirmed Expense records.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | Yes | Must equal `{recurringId}` |
| `title` | string | Yes | Trimmed, non-blank title |
| `categoryId` | string | Yes | User-owned Category reference |
| `expectedAmountMinor` | integer | Yes | Positive LKR minor-unit estimate |
| `frequency` | string | Yes | `WEEKLY`, `MONTHLY`, or `YEARLY` |
| `intervalCount` | integer | Yes | Positive number of frequency units |
| `anchorDate` | string | Yes | ISO recurrence anchor date |
| `startDate` | string | Yes | ISO first-active date |
| `endDate` | string | Optional | ISO date not before `startDate` |
| `active` | boolean | Yes | Scheduling state |
| `reminderEnabled` | boolean | Yes | Reminder preference |
| `reminderLeadDays` | integer | Yes | Non-negative number of days |

**Relationships:** `categoryId` refers to a user-owned Category. A template has no automatic accounting relationship to Expense; confirmation would require a separate Expense record.

**Validation if implemented:** Match the fake checks for non-blank ID/title/category, positive amount/interval, valid date order, and non-negative reminder lead time. Also enforce exact types/enums/dates and define short-month anchor behavior before use.

```json
{
  "id": "recurring-example-001",
  "title": "Internet bill",
  "categoryId": "bills",
  "expectedAmountMinor": 350000,
  "frequency": "MONTHLY",
  "intervalCount": 1,
  "anchorDate": "2026-10-15",
  "startDate": "2026-10-15",
  "active": true,
  "reminderEnabled": true,
  "reminderLeadDays": 2
}
```

### `users/{uid}/goals/{goalId}`

**Status: PROPOSED.** Only `FakeGoalRepository` exists.

**Purpose:** Savings-goal definitions with Contribution history below each Goal.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | Yes | Must equal `{goalId}` |
| `name` | string | Yes | Non-blank Goal name |
| `targetAmountMinor` | integer | Yes | Positive LKR minor-unit target |
| `startDate` | string | Yes | ISO date-only string |
| `targetDate` | string | Yes | ISO date not before `startDate` |
| `archived` | boolean | Yes | Archive state |

**Relationships:** Parent of `contributions`. Progress is the sum of valid child `amountMinor` values, including negative reversals; it is not derived from Income or Expense records.

**Validation if implemented:** Match the fake checks for non-blank ID/name, positive target, and date order; enforce exact fields/types/path ID; and decide deletion behavior when Contributions exist.

```json
{
  "id": "goal-example-001",
  "name": "Buy a laptop",
  "targetAmountMinor": 10000000,
  "startDate": "2026-10-01",
  "targetDate": "2027-10-01",
  "archived": false
}
```

### `users/{uid}/goals/{goalId}/contributions/{contributionId}`

**Status: PROPOSED.** Contribution invariants currently exist only in `FakeGoalRepository`.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `id` | string | Yes | Must equal `{contributionId}` |
| `goalId` | string | Yes | Must equal parent `{goalId}` |
| `amountMinor` | integer | Yes | Positive for opening/allocation; exact negative inverse for reversal |
| `kind` | string | Yes | `OPENING`, `ALLOCATION`, or `REVERSAL` |
| `occurredDate` | string | Yes | ISO date-only string |
| `reversesId` | string | Conditional | Required for reversal and absent otherwise |
| `note` | string | Optional | User note |

**Relationships:** Nested below and explicitly linked to its Goal. `reversesId` refers to one non-reversal Contribution in the same Goal.

**Validation if implemented:** Preserve append-only history. The fake treats an identical create retry as idempotent, rejects conflicting ID reuse, requires a reversal to be the exact negative of its target, and allows only one reversal per target. Enforcing all cross-document invariants may require transactions, deterministic IDs, rule-readable documents, or trusted backend logic.

```json
{
  "id": "contribution-example-001",
  "goalId": "goal-example-001",
  "amountMinor": 500000,
  "kind": "ALLOCATION",
  "occurredDate": "2026-10-16",
  "note": "Illustrative allocation"
}
```

### Optional proposal: `users/{uid}/settings/preferences`

**Status: PROPOSED AND NOT APPROVED FOR FIRESTORE.** No preferences model, repository, DataStore dependency, or Firestore implementation exists. UID-scoped Android DataStore remains the intended sole authority unless cloud synchronization is explicitly approved.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `baseCurrency` | string | Yes | Proposed fixed value `LKR` for the current model |
| `schemaVersion` | integer | Yes | Proposed positive migration version |

**Relationships:** Owned by the user root and not a financial transaction.

**Validation if approved:** Accept only the literal `preferences` document, exact fields/types, and currently only base `LKR`. Before implementation, approve source-of-truth, synchronization direction, conflicts, migration, offline behavior, security rules, and logout/user-switch handling.

```json
{
  "baseCurrency": "LKR",
  "schemaVersion": 1
}
```

## Queries and indexes

| Feature | Status | Query or proposed shape | Index status |
| --- | --- | --- | --- |
| Income history | **IMPLEMENTED** | Owner's `incomes`; half-open `occurredDate` range; ascending date then document ID | No explicit Income composite is checked in; add only if Firestore requests the exact index |
| Expense history | **IMPLEMENTED IN MEMORY AFTER SNAPSHOT** | Listener reads the owner's Expense collection, then filters half-open date range and sorts by date/ID locally | Two Expense composite definitions are checked in but are not used by the current listener; deployment unverified |
| Categories | **PROPOSED** | Filter `archived`, optionally order by `name` | Candidate `archived ASC, name ASC`; not checked in |
| Receivables | **PROPOSED** | Filter `archived`, order by `expectedDate` | Candidate composite only |
| Recurring templates | **PROPOSED** | Filter `active`, order by `anchorDate` | Candidate composite only |
| Goals | **PROPOSED** | Filter `archived`, order by `targetDate` | Candidate composite only |
| Contributions | **PROPOSED** | Goal child collection ordered by `occurredDate`, optionally document ID | Add a composite only if the final query requires it |

Use implemented query shapes and Firestore's generated missing-index guidance before adding definitions. A checked-in definition still does not prove deployment.

## Implementation checklist

- [x] Define decimal amount/rate precision and `HALF_UP` minor-unit rounding.
- [x] Implement `MoneyConverter`, `MoneyValidator`, and Income domain validation with boundary and overflow tests.
- [x] Implement explicit versioned Income mapping with stable IDs and ISO dates.
- [x] Implement authenticated, owner-tagged Income observation and account-switch clearing.
- [x] Implement owner-bound Income save and delete beneath `users/{uid}`.
- [x] Implement automatic/manual USD rates and preserved historical snapshots.
- [x] Check in deny-by-default ownership/schema rules and Income rule tests.
- [x] Check in index configuration for currently recorded indexes.
- [ ] Verify rules and required indexes in each deployed Firebase environment.
- [ ] Add Firestore implementations/rules only when the remaining proposed finance collections are implemented.
