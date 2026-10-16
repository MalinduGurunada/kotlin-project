# Annotated Screen Wireframes & Figma Documentation
**SE3092 Personal Finance App** | **Version 1.0** | **16 October 2026**
*Prepared by Himasha for SE3092 Team (Malindu, Himasha, Raveen)*

This directory contains the annotated UI screen designs, Figma project links, and scenario rationale for the Personal Finance App.

---

## 1. Figma Links & Assets

| Asset Type | Link / Path | Status |
| :--- | :--- | :--- |
| **Figma File** | `[Insert Figma Workspace Link]` | Draft / In Review |
| **Figma Prototype** | `[Insert Interactive Prototype Link]` | Active |
| **Exported Screen PNGs** | `docs/wireframes/exports/` | Pending Asset Additions |

---

## 2. Core Screen Wireframe Specifications

### 2.1 Expense Management Screen (`ExpenseListScreen` & `ExpenseEditorDialog`)
- **Key Fields**:
  - Original Amount (Decimal) & Currency Selector (LKR / USD)
  - Category Selection Dropdown
  - Payment Channel Dropdown (`CASH`, `CARD`, `BANK`, `OTHER`)
  - Spending Classification (`COMMITTED` vs `DISCRETIONARY`)
  - Occurred Date Picker & Optional Note Field
- **Validation Criteria**:
  - Positive non-zero amount decimal required.
  - Category selection required.
  - Inline error messages associated directly with input fields.
- **States Covered**:
  - **Ready**: Pre-filled sensible defaults (e.g. today's date).
  - **Loading**: Skeleton placeholder while fetching monthly records.
  - **Empty**: Informative empty state prompting "+ Add Expense".
  - **Saving**: Progress spinner on button preventing duplicate writes.

### 2.2 Income Screen
- **Key Fields**: Source (`SALARY`, `FREELANCE`, `ADSENSE`, `CRYPTO`), Entry Kind (`RECEIPT`, `REALIZED_PNL`), Amount, Date, Project Reference.
- **States Covered**: Active filters by date range, receipt summary cards.

### 2.3 Savings & Goals Screen
- **Key Fields**: Goal Name, Target Amount, Target Date, Contributions.
- **Visual Display**: Progress fills with target and remaining amounts explicitly displayed.

---

## 3. Scenario Rationale

These wireframes address Kavindu's personal finance requirements by providing:
1. **Low Friction Input**: Clean forms respecting keyboard type and soft-keyboard padding.
2. **Clear Currency Visibility**: Explicit LKR base conversion for multi-currency transactions.
3. **Actionable Overviews**: Distinguishing committed expenses from discretionary choices to highlight savings opportunities.

---

## 4. Next Steps for Team

1. Export high-resolution PNG / PDF wireframe mockups from Figma.
2. Place PNG mockups into `docs/wireframes/exports/`.
3. Update Figma workspace links above.
