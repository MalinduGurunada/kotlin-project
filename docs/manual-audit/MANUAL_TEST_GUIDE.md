# FinClarity manual testing guide

> **Historical snapshot:** This document audits `e0eda37` only. Before publication, remote `main` was fetched and the checkout advanced by 128 commits to `e2b4883`. These findings and original validation results are not an assessment of the newer application. See [README.md](README.md) before using this document.

Audit date: 5 November 2026 (Asia/Colombo).

Repository: `C:\Users\Himasha\AndroidStudioProjects\kotlin-project`.
Branch: `main`; audited commit: `e0eda37202132c8826d1c1d1ce762812bdc86a92` (16 October 2026).
With the user's approval, local main was fast-forwarded by nine commits to the existing `origin/main` snapshot. No fetch/pull was performed; this does not assert the current remote server has no newer commits. Work on dev or feature branches is outside this audit.

The task's `D:\PBD Assignment\Finane App\kotlin-project` path was unavailable because this environment has no D: drive. The initially open `PBDassignmnet` folder is a different project and is not the audited app.

SRS: `C:\Users\Himasha\Downloads\FinClarity_SRS (1).docx`, version 1.0, 15 October 2026. SHA-256: `C7F0275A732A0216FC7F129BF85FA2367D0AE067DA12C0C22889B161F61BA803`.
The SRS was read as requirements/data, including FR-01–FR-58, all 16 NFR rows, sections 1–7, appendices A–F and glossary. The pasted task supplied the audit instructions. The source SRS was not edited.

This is static implementation verification plus local build/unit-test evidence. No app was opened on a device, no manual result was recorded, no Firebase operation was executed, no notification was sent, and no real financial data was read or changed. Runtime behaviour and deployed Firebase settings remain unverified.

## Start here

This guide covers **every available app control in main e0eda37** and records why the requested financial tests cannot be performed. There are no hidden financial menus: the app has only Login Screen, Go to Home and Home Screen. Its app drawer name is **PersonalFinanceApp**, not FinClarity. If you have a richer installed app, its version is outside this audit even if the package/version number is the same.

MT-01–MT-10 can be attempted on a phone/emulator. MT-11–MT-31 are blocked at the missing-control checkpoint. Their acceptance targets and sample data are retained so missing functionality is visible, but there are no invented Add/Save/More/Settings button labels. A code-only PARTIAL requirement is not available to tap.

## Install safely and record your setup

Use an empty test emulator/profile or a spare test phone. Avoid installing over a real financial installation with the same package ID. Do not uninstall, clear storage, reset passwords or delete data from a real account.

The audit built [app-debug.apk](../../app/build/outputs/apk/debug/app-debug.apk) successfully. Android application ID is `com.example.personalfinanceapp`, version name `1.0`, version code `1`, minimum Android 8.0/API 26.

1. In Android Studio, use **Open** and select `C:\Users\Himasha\AndroidStudioProjects\kotlin-project`.
2. Check the branch indicator says **main**. These documents describe commit `e0eda37`, not dev or a feature branch.
3. In **Tools → Device Manager**, start a test emulator, or connect a test phone with USB debugging enabled.
4. Select the **app** run configuration and the test device, then press **Run**. Alternatively drag the built APK onto the running emulator. Device/Android Studio menu wording may vary.
5. If a phone blocks installation from an APK, follow that test device's installation prompt. Do not change security settings on a personal data device for this audit.
6. Record the device and first screen below. If you see a real login form, dashboard or bottom bar, stop and obtain an audit of that build; do not use guessed paths from these documents.

| Setup item | Your value |
| --- | --- |
| Tester and test date | |
| Device/emulator model and screen profile | |
| Android version/API | |
| Installed APK source/commit | |
| Test account A / B (aliases only, no passwords) | |
| First visible screen | |

For verdicts use **NOT TESTED**, **PASS** or **FAIL**. Actual Result is deliberately blank in each case. A known missing control has a separate availability of BLOCKED; it is not a completed financial test. Record the block in Actual Result. If you are assessing requirement availability rather than the unexecuted financial operation, mark FAIL with that explanation. No case below has been performed by this audit.

