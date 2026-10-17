# Annotated Screen Wireframes & Figma Documentation
**SE3092 Personal Finance App** | **Version 1.0** | **16 October 2026**
*Prepared by Himasha for SE3092 Team (Malindu, Himasha, Raveen)*

This directory contains the annotated UI screen designs, Figma project links, and scenario rationale for the Personal Finance App.

---

## 1. Figma Links & Assets

| Asset Type | Link / Description | Status |
| :--- | :--- | :--- |
| **High Fidelity Wireframes (Main Workspace)** | [PBD High Fidelity Figma Design](https://www.figma.com/design/t4EF4GdEHwEuP1Ns3bFfG2/PBD?node-id=0-1&t=WqbO9ZY0j3ggrXoW-1) | Verified / Team Approved |
| **Feature Screens Wireframes (Income, Expense, etc.)** | [PBD Feature Screen Wireframes](https://www.figma.com/design/t4EF4GdEHwEuP1Ns3bFfG2/PBD?node-id=21-756&t=WqbO9ZY0j3ggrXoW-1) | Active / Complete |
| **Exported Screen PNGs** | `docs/wireframes/exports/` | In Progress |

---

## 2. Core Screen Wireframe Specifications

### 2.1 Expense Management Screen (`ExpenseListScreen` & `ExpenseEditorDialog`)
*Direct Figma Node*: [Expense Wireframe Frame](https://www.figma.com/design/t4EF4GdEHwEuP1Ns3bFfG2/PBD?node-id=21-756&t=WqbO9ZY0j3ggrXoW-1)

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
*Direct Figma Node*: [Income Wireframe Frame](https://www.figma.com/design/t4EF4GdEHwEuP1Ns3bFfG2/PBD?node-id=21-756&t=WqbO9ZY0j3ggrXoW-1)

- **Key Fields**: Source (`SALARY`, `FREELANCE`, `ADSENSE`, `CRYPTO`), Entry Kind (`RECEIPT`, `REALIZED_PNL`), Amount, Date, Project Reference.
- **States Covered**: Active filters by date range, receipt summary cards.

### 2.3 Savings & Goals Screen
*Direct Figma Node*: [Savings & Goals Frame](https://www.figma.com/design/t4EF4GdEHwEuP1Ns3bFfG2/PBD?node-id=21-756&t=WqbO9ZY0j3ggrXoW-1)

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
