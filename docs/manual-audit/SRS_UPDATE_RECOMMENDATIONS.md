# FinClarity SRS update recommendations

> **Historical snapshot:** This document audits `e0eda37` only. Before publication, remote `main` was fetched and the checkout advanced by 128 commits to `e2b4883`. These findings and original validation results are not an assessment of the newer application. See [README.md](README.md) before using this document.

Audit date: 5 November 2026 (Asia/Colombo).

Repository: `C:\Users\Himasha\AndroidStudioProjects\kotlin-project`.
Branch: `main`; audited commit: `e0eda37202132c8826d1c1d1ce762812bdc86a92` (16 October 2026).
With the user's approval, local main was fast-forwarded by nine commits to the existing `origin/main` snapshot. No fetch/pull was performed; this does not assert the current remote server has no newer commits. Work on dev or feature branches is outside this audit.

The task's `D:\PBD Assignment\Finane App\kotlin-project` path was unavailable because this environment has no D: drive. The initially open `PBDassignmnet` folder is a different project and is not the audited app.

SRS: `C:\Users\Himasha\Downloads\FinClarity_SRS (1).docx`, version 1.0, 15 October 2026. SHA-256: `C7F0275A732A0216FC7F129BF85FA2367D0AE067DA12C0C22889B161F61BA803`.
The SRS was read as requirements/data, including FR-01–FR-58, all 16 NFR rows, sections 1–7, appendices A–F and glossary. The pasted task supplied the audit instructions. The source SRS was not edited.

This is static implementation verification plus local build/unit-test evidence. No app was opened on a device, no manual result was recorded, no Firebase operation was executed, no notification was sent, and no real financial data was read or changed. Runtime behaviour and deployed Firebase settings remain unverified.

## Keep requirements separate from implementation status

The SRS specifies a target product. Missing features are implementation gaps, not a reason to silently delete their requirements. Preserve FR-01–FR-58, their MoSCoW priorities and traceability. Add an **as-built status dated 5 November 2026, commit e0eda37** and link FEATURE_AUDIT.md. No existing SRS/document has been rewritten.

Suggested status wording for the introduction or a new implementation-status note:

> The specified capabilities describe the intended FinClarity product. In main at e0eda37, the Android application contains a two-screen navigation prototype, finance models and in-memory repository implementations. Production authentication, feature screens, ViewModels, Firestore access, Room caching, reminders and financial analytics are not integrated. Repository-only functionality must not be presented as a usable phone feature. Runtime acceptance remains subject to manual testing.

## Exact locations and proposed corrections