## Test cases

<a id="mt-01"></a>

### MT-01 — Open the correct audited app

**Feature:** Open the correct audited app.

**Coverage:** Shell; FR-45 gap.

**Availability:** READY.

**Navigation:** App drawer → PersonalFinanceApp.

**Prerequisites:** Install the APK built from commit e0eda37 on a test phone/emulator. No account or internet is needed.

**Test Steps:**

1. Open the app drawer and tap PersonalFinanceApp.
2. Read the screen and find the button.
3. Record the displayed text. If you see a finance dashboard or real login fields, stop: that installed build differs from this audited main snapshot.

**Expected Result:** The source defines Login Screen and the Go to Home button. It has no email/password fields. This is a shell check, not a successful FR-02 login.

**Failure Indicators:** App cannot launch, crashes, or shows a different build. A different UI requires a fresh audit, not guessed test paths.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-02"></a>

### MT-02 — Use the only app button

**Feature:** Use the only app button.

**Coverage:** Prototype navigation; FR-02/45 gaps.

**Availability:** READY.

**Navigation:** Open app → Go to Home.

**Prerequisites:** MT-01 is complete; no account/network.

**Test Steps:**

1. Open PersonalFinanceApp and ensure Login Screen is visible.
2. Tap Go to Home once.
3. Read the result and inspect the whole screen for controls.

**Expected Result:** Home Screen appears. No credential check occurs. There are no income, expense, goal, More or Settings buttons.

**Failure Indicators:** Tap does nothing, crash, or wrong destination. Reaching Home is not evidence of authentication.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-03"></a>

### MT-03 — Return from Home with Android Back

**Feature:** Return from Home with Android Back.

**Coverage:** Prototype navigation.

**Availability:** READY.

**Navigation:** Open app → Go to Home → Android Back.

**Prerequisites:** Begin on Login Screen; no network.

**Test Steps:**

1. Open the app and tap Go to Home.
2. Use Android Back once (gesture or device Back button).
3. Read the screen; tap Go to Home again.

**Expected Result:** The Login route remains below Home in the navigation stack, so Back should return to Login Screen and the button should work again. This platform behaviour has not been exercised here.

**Failure Indicators:** App crashes, unexpected blank screen or repeated taps build an unusable stack.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-04"></a>

### MT-04 — Close and reopen the app

**Feature:** Close and reopen the app.

**Coverage:** FR-02 session distinction.

**Availability:** READY.

**Navigation:** Open app → Go to Home; then Android Settings → Apps → PersonalFinanceApp.

**Prerequisites:** Use a test installation. Do not clear app storage.

**Test Steps:**

1. Open the app and tap Go to Home.
2. Send the app to the background and reopen it from Recents; note the screen.
3. Open Android Settings → Apps → PersonalFinanceApp → Force stop (wording varies by phone), then reopen the app.
4. Note the fresh-launch screen separately from the Recents result.

**Expected Result:** A fresh navigation graph starts at Login. Android may restore navigation state in other relaunch situations; neither Home restoration nor Login display establishes a Firebase session because no login exists. No financial persistence can be checked.

**Failure Indicators:** Crash or blank screen. Treat any supposed account/session evidence as unproven; no credentials were entered.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-05"></a>

### MT-05 — Use light and dark themes

**Feature:** Use light and dark themes.

**Coverage:** Compatibility NFR; THEME.

**Availability:** READY.

**Navigation:** Android Settings → Display → return to PersonalFinanceApp.

**Prerequisites:** Phone/emulator with system light/dark setting. Menu wording varies by Android version.

**Test Steps:**

1. Open Android Settings → Display and choose Light (or disable Dark theme).
2. Open PersonalFinanceApp, inspect Login Screen, then tap Go to Home.
3. Return to Android Settings → Display and enable Dark theme.
4. Reopen the app, use Android Back if needed, and inspect the same text/button again.
5. On Android 12 or later, optionally change Wallpaper & style colours and reopen the app.

