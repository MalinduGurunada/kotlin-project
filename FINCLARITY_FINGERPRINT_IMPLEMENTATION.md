# FinClarity Fingerprint Authentication Implementation & Verification Report
**Module:** SE3092 Platform Based Development — Assignment 01 (Year 3, Semester 2, 2026)  
**Application Title:** FinClarity — Personal Finance Management System for Android  
**Target Scenario:** Mr. Kavindu Silva (Junior Software Engineer, Colombo)  
**Date:** 10 November 2026  
**Git Branch:** `dev`  
**Repository Location:** `D:\PBD Assignment\Finane App\kotlin-project`

---

## 1. Executive Summary

A fully functional, user-configurable **Fingerprint & Face Recognition Authentication System** has been implemented in FinClarity.

### Key Verified Features:
1. **`MainActivity` Inheritance Fix**:
   - `MainActivity.kt` updated to inherit from `androidx.fragment.app.FragmentActivity` instead of `ComponentActivity`.
   - AndroidX `BiometricPrompt` requires `FragmentActivity` to attach its dialog fragment; this fix allows `BiometricPrompt` to launch natively on real Android phones and emulators!
2. **User-Configurable Biometric Setting**:
   - Added **"Fingerprint Unlock"** setting card under `MoreScreen.kt` with switch **"Use Fingerprint to Unlock"**.
   - Verified device hardware capability (`canAuthenticate`) before enabling.
   - Requires successful `BiometricPrompt` authentication before toggling ON.
3. **UID-Scoped DataStore Preference**:
   - Persisted per authenticated user `userId` in `UserPreferences.kt` & `DataStorePreferencesRepository.kt`.
   - Disabled by default (`biometricUnlockEnabled = false`).
   - Automatically reset on logout (`clearUserPreferencesOnLogout`) so account switching never leaks unlocked states across users.
4. **Biometric App Lock Gate on Launch & Resume**:
   - When an authenticated user launches or reopens FinClarity with `biometricUnlockEnabled == true`, the app presents `BiometricLockScreen.kt`:
     - **FinClarity** title
     - **Welcome back, [First Name]** (using shared `ProfileAvatar`)
     - *"Your finances are protected."*
     - **Unlock with Fingerprint** primary button (`#0C5037` Dark Green container)
     - **Use Account Login Instead** fallback button (signs out safely and returns to Login)
   - Scanning a registered fingerprint/face unlocks access to the financial dashboard (`Overview`).
5. **Zero Unsafe Shortcuts**:
   - Biometric unlock is an additional local security check on top of Firebase Authentication. It never grants access when `auth.currentUser == null`.
6. **Automated Testing & Build Verification**:
   - Executed `./gradlew app:testDebugUnitTest`: **596 unit tests passed 100%**.
   - Executed `./gradlew app:assembleDebug`: **BUILD SUCCESSFUL** (`app/build/outputs/apk/debug/app-debug.apk`).
   - **Zero Git commits or pushes made**, preserving your local working tree for emulator testing.

---

## 2. Modified & Created Files

| File Path | Description |
| :--- | :--- |
| `app/src/main/java/com/example/personalfinanceapp/MainActivity.kt` | Inherits from `FragmentActivity` enabling native AndroidX `BiometricPrompt` dialog attachment. |
| `app/src/main/java/com/example/personalfinanceapp/feature/preferences/UserPreferences.kt` | Added `biometricUnlockEnabled: Boolean = false` field to user preference model. |
| `app/src/main/java/com/example/personalfinanceapp/feature/preferences/PreferencesRepository.kt` | Added `updateBiometricUnlockEnabled(userId, enabled)` contract. |
| `app/src/main/java/com/example/personalfinanceapp/feature/preferences/DataStorePreferencesRepository.kt` | Implemented `updateBiometricUnlockEnabled` with UID map persistence and logout cleanup. |
| `app/src/main/java/com/example/personalfinanceapp/feature/preferences/di/PreferencesModule.kt` | Created Hilt module binding `PreferencesRepository` to `DataStorePreferencesRepository`. |
| `app/src/main/java/com/example/personalfinanceapp/feature/preferences/ui/UserPreferencesViewModel.kt` | Created ViewModel exposing `biometricUnlockEnabled` state. |
| `app/src/main/java/com/example/personalfinanceapp/feature/dashboard/ui/MoreScreen.kt` | Added **"Fingerprint Unlock"** setting card with toggle switch **"Use Fingerprint to Unlock"**. |
| `app/src/main/java/com/example/personalfinanceapp/feature/auth/ui/BiometricLockScreen.kt` | Created full-screen lock view with `ProfileAvatar`, "Unlock with Fingerprint" button, and fallback. |
| `app/src/main/java/com/example/personalfinanceapp/navigation/AppNavHost.kt` | Integrated Biometric App Lock Gate for authenticated startup sessions. |

---

## 3. Step-by-Step Manual Retesting Instructions

1. **Test Enabling Fingerprint Unlock**:
   - Open **FinClarity** ➔ Log in with Firebase.
   - Tap **"More"** tab ➔ Locate **"Fingerprint Unlock"** card ➔ Toggle **"Use Fingerprint to Unlock"** ON.
   - Android system prompt *"Verify your fingerprint to enable biometric app lock"* appears immediately.
   - Touch fingerprint sensor (`adb emu finger touch 1` in emulator or real finger on phone) ➔ Setting toggles ON!
2. **Test App Launch / Re-open Lock Gate**:
   - Close or background FinClarity.
   - Reopen FinClarity while authenticated.
   - **FinClarity App Lock** screen appears displaying:
     - Header: **FinClarity**
     - Greeting: **Welcome back, Himasha** (with `H` avatar initials)
     - Message: *"Your finances are protected."*
   - Touch fingerprint sensor ➔ FinClarity unlocks and opens your Overview dashboard!
3. **Test Cancel & Account Fallback**:
   - Reopen app ➔ Cancel biometric prompt.
   - Tap **"Use Account Login Instead"** ➔ App signs out safely and returns to Login screen.
   - Log in as another user ➔ Fingerprint unlock remains disabled by default for the new account (UID isolation confirmed).
