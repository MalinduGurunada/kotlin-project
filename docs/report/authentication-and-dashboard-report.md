# Technical Report Section — Authentication, Expense Management, Dashboard & Reminders
**SE3092 Personal Finance App** | **Author: Himasha** | **Date: 29 October 2026**

---

## 1. Authentication & Session Management (Task H01)

### Architecture
- **Repository Pattern**: Implemented `AuthRepository` interface and `FirebaseAuthRepository` using Firebase Auth SDK.
- **Dependency Injection**: Bound using `@Binds` in `AuthModule` with Hilt `@Singleton` lifecycle scope.
- **Session State Flow**: Exposes observable `observeAuthState(): Flow<UserId?>` which automatically updates upon sign-in/sign-out events.
- **Navigation Guarding**: `AppNavHost` observes `AuthState` flow and automatically redirects unauthenticated requests to `LoginScreen`. On sign-out, the backstack is cleared (`popUpTo(0)`) so the Android back button cannot reopen protected user screens.

---

## 2. Expense Management & Cloud Persistence (Task H03 & H04)

### Firestore Data Scoping
- Expenses are persisted under `/users/{userId}/expenses/{expenseId}` in Cloud Firestore.
- **Security Rules (`firestore.rules`)**:
  - Enforces `request.auth.uid == userId` for document read, write, and delete operations.
  - Enforces field validation (`baseAmountMinor >= 0`, `spendingClass in ['COMMITTED', 'DISCRETIONARY']`, `paymentChannel in ['CASH', 'CARD', 'BANK', 'OTHER']`).
  - Enforces ownership anti-forging (`request.resource.data.userId == request.auth.uid`).

---

## 3. Live Monthly Dashboard & Analytics (Task H05 & H06)

### Reactive State Flow
- `DashboardViewModel` combines reactive `Flow` streams from `IncomeRepository`, `ExpenseRepository`, and `GoalRepository`.
- **Calculations**:
  - **Total Available Balance**: Income LKR - Expense LKR.
  - **Outflow Ratios**: Committed (67%) vs Discretionary (33%) progress bar split.
  - **Active Savings Goal**: MacBook Pro M3 Fund with 70% progress bar.
  - **Timeline Activity**: Combined income and expense activity list sorted by date.

---

## 4. Local Room Reminders & Notifications (Task H07)

### Architecture
- **Room Database**: UID-scoped `ReminderEntity` and `ReminderDao` storing due bill schedules and notification history.
- **WorkManager & Channels**: Scheduled worker jobs checking due items and triggering notifications on `reminders_channel`.
- **Session Cleanup**: Logout action clears local Room reminders and cancels pending background workers.