**Expected Result:** Text/button should remain readable in both modes. Theme.kt chooses system light/dark and dynamic colours on Android 12+. The placeholder has no explicit full-screen Surface/background, so actual full-screen contrast/background must be observed, not assumed. Older devices use the static palettes.

**Failure Indicators:** Unreadable text, clipped control, missing button or crash after a theme change. Dynamic wallpaper colours are not expected below Android 12.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-06"></a>

### MT-06 — Increase text size

**Feature:** Increase text size.

**Coverage:** Accessibility NFR.

**Availability:** READY.

**Navigation:** Android Settings → font size → PersonalFinanceApp.

**Prerequisites:** Test phone/emulator; note original font size.

**Test Steps:**

1. Open Android Settings → Display → Font size (or Accessibility → Display size and text) and choose the largest font size.
2. Open the app. Read Login Screen and Go to Home.
3. Tap Go to Home and read Home Screen.
4. Restore your original text setting.

**Expected Result:** Both labels and the only button should stay readable and usable. This is a runtime acceptance check; source use of sp/Material components does not prove it passes.

**Failure Indicators:** Text is cut off, overlaps or makes the button unreachable.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-07"></a>

### MT-07 — Rotate both placeholder screens

**Feature:** Rotate both placeholder screens.

**Coverage:** Compatibility/robustness NFRs.

**Availability:** READY.

**Navigation:** Open app → rotate → Go to Home → rotate.

**Prerequisites:** Enable auto-rotate on a phone/emulator.

**Test Steps:**

1. Open the app on Login Screen in portrait.
2. Rotate to landscape and read the label/button.
3. Tap Go to Home, rotate back to portrait, and read Home Screen.
4. Use Android Back and check that navigation still responds.

**Expected Result:** No crash; current labels/control remain reachable. Record whether route state survives rotation. No form or financial record exists to test for draft/data loss.

**Failure Indicators:** Crash, blank screen or inaccessible Go to Home control after rotation.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-08"></a>

### MT-08 — Open the shell without internet

**Feature:** Open the shell without internet.

**Coverage:** FR-27/57 distinction.

**Availability:** READY.

**Navigation:** Android Quick Settings → disable network → open app → Go to Home.

**Prerequisites:** No account or financial records are required. Note current Wi-Fi/mobile-data state.

**Test Steps:**

1. Turn off Wi-Fi and mobile data using Android Quick Settings.
2. Open the app and tap Go to Home.
3. Use Android Back once.
4. Restore the original network settings.

**Expected Result:** The local placeholder should still navigate. This proves only that the shell does not need internet; it does not prove offline financial entry, Firestore caching, Room or synchronization.

**Failure Indicators:** Shell crashes or does not navigate without a connection. Do not mark offline finance requirements PASS from this result.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-09"></a>

### MT-09 — Read and operate the button with TalkBack

**Feature:** Read and operate the button with TalkBack.

**Coverage:** Accessibility NFR.

**Availability:** READY.

**Navigation:** Android Settings → Accessibility → TalkBack → PersonalFinanceApp.

**Prerequisites:** Know how to disable TalkBack on your test device before enabling it; use a test emulator if preferred.

**Test Steps:**

1. Open Android Settings → Accessibility → TalkBack and turn it on.
2. Open PersonalFinanceApp and move accessibility focus to Login Screen and Go to Home.
3. Activate Go to Home using the TalkBack gesture (usually double-tap).
4. Listen for Home Screen and confirm the focus does not become trapped.
5. Turn TalkBack off using your device instructions.

**Expected Result:** Visible text/button should be announced and the button operable. There are no chart/icon semantics to validate in this build.

**Failure Indicators:** Unlabelled button, unreachable focus, trapped focus or crash.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-10"></a>

### MT-10 — Check minimum/current Android and phone sizes

**Feature:** Check minimum/current Android and phone sizes.

**Coverage:** Compatibility NFR.

**Availability:** READY.

**Navigation:** Test device app drawer → PersonalFinanceApp → Go to Home.

**Prerequisites:** Have an Android 8.0/API 26 emulator and a current Android phone/emulator; include about 5-inch and 7-inch profiles if available. Install the same audited APK on test devices only.