| SRS location / IDs | Discrepancy | Recommended wording or action |
| --- | --- | --- |
| §2.1 tiers and Room; §2.4 persistence; §5 Firebase/local interfaces; glossary Room | No Room or production Firebase data path | Keep the target architecture, labelled **Required target architecture**. Add: “The current main implementation uses process-local fake repositories; no Room cache or Firestore repository is integrated. Room-mediated offline reads/writes remain unimplemented.” |
| §2.4 strict MVVM; §4 maintainability | No ViewModel classes/DI bindings despite Hilt shell | Add status: “Compose navigation and Hilt application/activity setup exist. The ViewModel/UI-state layer and production repository injection remain to be implemented.” Do not claim the current app implements the full three-tier chain. |
| §2.2 account/onboarding; §3.1 FR-01–07; §5 Login/Register | Login label is not login | Add status: “Go to Home is a prototype navigation button without authentication. Registration, login/logout persistence, reset, Google sign-in, onboarding and account/data deletion are unavailable on main.” Retain all account requirements. |
| FR-05, FR-57; §4 security; §5 Firebase rules | No submitted rules or SDK calls; proposed schema only | Add: “Per-user paths, real-time listeners, server timestamps and submitted/deployed Security Rules are not verified on current main. The schema document is a proposal.” Do not claim deployed rules are insecure or already deployed. |
| FR-08 and Appendix F IncomeSource | Enum is not a user-defined source entity | Retain source management as pending. State current enum has Salary/Freelance/AdSense/Crypto only; cadence, expected ranges, Other and per-source defaults are absent. |
| FR-09–12/20/31; §2.2 currency; Appendix F Money-like fields | Money fields exist, not validated/calculated/persisted UI transactions | Add status: “Money preserves caller-provided decimal strings and LKR minor-unit Longs in fake repositories. Conversion, field-consistency validation, last-used-rate settings and durable storage remain unimplemented.” |
| §2.2 currencies; FR-20; Appendix C UC-05/07 | Only LKR/USD CurrencyCode values; USDT/ETH asset text is a different concept | Clarify asset versus denomination: “A trade has an asset symbol distinct from its result currency. Supported transaction currencies are explicitly enumerated; USDT/ETH amounts require a defined scale and rate policy before support is claimed.” Current status: LKR/USD representation only, no editor. |
| FR-12/16–19; Appendix F project/milestone/invoice | Receivable is a simple expected-payment record | Add: “Current Receivable has a title, project text reference, expected amount/currency/date and archive flag. It does not implement a project, milestone, invoice status, reconciliation or overdue-reminder lifecycle.” Keep those required behaviours pending. |
| FR-15/21/22/45/48/50; Appendix D calculations | No totals, signed net, YTD, multi-month summaries or dashboard | Keep numerical targets as acceptance examples, not outputs already produced. Explicitly exclude pending income and require signed realised losses in the future common aggregation path. No new monthly/quarterly ID should be introduced as implemented. |
| FR-23/24; §4 friction; §5 quick-add | No expense editor; model lacks merchant/time/recurring origin | Add status: “Expense currently models date without time, payment channel plus optional label, category snapshot, Money and note in memory. UI capture and remaining record fields are pending.” Do not claim three taps/ten seconds was measured. |
| FR-25; Appendix E | Only Food/Transport/Bills fake seeds; no reclassification | Preserve Appendix E defaults as target. Add status listing the three current fake seeds and their classes. Specify whether category identity/effective class overrides historical snapshots so retroactive analysis is deterministic. Do not change target Transport(commute) to discretionary merely to match the fake. |
| FR-26/29/30/32 | Merchant memory, catch-up, transfers and full filters absent | Add status: “Only repository date filtering and caller-provided dates exist. Merchant/account/search/catch-up flows remain pending.” PaymentChannel is not an account ledger. |
| FR-27; §4 offline reliability; §5 Room/WorkManager | No durable offline storage or sync | Add: “An in-memory fake can run without network in tests, but it is not offline financial support. Required acceptance includes restart while disconnected and exactly-once reconciliation after reconnect.” |
| FR-28 | Fake update/delete have no undo | Clarify: “The user can undo a save or delete immediately, with the visible undo window specified in the UI design; undo restores the previous record or removes a new one without duplicates.” Current status remains repository-only partial. |
| FR-33; Appendix F RecurringRule | Templates support weekly/monthly/yearly intervals; no occurrences | Proposed extension: “Recurring committed-cost templates support weekly, monthly or yearly frequency, positive interval, anchor/start/optional end date, category and expected LKR amount. Each due occurrence remains pending until confirmed; edits and deactivation shall not duplicate already confirmed expenses.” These are target behaviours; only template storage/validation is currently partial. |
| FR-34/35/55; §2.2 gentle reminders; §4 battery; §5 channels | Reminder flags are not schedulers/delivery | Keep requirements pending. Add status: “No notification channel, permission flow, WorkManager task, due generator, daily cap or notification action exists on current main.” Do not promise notification delivery from a stored flag. |
| FR-36/38; §6 single goal/additional optional; Appendix F Goal/Contribution | Multiple IDs and separate opening records in fake, no goal UI | Proposed clarification: “At least one primary goal is required; optional multiple goals maintain independent contribution histories. Initial saved value is an OPENING contribution, created consistently with the goal. All saved/remaining/progress values include opening, allocation and signed reversal records for that goal.” Existing IDs suffice; do not invent a duplicate multiple-goal FR. |
| FR-37/39–43; Appendix D | No required/actual saving, projection, what-if, spending delay or set-aside flow | Preserve targets and add explicit pending status. Specify the months-remaining boundary, rounding, zero/negative saving rate, past deadlines and contribution-versus-cash-flow semantics before claiming displayed results. |
| FR-44 | Fake updates preserve separate records; no UI or celebrations | Proposed extension: “Editing goal target/deadline or archiving a goal shall preserve its immutable contribution/reversal history. Milestone celebrations at 25%, 50%, 75% require a defined once-only crossing policy.” Current status is partial; archive is code-only. |
| FR-46–54; §4 emotional safety/plain language | No chart, insight, period or meaningful financial empty states | Retain the full set as pending; do not substitute a Home Screen label for an empty-state implementation. Define expected zero/negative-income chart behaviour. |
| FR-56; Appendix F UserProfile; §5 settings | No settings/UI/DataStore integration | Add status: “Current currency enums and fake category operations are not a settings screen. Preferences/payday/reminder configuration is not implemented.” Existing architecture docs describe UID-scoped DataStore as planned; do not present that as current code. |
| FR-57 conflict resolution | Local Mutex/Flows cannot establish server conflict semantics | Clarify required offline/concurrent ordering, timestamp behaviour and tie handling; verify with isolated test accounts/emulator before claiming document-level last-write-wins. No rule/data changes are recommended by this audit action. |
| FR-58 CSV | No export | Retain as Could/pending. Do not claim file creation or sharing capability. |
| §4 scalability and Appendix F MonthlySummary | In-memory date filter, no paging or pre-aggregated summaries | Add status: “Date-bounded fake observations exist; cloud queries, 50-record paging and summary aggregation are not implemented.” Keep paging/cost targets. |
| §4 compatibility/accessibility; §2.4 API bounds | Configured 26 minimum /37 target; theme foundation only | Add actual configured API bounds and successful debug build. Keep device/large-font/contrast/TalkBack tests explicitly NOT TESTED; do not infer compatibility/accessibility certification. |
| §4 demonstrability; Appendix C | Only route prototype can be demonstrated on this branch | Add status: “57 fake-repository/example local tests pass; the debug APK builds. No full finance use case or live Firebase demo has been verified.” |
| Appendices A/B/C; glossary | Intended use cases/remedies can be mistaken for completion claims | Preserve traceability/use-case descriptions; attach per-FR status and a date/commit. Label diagrammed UI/data links as target until integrated. |

