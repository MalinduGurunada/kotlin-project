# FinClarity manual test results checklist

> **Historical snapshot:** This document audits `e0eda37` only. Before publication, remote `main` was fetched and the checkout advanced by 128 commits to `e2b4883`. These findings and original validation results are not an assessment of the newer application. See [README.md](README.md) before using this document.

Build: **main e0eda37**, audit date **5 November 2026**. These are phone/manual cases, not the 57 passing local unit tests. No phone test was executed by this audit. Use the full steps in [MANUAL_TEST_GUIDE.md](MANUAL_TEST_GUIDE.md).

Tester: __________  Test date: __________  Device / Android: __________  Installed commit/APK: __________

Enter NOT TESTED / PASS / FAIL in Verdict after your own attempt. Actual Result and Verdict columns below are deliberately blank. Availability distinguishes executable shell checks from absent financial flows. For a block, record the missing control and leave the financial operation NOT TESTED, or use FAIL when explicitly judging requirement availability. Do not infer a PASS from opening the placeholder. Use disposable accounts/records only; never record passwords here.

## Available app shell

| Test | Check | Availability | Actual Result | Verdict |
| --- | --- | --- | --- | --- |
| [MT-01](MANUAL_TEST_GUIDE.md#mt-01) | Open the correct audited app | READY | | |
| [MT-02](MANUAL_TEST_GUIDE.md#mt-02) | Use the only app button | READY | | |
| [MT-03](MANUAL_TEST_GUIDE.md#mt-03) | Return from Home with Android Back | READY | | |
| [MT-04](MANUAL_TEST_GUIDE.md#mt-04) | Close and reopen the app | READY | | |
| [MT-08](MANUAL_TEST_GUIDE.md#mt-08) | Open the shell without internet | READY | | |

## Display and device checks

| Test | Check | Availability | Actual Result | Verdict |
| --- | --- | --- | --- | --- |
| [MT-05](MANUAL_TEST_GUIDE.md#mt-05) | Use light and dark themes | READY | | |
| [MT-06](MANUAL_TEST_GUIDE.md#mt-06) | Increase text size | READY | | |
| [MT-07](MANUAL_TEST_GUIDE.md#mt-07) | Rotate both placeholder screens | READY | | |
| [MT-09](MANUAL_TEST_GUIDE.md#mt-09) | Read and operate the button with TalkBack | READY | | |
| [MT-10](MANUAL_TEST_GUIDE.md#mt-10) | Check minimum/current Android and phone sizes | READY | | |

## Repository foundations; blocked phone tests

| Test | Check | Availability | Actual Result | Verdict |
| --- | --- | --- | --- | --- |
| [MT-11](MANUAL_TEST_GUIDE.md#mt-11) | Add/view/edit/delete income, currency and crypto entries | BLOCKED — no app route | | |
| [MT-12](MANUAL_TEST_GUIDE.md#mt-12) | Create/rename/archive/reclassify categories | BLOCKED — no app route | | |
| [MT-13](MANUAL_TEST_GUIDE.md#mt-13) | Add/view/edit/delete an expense and undo | BLOCKED — no app route | | |
| [MT-14](MANUAL_TEST_GUIDE.md#mt-14) | Back-date expenses and filter/search history | BLOCKED — no app route | | |
| [MT-15](MANUAL_TEST_GUIDE.md#mt-15) | Create/edit/deactivate a recurring commitment | BLOCKED — no app route | | |
| [MT-16](MANUAL_TEST_GUIDE.md#mt-16) | Manage multiple goals and contribution history | BLOCKED — no app route | | |

## Missing SRS flows

| Test | Check | Availability | Actual Result | Verdict |
| --- | --- | --- | --- | --- |
| [MT-17](MANUAL_TEST_GUIDE.md#mt-17) | Check the remaining promised screens and account features | BLOCKED — no app route | | |

## Financial scenarios

| Test | Check | Availability | Actual Result | Verdict |
| --- | --- | --- | --- | --- |
| [MT-18](MANUAL_TEST_GUIDE.md#mt-18) | Scenario A — income total LKR 196,000 | BLOCKED — no app route | | |
| [MT-19](MANUAL_TEST_GUIDE.md#mt-19) | Scenario B — expenses LKR 57,000; cash flow LKR 139,000 | BLOCKED — no app route | | |
| [MT-20](MANUAL_TEST_GUIDE.md#mt-20) | Scenario C — MacBook goal and reversals | BLOCKED — no app route | | |

## Firebase and offline scenario; blocked

| Test | Check | Availability | Actual Result | Verdict |
| --- | --- | --- | --- | --- |
| [MT-21](MANUAL_TEST_GUIDE.md#mt-21) | Scenario D — Registration and login | BLOCKED — no app route | | |
| [MT-22](MANUAL_TEST_GUIDE.md#mt-22) | Scenario D — Session after close and reopen | BLOCKED — no app route | | |
| [MT-23](MANUAL_TEST_GUIDE.md#mt-23) | Scenario D — Records after app restart | BLOCKED — no app route | | |
| [MT-24](MANUAL_TEST_GUIDE.md#mt-24) | Scenario D — Live Firebase synchronization | BLOCKED — no app route | | |
| [MT-25](MANUAL_TEST_GUIDE.md#mt-25) | Scenario D — Disconnect from the internet | BLOCKED — no app route | | |
| [MT-26](MANUAL_TEST_GUIDE.md#mt-26) | Scenario D — Read previously loaded records offline | BLOCKED — no app route | | |
| [MT-27](MANUAL_TEST_GUIDE.md#mt-27) | Scenario D — Add an expense while offline | BLOCKED — no app route | | |
| [MT-28](MANUAL_TEST_GUIDE.md#mt-28) | Scenario D — Edit and delete records while offline | BLOCKED — no app route | | |
| [MT-29](MANUAL_TEST_GUIDE.md#mt-29) | Scenario D — Reconnect to the internet | BLOCKED — no app route | | |
| [MT-30](MANUAL_TEST_GUIDE.md#mt-30) | Scenario D — Confirm offline changes synchronized | BLOCKED — no app route | | |
| [MT-31](MANUAL_TEST_GUIDE.md#mt-31) | Scenario D — Different-account isolation | BLOCKED — no app route | | |

## Financial checkpoints (all unexecuted; not current UI outputs)

| Check | Expected if the full feature is implemented | Actual Result | Verdict |
| --- | --- | --- | --- |
| November received income | LKR 196,000 | | |
| AdSense original/rate/converted | USD 60 / LKR 300 per USD / LKR 18,000 | | |
| Crypto gain minus loss | LKR 3,000 | | |
| Expense total | LKR 57,000 | | |
| Net cash flow | LKR 139,000 | | |
| Committed/discretionary, SRS commute classification | LKR 49,000 /8,000 | | |
| MacBook opening /remaining | LKR 11,200 /478,800 | | |
| After +20,000 /remaining | LKR 31,200 /458,800 | | |
| After +10,000 /remaining | LKR 41,200 /448,800 | | |
| Reverse second 10,000 /remaining | LKR 31,200 /458,800; both original and reversal retained | | |
| Separate Emergency Fund goal | LKR 5,000; MacBook unchanged | | |
| Offline add/edit/delete survives restart | Same test records and intended values while disconnected | | |
| Reconnect and second device | Exactly one new record, edited value, deleted test item absent | | |
| Account B isolation | No A-ONLY records or cached details | | |

## Outcome counts to fill after manual work

Ready shell checks: 10. Blocked financial/authentication cases on audited main: 21. Total numbered cases: 31.

PASS: ____  FAIL: ____  NOT TESTED: ____  Blocked prerequisites observed: ____

Notes / screenshots retained locally / different installed build: __________________________________

The local unit-test run is recorded separately in FEATURE_AUDIT.md: 57 tests, zero failures/errors/skips. The APK build succeeded. Neither result fills any manual verdict here.