**Test Steps:**

1. On each test device, open PersonalFinanceApp.
2. Tap Go to Home, use Back, rotate, and repeat with large text/light/dark mode where supported.
3. Write each Android version/screen profile and observed result in Actual Result.
4. Do not call the whole device matrix PASS if only one device was available; list untested devices.

**Expected Result:** APK should install and shell navigation should work on supported API>=26 profiles without clipping/crashes. Build configuration alone does not establish these runtime results.

**Failure Indicators:** Install failure on a supported device, crash or inaccessible control. Device setup failures should be recorded separately from app defects.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-11"></a>

### MT-11 — Add/view/edit/delete income, currency and crypto entries

**Feature:** Add/view/edit/delete income, currency and crypto entries.

**Coverage:** FR-09/10/12/20/31/32.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for an income list, add-income button, edit/delete controls, currency/rate fields or crypto gain/loss selector. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Record Salary LKR 130,000; view it; edit to 131,000 and verify the same record changes; restore 130,000; create and delete a separate LKR 100 disposable income and verify it disappears. Complete Scenario A only when the actual entry labels/navigation are confirmed.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-12"></a>

### MT-12 — Create/rename/archive/reclassify categories

**Feature:** Create/rename/archive/reclassify categories.

**Coverage:** FR-25.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a category-management screen. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Create Test Groceries, choose Committed, rename it, then reclassify and verify existing expense analysis updates. Restore the intended classification. Archive only disposable categories. The fake supports create/rename/archive but not reclassification or retroactive analysis.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-13"></a>

### MT-13 — Add/view/edit/delete an expense and undo

**Feature:** Add/view/edit/delete an expense and undo.

**Coverage:** FR-24/28/31.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for an expense editor, transaction list or undo control. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Create a disposable LKR 100 expense, view it, edit to 150, and verify the total increases by 50. Undo the save if supported. Delete the disposable item, use immediate undo, and verify exactly one item is restored. Check currency/rate/category/channel/date/note fields separately; time, merchant and recurring origin are absent from the current model.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-14"></a>

### MT-14 — Back-date expenses and filter/search history

**Feature:** Back-date expenses and filter/search history.

**Coverage:** FR-29/32.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a date picker, catch-up list, transaction search box or filters. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Add disposable LKR 100 records dated 31 October and 1 November 2026; November must include only the latter. Test inclusive start/exclusive end boundaries; then category, merchant, channel, source and amount filters and gentle catch-up messages. The fakes provide date-range filtering only.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-15"></a>

### MT-15 — Create/edit/deactivate a recurring commitment

**Feature:** Create/edit/deactivate a recurring commitment.

**Coverage:** FR-33.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a recurring-cost list, template editor or pending due-item action. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Set Rent LKR 34,000 monthly on day 5 with a committed category. Confirm one pending occurrence; exactly one expense is created. Adjust the next disposable occurrence before confirming; deactivate the template and check future generation stops. Weekly/yearly template enums do not yet generate occurrences.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-16"></a>

### MT-16 — Manage multiple goals and contribution history

**Feature:** Manage multiple goals and contribution history.

**Coverage:** FR-36/38/44; proposed FR-59.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a goal list, goal editor, contribution action, reversal action or archive control. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Use Scenario C. Edit a disposable target/deadline while preserving contributions. Create a second goal and confirm each goal keeps its own history. Fake repository support exists but no phone flow, progress display or milestones exist.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-17"></a>

### MT-17 — Check the remaining promised screens and account features

**Feature:** Check the remaining promised screens and account features.

**Coverage:** FR-01–08/11/13–19/21–23/26–27/30/34–35/37/39–43/45–58.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Use only disposable test records when a real feature is available.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for registration/reset/Google/onboarding/account/settings, project/invoice/subscription, dashboard/insight/export/reminder controls. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: The individually described SRS behaviours must be accessible and connected before they can be tested. Do not infer Google sign-in, account deletion, CSV, transfers, chart drill-down, merchant memory, salary/AdSense reminders, stability or reality check from dependencies or models.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-18"></a>

