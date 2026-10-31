# Live Demonstration Script & Release Evidence
**SE3092 Personal Finance App** | **Author: Himasha** | **Date: 31 October 2026**

---

## 1. APK Build & Verification

### Build Debug Release APK
From the project root, execute:
```powershell
.\gradlew.bat :app:assembleDebug
```
- **Output Artifact**: `app/build/outputs/apk/debug/app-debug.apk`
- **Verification**: Executed `./gradlew app:testDebugUnitTest` (500 unit tests passed).

---

## 2. Income & Backend Live Demonstration Script

1. **Firebase Configuration Setup**:
   - Verify `app/google-services.json` is configured for project `se3092-personal-finance`.
2. **User Authentication Flow**:
   - Launch app -> Sign up with `himasha.p@example.lk` and `Thilanka@99`.
   - Verify user UID maps directly to Firestore `/users/{userId}` document.
3. **Income Management & USD Conversion**:
   - Add Income entry in USD ($150.00 USD).
   - Demonstrate automatic LKR base conversion (`LKR 48,250.00`) and live sync to Cloud Firestore.
4. **Receivables & Goal Progress**:
   - View `MacBook Pro M3 Fund` savings goal (70% lime progress bar, remaining LKR 165,000).
5. **Offline & System State**:
   - Disconnect network -> demonstrate cached snapshot display and offline indicator.
