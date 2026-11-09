# FinClarity manual audit

This folder contains the SRS comparison and manual testing materials produced on 5 November 2026.

> **Historical snapshot:** This audit covers commit `e0eda37202132c8826d1c1d1ce762812bdc86a92` (`e0eda37`). Before publication, a fetch found that remote `main` had advanced by 128 commits to `e2b4883`. The checkout was fast-forwarded before committing these documents. The findings, feature counts and test results below do **not** describe the newer application code. A new audit is required before using them to assess current `main`.

## Documents

| Document | What to use it for |
| --- | --- |
| [Feature audit](FEATURE_AUDIT.md) | All FR-01 to FR-58, priority/status totals, code evidence, all 16 non-functional areas, architecture and implementation gaps. |
| [Manual testing guide](MANUAL_TEST_GUIDE.md) | Beginner-friendly phone/emulator steps, actual navigation labels, sample financial values, expected results and failure indicators. |
| [SRS update recommendations](SRS_UPDATE_RECOMMENDATIONS.md) | Documentation mismatches, proposed wording, existing requirement corrections and proposed FR-59 for contribution reversal integrity. |
| [Test results checklist](TEST_RESULTS_CHECKLIST.md) | Blank actual-result and verdict fields for recording your own manual attempts. |

## How to use this audit

1. Confirm which application commit your installed APK comes from. A package name or version number alone does not identify the audited source.
2. Read the feature audit's status definitions and the snapshot warning above.
3. For the audited `e0eda37` build, start with MT-01 and MT-02 in the manual guide to confirm the placeholder screens and navigation.
4. Follow MT-01 to MT-10 for the available shell, theme, accessibility and device checks.
5. MT-11 to MT-31 document financial/authentication flows blocked by missing screens in that old snapshot. They are not ready-to-run instructions for a newer build.
6. Record what you actually observed in the checklist. Use NOT TESTED, PASS or FAIL as the guide explains; a missing prerequisite is not a successful financial test.
7. For current `main` or another feature branch, re-inspect its actual screens and data connections and refresh the audit before following feature-specific instructions.

Use disposable test accounts and records. Do not delete or alter real financial data. Never enter passwords in the checklist.

## Findings for the audited snapshot only

| Requirement status | Count |
| --- | ---: |
| Fully implemented | 0 |
| Partial, repository-only | 14 |
| Missing | 44 |
| Unverified implementation scope | 0 |
| Total functional requirements | 58 |

All 14 partial requirements had in-memory repository support without connected phone features. The reachable app at that commit consisted of Login Screen, Go to Home and Home Screen. Runtime behaviour and deployed Firebase settings were not verified. These statements are historical and must not be applied to the later main branch.

The audit identified additional code foundations for multiple goals, append-only contribution reversals, archival and recurring intervals. They were not integrated phone features in `e0eda37`.

## Financial worksheets

The guide supplies acceptance arithmetic, not observed application results:

- Scenario A: LKR 196,000 net received income, including USD 60 at LKR 300/USD and crypto gain/loss net of LKR 3,000.
- Scenario B: LKR 57,000 expenses and LKR 139,000 net cash flow when combined with Scenario A.
- Scenario C: MacBook goal of LKR 490,000; opening savings of LKR 11,200; contributions of LKR 20,000 and LKR 10,000; reversal of the second contribution leaves LKR 31,200 saved and LKR 458,800 remaining.
- Scenario D: registration, session persistence, restart, live synchronization, offline reads/writes, reconnect and account-isolation checkpoints.

## Validation and limits

The following commands succeeded against `e0eda37` during the original audit:

```powershell
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest
.\gradlew.bat --offline --no-daemon :app:assembleDebug
```

All 57 existing local unit tests passed with zero failures, errors or skips, and the debug APK built successfully. Those tests covered fake repositories and an example arithmetic test; they did not establish successful Firebase, Room, UI or device behaviour. They were not rerun as validation of the newer `main` code for this documentation publication.

No manual tests were performed, no live Firebase operations were invoked and no notifications were sent during inspection. Test reports, APKs and the source SRS are local artifacts and are not included in this documentation commit. Relative links to build outputs in the detailed documents work only where those outputs exist.

The source SRS remains unchanged. FR-59 is a proposal, and existing FR-01 to FR-58 retain their IDs. For project setup and the current implementation overview, see the [project README](../../README.md).