### MT-18 — Scenario A — income total LKR 196,000

**Feature:** Scenario A — income total LKR 196,000.

**Coverage:** FR-09/10/15/20–22/48.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Fresh disposable account/dataset would be required; income UI and authentication are absent.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for income entry and a selected-month income total. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Use the exact transaction table below. All five entries dated 5 November 2026 should yield LKR 196,000; crypto subtotal must be +3,000, not +7,000. No UI path or result screen can be supplied on this commit.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-19"></a>

### MT-19 — Scenario B — expenses LKR 57,000; cash flow LKR 139,000

**Feature:** Scenario B — expenses LKR 57,000; cash flow LKR 139,000.

**Coverage:** FR-24/25/28/45–50.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Scenario A and an otherwise empty disposable November dataset would be required; both entry flows are absent.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for an expense entry flow, dashboard, category breakdown or summary. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Use the exact expense table below with Scenario A in the same month. Verify 57,000 spent and 139,000 cash flow. The current branch has no categories UI, dashboard, monthly/quarterly summary or charts.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-20"></a>

### MT-20 — Scenario C — MacBook goal and reversals

**Feature:** Scenario C — MacBook goal and reversals.

**Coverage:** FR-36–44; proposed FR-59.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Disposable goal; deadline 12 months after the test day. On 5 November 2026 use 5 November 2027.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for goal creation, initial savings, contribution history or reversal controls. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Use the goal checkpoints below. Full reversal negates one previous contribution once. Goal allocations are not expense entries in the current fake repository; no dashboard calculation is implemented.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-21"></a>

### MT-21 — Scenario D — Registration and login

**Feature:** Scenario D — Registration and login.

**Coverage:** FR-01/02.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for registration or real login controls. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Register two disposable accounts with email addresses you control; use different passwords. Verify sign-in and wrong-password rejection. No email/password/register UI or Auth SDK calls exist here.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-22"></a>

### MT-22 — Scenario D — Session after close and reopen

**Feature:** Scenario D — Session after close and reopen.

**Coverage:** FR-02.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for an authenticated login/logout flow. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Log in to test account A, close/force-stop/reopen, and verify the authenticated account remains A; logout should require sign-in. MT-04 checks only navigation, not identity.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-23"></a>

### MT-23 — Scenario D — Records after app restart

**Feature:** Scenario D — Records after app restart.

**Coverage:** FR-09/24/27/57.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for income/expense save and record-list controls. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Create a disposable Test Persistence expense of LKR 123; after save, force-stop/reopen and find exactly one record with the same values. Fake repository memory is not persistent storage.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-24"></a>

### MT-24 — Scenario D — Live Firebase synchronization

**Feature:** Scenario D — Live Firebase synchronization.

**Coverage:** FR-45/57.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a signed-in record list on two test devices. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: On two devices signed in as A, create Test Live LKR 124 on one and observe the same single item on the other; edit it and observe the change. No console access or real account is required. No Firestore listener exists here.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-25"></a>

### MT-25 — Scenario D — Disconnect from the internet

**Feature:** Scenario D — Disconnect from the internet.

**Coverage:** FR-27/57.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a financial screen with loaded records and offline state. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Load test records online, disable Wi-Fi/mobile data and observe cached data plus unobtrusive offline/retry state. A functioning Home label is not cached financial data.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-26"></a>

### MT-26 — Scenario D — Read previously loaded records offline

**Feature:** Scenario D — Read previously loaded records offline.

**Coverage:** FR-27; offline NFR.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a financial transaction list. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Load the disposable LKR 123 record online, disconnect, reopen and read its amount/category/date. Repeat after force-stop while still offline to distinguish durable storage from a retained screen/in-memory list.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-27"></a>

### MT-27 — Scenario D — Add an expense while offline

**Feature:** Scenario D — Add an expense while offline.

**Coverage:** FR-27.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for an expense add/save flow. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: With networking disabled, create Test Offline Add LKR 125 dated 5 November 2026. Check local list/total immediately; force-stop/reopen still offline and ensure the single new record survives. Current app cannot add a record.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-28"></a>

