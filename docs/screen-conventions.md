# Shared Screen Conventions
**SE3092 Personal Finance App** | **Version 1.0** | **16 October 2026**
*Prepared by Himasha for Malindu, Himasha, and Raveen. Status: agreed by the team.*

This guide records the team’s agreed visual style and conventions for Compose screens, navigation, forms, feedback, and accessibility. Each member applies these conventions to their own features to maintain a consistent experience across the application.

---

## 1. Colour and Typography

### Colour Palette & Usage
| Use | Colour and Convention |
| :--- | :--- |
| **Primary surfaces** | `#0C5037` dark green for the sign-in background, Overview header, amount panels, and main action buttons. |
| **Secondary surfaces** | `#E7F5EE` pale green for icon circles, selected options, and active navigation backgrounds. |
| **Accent** | `#A2F649` lime for Sign in and completed progress fills. Use black text on lime. |
| **Special icon controls** | `#025A40` for the Income arrow circle and Savings Add circle; white glyphs. |
| **Text** | Primary `#111111`; secondary `#6B6B6B` on light surfaces. White primary and readable light grey secondary text on dark surfaces. No blue secondary labels. |
| **Expenses and errors** | Muted coral such as `#D86B61`, supported by a minus sign, message, or error icon. Check contrast at the actual text size. |
| **Neutral surfaces** | White cards and fields; light grey borders and unfilled progress tracks. |

### Typography
Use the shared Material 3 typography theme. Suggested sizes:
- **Titles**: 24–28sp
- **Main totals**: 32–36sp
- **Body**: 14–16sp
- **Supporting text**: 12–14sp

Use sentence case and familiar words. These dimensions are starting values, not fixed limits when text scales.

---

## 2. Layout and Icons

- **Padding & Spacing**: Use approximately 20dp horizontal padding and spacing based on 8dp increments.
- **Corners**: Use 20–24dp card corners and 12–16dp field/button corners.
- **Form Behavior**: Keep forms scrollable and respect system bars and the keyboard. Avoid fixed heights that clip content.
- **Icon Style**: Use consistent dark green outline icons on pale green badges. Keep standalone category/calendar form icons without extra badges.
- **Symbol System**:
  - Cash uses a banknote symbol.
  - Overview & Goals use the colourful laptop artwork.
  - Emergency fund uses the colourful shield and heart.
  - Google sign-in uses the multicolour Google G.
  - Decorative credit cards and bubbles stay on sign-in only.

---

## 3. Shared Components and Navigation

| Component | Convention |
| :--- | :--- |
| **Buttons** | Main actions use dark green with white labels. Sign in uses lime with black text. Use specific labels such as *Save expense*. Show disabled and progress states without changing button size. |
| **Fields** | Persistent visible label, clear required/optional status, helpful keyboard type, and inline error. Hints do not replace labels. Password fields support show/hide. |
| **Selections** | One choice for payment/spending type. Use pale green selection fill plus a check, radio state, or selected semantics. Keep displayed labels consistent with shared model values. |
| **Cards and progress** | White rounded cards with restrained border/shadow. Show saved, target, and remaining values beside progress; do not rely on the bar alone. |
| **Dialogs and sheets** | Use for confirmations and short choices. Give a clear title and named actions. Delete requires confirmation. Cancel preserves the original record. |

### Navigation Behavior
- **Main destinations**: Overview, Activity, Goals, and More. The active tab has a pale green background and dark green label/icon.
- **Bottom Navigation**: Hide bottom navigation on authentication and focused create/edit forms.
- **Action Placement**: Place an obvious Add action in Activity, offering Income or Expense.
- **Form Back Behavior**: Forms use a back arrow. An untouched form closes; a changed form asks *Discard changes* or *Keep editing*.
- **Post-Save & Logout**: After a confirmed save, return to the relevant list/detail screen and reflect the saved record. Logout clears protected navigation and previous user-scoped screen state.

---

## 4. Validation and Screen States

### Expense Fields
Amount/currency, category, date, payment method, spending classification, and optional note. Keep the note limit and the mapping of Essential/Optional labels consistent with the shared model and validation rules. Display a reporting period for totals and use the agreed available-money calculation.

### Screen States
| State | Expected Screen Behaviour |
| :--- | :--- |
| **Ready** | Show sensible defaults and clear actions. Expense date defaults to today; do not silently assume an unselected required category. |
| **Invalid input** | Show a field-specific message after submission or field interaction. Amount must be valid and positive. Preserve inputs and move focus to the first error where useful. |
| **Loading** | Show a progress indicator or skeleton with a meaningful label. Preserve the screen structure; disable only actions that depend on unavailable data. |
| **Empty** | Explain what is missing and offer an appropriate next action, such as *Add expense* or *Create category*. An empty dataset is not an error. |
| **Saving** | Show *Saving* and prevent duplicate submissions. Keep entered values visible. |
| **Failure** | Show a short understandable message and *Retry* where appropriate. Keep form values. Do not expose raw exceptions or Firebase error codes. |
| **Offline or pending** | Show available cached data and an accurate offline/pending status. Do not claim a cloud save is confirmed while a write remains pending. |
| **Success** | Show a brief confirmation and refresh the relevant state. Preserve the selected reporting period or filter where practical. |

---

## 5. Accessibility Conventions

| Area | Requirement |
| :--- | :--- |
| **Touch and focus** | Every interactive target is at least 48dp, including visually small Google, plus, and visibility controls. Use logical keyboard and screen-reader focus order. |
| **Contrast** | Check each text/background combination. Target at least 4.5:1 for normal text and 3:1 for large text. Dark green labels are preferable to lime text on white. |
| **Text scaling** | Support Android font scaling. Allow wrapping and scrolling; do not truncate essential amounts, labels, or errors. |
| **Semantics** | Provide meaningful labels for icon-only controls, for example *Add goal* and *Show password*. Exclude purely decorative artwork from accessibility announcements. |
| **Feedback** | Announce important errors and operation results. Selected controls expose selected state. Use text and symbols alongside colour. |
| **Charts and money** | Provide readable values or a table alongside charts. Show currency and consistent decimal formatting. Make positive/negative values understandable without colour. |
| **Forms** | Associate errors with fields. Keep labels visible. Allow the focused field and its error to remain visible above the keyboard. |

---

## 6. Compose Implementation Conventions

- Implement reusable themed components for buttons, fields, selection controls, money summaries, goal cards, and state messages.
- Store user-facing text in string resources (`strings.xml`).
- Use the existing project theme (`PersonalFinanceAppTheme`) and dependencies rather than creating a separate design system per feature.
- **Architecture**: Composable screens render state and send events. ViewModels own screen behaviour and expose immutable UI state (`StateFlow`). Repositories handle data access. Reuse shared money/date helpers so UI formatting and financial meaning stay consistent.
