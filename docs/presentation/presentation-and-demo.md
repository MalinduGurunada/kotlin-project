# Presentation Slides, Speaker Notes & Live Demonstration Script
**SE3092 Personal Finance App** | **Author: Himasha** | **Date: 29 October 2026**

---

## 1. Slide Outline & Architecture Diagrams

### Slide 1: Personal Finance Tracker — Architecture Overview
- **UI Layer**: Jetpack Compose, Material 3, Navigation Compose with protected route guards (`AppNavHost`).
- **ViewModel Layer**: Hilt `@HiltViewModel` exposing immutable `StateFlow` (`DashboardUiState`, `ExpenseUiState`, `AuthUiState`).
- **Data Layer**: Cloud Firestore (`/users/{userId}/expenses`), Room SQLite (`ReminderEntity`), and `FirebaseAuth`.

### Slide 2: Security & Multi-User Data Scoping
- **Firestore Security Rules**: User document ownership enforcement (`request.auth.uid == userId`), schema field/type validation, ownership anti-forging checks (`data.userId == userId`), and default deny-all fallback.

---

## 2. Live Demonstration Script

1. **Authentication Flow**:
   - Register a new account (`himasha.p@example.lk`).
   - Sign in and verify automatic navigation to `OverviewScreen` Dashboard.
2. **Expense Management**:
   - Create a new expense item -> verify instant list reflection and LKR total calculation.
   - Edit and delete an expense item.
3. **Dashboard & Analytics**:
   - Demonstrate live `Total Available Balance`, `Income` vs `Expenses`, `Committed (67%)` vs `Discretionary (33%)` progress bar, and `Active Savings Goal` (70%).
4. **Local Room Notifications & Logout**:
   - Demonstrate local Room reminder due alerts.
   - Perform Sign Out -> verify backstack clearance and local session cleanup.