### MT-28 — Scenario D — Edit and delete records while offline

**Feature:** Scenario D — Edit and delete records while offline.

**Coverage:** FR-28; offline NFR.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for expense edit/delete controls. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Prepare two disposable records online; disconnect. Edit one from LKR 125 to 150 and delete the other. Reopen offline and check exactly those changes persist. Never delete real records.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-29"></a>

### MT-29 — Scenario D — Reconnect to the internet

**Feature:** Scenario D — Reconnect to the internet.

**Coverage:** FR-27/57.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a local queued change/offline indication. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Restore networking after offline changes; wait for any sync/pending indicator to settle and record its actual wording/time. No sync indicator or queue exists here; do not invent a timeout guarantee.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-30"></a>

### MT-30 — Scenario D — Confirm offline changes synchronized

**Feature:** Scenario D — Confirm offline changes synchronized.

**Coverage:** FR-27/57.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for a second device financial list. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: After reconnecting device 1, inspect test account A on device 2 online. Confirm exactly one new record, the edited amount and the deleted test item absent; compare totals and restart both apps. This tests observed sync, not whether Room is the mechanism.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

<a id="mt-31"></a>

### MT-31 — Scenario D — Different-account isolation

**Feature:** Scenario D — Different-account isolation.

**Coverage:** FR-05/07/57.

**Availability:** BLOCKED — no app route.

**Navigation:** Open PersonalFinanceApp → Go to Home → Home Screen. No financial menu or feature button exists.

**Prerequisites:** Safe test account(s)/devices and the named financial/authentication UI would be required. These prerequisites are absent on audited main. Do not use live personal financial data.

**Test Steps:**

1. Open PersonalFinanceApp. If Login Screen appears, tap Go to Home.
2. Inspect Home Screen for login/logout and account-specific financial lists. This build has only the Home Screen label.
3. Stop here: the required control does not exist. Do not tap an invented menu or enter values into another app/build.
4. Write "Blocked: required screen/control absent in main e0eda37" in Actual Result when you perform this check. Leave Verdict NOT TESTED for the unexecuted feature steps; if assessing requirement availability itself, record FAIL and explain the missing control.

**Expected Result:** Current source expectation: cannot execute this financial flow. Required acceptance behaviour after implementation: Create marker A-ONLY LKR 126 in account A. Log out, sign in as B, and verify it is absent, including after restart and offline reads. Switch back to A and verify it remains there. This UI check does not prove Security Rules reject malicious cross-account reads; separate emulator rule tests would be needed.

**Failure Indicators:** Missing control is the known implementation gap. Never record the financial operation PASS merely because the placeholder opens.

**Actual Result:**

**Verdict:** NOT TESTED / PASS / FAIL (current: NOT TESTED).

## Scenario A worksheet — expected income, not an observed app result

Linked case: MT-18. **Entry path: unavailable. Verification path: unavailable.** Open app → Go to Home reaches a label only. You cannot enter any row below into this build, so stop there. Each row needs its own received-income entry when an integrated income editor exists; exact source labels, entry controls, save buttons and total locations cannot be confirmed here.

Use a new empty test dataset. Use the same date, **5 November 2026**, and the same selected month, **November 2026**, for all records. If testing on a different day, choose one month and use it consistently.

| Separate intended entry | Original amount/currency | Applied LKR rate | Expected signed LKR receipt | Running income |
| --- | ---: | ---: | ---: | ---: |
| Salary | 130,000 LKR | 1 | 130,000 | 130,000 |
| Freelance | 45,000 LKR | 1 | 45,000 | 175,000 |
| AdSense | 60 USD | 300 | 18,000 | 193,000 |
| Crypto realised gain | 5,000 LKR | 1 | 5,000 | 198,000 |
| Crypto realised loss | 2,000 LKR loss magnitude | 1 | -2,000 | **196,000** |