### Resolve the SRS's signed-value inconsistency

FR-20 requires negative realised losses, while §4 Security says to require positive amounts for every record. A universal positive-amount rule would reject the required losses and contribution reversals.

Proposed replacement for the amount-validation sentence in §4 Security (a requirement change proposal, not a rule-file edit):

> Validate field types, identifiers, currencies, ownership and money consistency per record kind. Ordinary expense/received-income amounts and opening/allocation contributions must be positive. Realised trading losses and valid contribution reversals must use the required signed representation; a reversal must exactly negate one eligible same-goal original and may occur only once. Deny all other access.

Also define loss magnitude versus signed stored amount so direction is applied exactly once. Do not let clients submit contradictory original/rate/LKR fields without validation. The current fake repositories do not provide these production guarantees.

## Genuinely new requirement proposal

**FR-59 — Contribution reversal integrity (proposed Should; repository-only partial, unavailable in UI).**

> The system shall preserve goal contributions as an append-only history. A user may reverse an eligible contribution in full by appending a new signed record referencing the original contribution of the same goal. The reversal amount shall be the exact negative of the original, and the original shall be reversed no more than once. An identical retry with the same ID shall not create a duplicate; changing a saved contribution under that ID shall be rejected. Goal totals shall include reversals without deleting the original history.

Evidence: [FakeGoalRepository.kt](../../app/src/main/java/com/example/personalfinanceapp/feature/goal/FakeGoalRepository.kt), `addContribution`, and its existing local test suite. The fake accepts reversals of any non-reversal record, including OPENING; decide whether the product should allow reversal of initial savings before finalising the wording. No UI, durable storage or server enforcement exists. Do not label FR-59 delivered until those paths are integrated. Manual paths are unavailable; MT-16/20 and the Scenario C worksheet give the blocked phone checkpoint and correct intended totals.

Do not allocate FR-60 onward to duplicate FR-10 conversion, FR-25 category management, FR-33 recurring costs, FR-36 multiple goals, FR-38 contributions, FR-44 edits/archival, FR-45 dashboard or FR-50 comparisons. No other genuinely new financial phone feature is evidenced on this branch. The system theme belongs under compatibility rather than a new finance requirement.

## Priority for implementation and later manual acceptance

1. Integrate real authentication and submitted/tested ownership rules before adding production financial data; verify session, logout and account switching with disposable accounts.
2. Connect expense/income screens through ViewModels and production repositories with a shared exact-money/sign policy. Add durable offline operations and restart/reconnect tests.
3. Integrate common totals/dashboard and compare Scenario A/B (196,000 /57,000 /139,000) before adding charts/trends.
4. Integrate goal opening/contributions/progress/history, then required rate/projections and reversal integrity.
5. Complete recurring occurrences/receivables and the remaining SRS behaviours; retain their priorities and blockers rather than claiming template fields implement them.

This order is an audit recommendation only. No application implementation was changed, no new requirement was inserted into the original SRS, and no deployed service was inspected or modified.