Expected source totals: Salary 130,000; Freelance 45,000; AdSense 18,000; Crypto **3,000**. This is 130,000 + 45,000 + (60×300) + 5,000 − 2,000 = **196,000**. Treat all five as received/realised and exclude unrelated records. An unreceived receivable must not increase the total.

There is no current UI from which to choose a loss direction or decide whether to type a negative amount. Income holds both direction and Money, but fake saveIncome does not enforce their agreement. Never invent a loss-entry procedure or claim a displayed total of 196,000. No crypto net/YTD screen, source breakdown or monthly dashboard exists here. USDT/ETH can appear only as free-text assetSymbol in code, not as selectable CurrencyCode values.

When a future build supplies these controls, check the stored original 60 USD, rate 300 and fixed 18,000 LKR; a later default-rate change must not rewrite that historic receipt. That rate-change control is also absent here.

## Scenario B worksheet — expenses and combined cash flow

Linked case: MT-19. **Entry/list/chart/summary paths: unavailable.** Open app → Go to Home; no expense form, category picker, dashboard or summary exists. You cannot record these expenses in this build. Defaults below come from SRS Appendix E, not the app's three fake category seeds.

| Intended entry | LKR | Suggested SRS category | Intended classification | Running spending |
| --- | ---: | --- | --- | ---: |
| Rent | 34,000 | Rent & Housing | Committed | 34,000 |
| Groceries | 12,000 | Groceries | Committed | 46,000 |
| Coffee and dining | 8,000 | Coffee & Dining Out | Discretionary | 54,000 |
| Transport | 3,000 | Transport (commute) | Committed | **57,000** |

Use 5 November 2026 for each entry, LKR/rate 1, and no other test expenses. The assignment of Transport matters: Appendix E classifies commuting as committed, whereas the fake seed named Transport is discretionary. Since there is no reclassification UI, this difference cannot be corrected in the app yet.

Expected after Scenario A: 196,000 − 57,000 = **139,000 LKR net cash flow**, saving rate **70.9184%** (about **70.92%**). No actual saving-rate calculator or rounding convention is implemented.

| Split policy | Committed LKR | Discretionary LKR | Share of spending (committed / discretionary) | Share of income (committed / discretionary) |
| --- | ---: | ---: | --- | --- |
| Appendix E commute classification | 49,000 | 8,000 | 85.96% / 14.04% | 25.00% / 4.08% |
| If Transport is discretionary | 46,000 | 11,000 | 80.70% / 19.30% | 23.47% / 5.61% |

Only the arithmetic is verified here. The intended category totals are 34,000/12,000/8,000/3,000 with four records. No committed/discretionary chart, drill-down, dashboard or financial-summary view exists to display them. No monthly or quarterly summary is implemented. A future quarter containing these entries and no other records should agree with their month totals, but that is an acceptance target, not a present feature.

## Scenario C worksheet — goal, two contributions and one reversal

Linked cases: MT-16 and MT-20. **Goal list/create/details/contribute/reverse paths: unavailable.** There is no initial-savings field or contribution button in the app. In code, initial savings would require a separate OPENING contribution; a Goal alone has no saved-amount field. No automated or manual financial writes were performed by the audit.

Intended goal: **MacBook Pro**, target **LKR 490,000**, deadline **12 months after the test date** (5 November 2027 if testing on 5 November 2026). Intended opening saving: **LKR 11,200**. Choose the second contribution as **LKR 10,000** so each checkpoint is unambiguous. These progress figures are arithmetic acceptance targets; no current progress/calculation UI displays them.

| Intended checkpoint | Change | Sum of contribution ledger | Remaining to 490,000 | Progress |
| --- | ---: | ---: | ---: | ---: |
| Create goal and separate opening record | +11,200 | 11,200 | 478,800 | 2.2857% |
| First contribution | +20,000 | 31,200 | 458,800 | 6.3673% |
| Second contribution | +10,000 | 41,200 | 448,800 | 8.4082% |
| Reverse second contribution in full | -10,000 | **31,200** | **458,800** | **6.3673%** |

The fake repository requires a new reversal record pointing to the same-goal original, exactly negating its amount. An original contribution can be reversed only once. Identical retries with the same contribution ID succeed without adding another row; changing an existing ID is rejected. The original remains in history. These are code/unit-test findings, not controls you can operate on a phone.

For a second goal, an intended example is **Emergency Fund**, target LKR 100,000, opening 0, contribution 5,000. Each goal's observed contributions must stay separate; MacBook would remain at 31,200. Multiple IDs are supported in memory only. There is no second-goal form or progress display.

At exactly 12 months remaining, the intended initial requirement is 478,800/12 = **39,900 LKR/month**. If the first 20,000 contribution occurs immediately with 12 months still remaining, the target would be 458,800/12 = **38,233.33**. If one month has elapsed, the SRS Appendix D example uses 11 remaining months: 458,800/11 = **41,709.09**. No actual months-left/rounding/forecast policy is implemented, so none of these is claimed as an app output.

The fake goal repository does not create an expense or modify income when adding a contribution; the repositories are independent. Do not deduct contributions again from the Scenario A/B cash-flow arithmetic. There is no implemented dashboard to verify how a future version treats transfers to savings. Ahead/On track/Behind, projected dates, what-if deadlines, goal-denominated delays and milestone celebrations are all absent.

## Scenario D — distinguish cache, memory and actual offline support

MT-21–MT-31 individually cover the requested registration/login, session, record restart, live sync, disconnection, loaded reads, offline add, offline edit/delete, reconnect, queued-change sync and account isolation. They all stop at the missing-control checkpoint on this commit.

| Mechanism | Evidence in audited main | What a future phone test would and would not establish |
| --- | --- | --- |
| In-memory fake repositories | Implemented but unwired; lists live only inside each repository object | A unit-test save/read within one object is not durable/offline/cloud storage. |
| Firestore built-in cache | Firebase dependency exists, but no financial reads/writes/listeners exist | A previously loaded record showing offline could be SDK cache in a future Firebase build; that alone does not prove Room exists. |
| Room | No dependency, entity, DAO, database, module or integration | No Room-specific behaviour can be claimed. Runtime offline success alone would not establish the implementation mechanism. |
| Durable offline writes and reconnect | No write queue, local durable store or Firestore repository | Force-stop/reopen offline plus cross-device comparison after reconnect tests persistence/sync outcomes; source inspection must establish the mechanism. |
| Security Rules | No rules file/tests on this commit; deployed rules not accessed | UI account switching can reveal leaks but cannot prove server denial of malicious cross-account requests. |

Do not send notification tests, inspect real Firestore records, or alter Firebase settings/rules to work around the missing app UI. When an integrated build exists, use disposable A/B accounts and uniquely named records as described in the cases. Do not put passwords in the checklist.

## Coverage and what to test first

1. MT-01/02: confirm the installed APK matches this branch, then test its only button.
2. MT-03/04/08: Back, restart and network-independent shell; these are not finance/session passes.
3. MT-05–10: theme, font size, orientation, TalkBack and supported-device checks.
4. Record MT-11–31 as blocked when their missing prerequisite is observed. Authentication/data isolation, durable expense entry/sync, financial signs/totals and goal history are the first finance areas to test after integration.

| Scope | Cases | Coverage limit |
| --- | --- | --- |
| All reachable controls/theme/platform behaviours | MT-01–10 | Runtime checks ready; none executed in this audit |
| All 14 repository-only PARTIAL FRs | MT-11–16 and MT-18–20 | FR-09/10/12/20/24/25/28/29/31/32/33/36/38/44; no phone controls |
| Other SRS features | MT-17, MT-21–31 | Feature availability gaps, not invented screen instructions |
| Income/expense/goal worksheets | MT-18–20 | Exact expected arithmetic; no displayed results |
| All 11 Firebase/offline actions | MT-21–31 | Blocked until an integrated build is available |

The current app cannot meet the requested full finance walkthrough success condition because the financial UI does not exist on audited main. This guide covers every currently available behaviour and makes the remaining blocks explicit. Re-audit the connected feature implementation before replacing blocked checkpoints with actual Add/Edit/Delete/Save paths.
