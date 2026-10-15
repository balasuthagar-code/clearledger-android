# Firestore Data Contract & Architecture Spec

> **Status:** Draft / Proposed — Team Confirmation Required  
> **Target Audience:** All ClearLedger Team Members  
> **Root Path:** `users/{uid}`

---

## A. Collection Paths & Schemas

All user-scoped documents reside under `users/{uid}/...`. Document IDs are auto-generated Firestore UIDs unless specified otherwise.

The nine required collections under `users/{uid}` are:
1. `incomeSources`
2. `incomeEntries`
3. `projects`
4. `cryptoTrades`
5. `categories`
6. `expenses`
7. `recurring`
8. `goals`
9. `checkins`

### 1. `users/{uid}/incomeSources/{sourceId}`
- **Purpose:** Stores recurring or one-off income source definitions (e.g., Salary, Freelance).
- **Fields & Types:**
  - `sourceId` (String, Required) — Document ID.
  - `name` (String, Required) — Name of the income source.
  - `defaultAmountMinor` (Long, Optional) — Default amount in minor currency units.
  - `currencyCode` (String, Required) — ISO 4217 currency code (e.g., `"USD"`).
  - `createdAt` (Timestamp, Required) — Creation timestamp.
  - `updatedAt` (Timestamp, Required) — Last modification timestamp.

---

### 2. `users/{uid}/incomeEntries/{entryId}`
- **Purpose:** Records actual income received against sources or projects.
- **Fields & Types:**
  - `entryId` (String, Required) — Document ID.
  - `sourceId` (String, Optional) — Reference to `incomeSources/{sourceId}`.
  - `projectId` (String, Optional) — Reference to `projects/{projectId}`.
  - `amountMinor` (Long, Required) — Amount received in minor units.
  - `currencyCode` (String, Required) — ISO 4217 currency code.
  - `incomeDate` (Timestamp, Required) — Date income was received (represented as Firestore Timestamp).
  - `monthKey` (String, Required) — Formatted as `YYYY-MM` (e.g., `"2026-10"`).
  - `notes` (String, Optional) — Description or notes.
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 3. `users/{uid}/projects/{projectId}`
- **Purpose:** Tracks income/expense projects, clients, or budget campaigns.
- **Fields & Types:**
  - `projectId` (String, Required) — Document ID.
  - `name` (String, Required) — Project title.
  - `status` (String, Required) — e.g., `"ACTIVE"`, `"COMPLETED"`.
  - `targetBudgetMinor` (Long, Optional).
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 4. `users/{uid}/cryptoTrades/{tradeId}`
- **Purpose:** Logs cryptocurrency transactions, trades, and holdings.
- **Fields & Types:**
  - `tradeId` (String, Required) — Document ID.
  - `assetSymbol` (String, Required) — e.g., `"BTC"`, `"ETH"`.
  - `tradeType` (String, Required) — e.g., `"BUY"`, `"SELL"`.
  - `quantityBaseUnits` (Long, Required) — Quantity in satoshis or base units (scaled integer, e.g., $1\text{ BTC} = 10^8$ satoshis). *Note: Precision and range limitations apply; not all assets use 8 decimal places (proposed team agreement for high-precision tokens).*
  - `unitPriceMinor` (Long, Required) — Price per unit in minor currency units (e.g., USD cents). *Proposed — team confirmation required for sub-cent crypto pricing precision.*
  - `totalAmountMinor` (Long, Required) — Total trade fiat value in minor units.
  - `currencyCode` (String, Required) — ISO 4217 currency code for fiat valuation.
  - `tradeDate` (Timestamp, Required).
  - `monthKey` (String, Required) — `YYYY-MM`.
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 5. `users/{uid}/categories/{categoryId}`
- **Purpose:** Expense and income category classifications (e.g., Food, Utilities).
- **Fields & Types:**
  - `categoryId` (String, Required) — Document ID.
  - `name` (String, Required).
  - `type` (String, Required) — `"EXPENSE"` or `"INCOME"`.
  - `icon` (String, Optional).
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 6. `users/{uid}/expenses/{expenseId}`
- **Purpose:** Records individual expense transactions.
- **Fields & Types:**
  - `expenseId` (String, Required) — Document ID.
  - `categoryId` (String, Required) — Reference to `categories/{categoryId}`.
  - `projectId` (String, Optional) — Reference to `projects/{projectId}`.
  - `amountMinor` (Long, Required) — Expense amount in minor units.
  - `currencyCode` (String, Required) — ISO 4217 currency code.
  - `expenseDate` (Timestamp, Required) — Firestore Timestamp.
  - `monthKey` (String, Required) — `YYYY-MM`.
  - `payee` (String, Optional).
  - `notes` (String, Optional).
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 7. `users/{uid}/recurring/{recurringId}`
- **Purpose:** Recurring bills, subscriptions, or recurring income/expenses.
- **Fields & Types:**
  - `recurringId` (String, Required) — Document ID.
  - `name` (String, Required).
  - `type` (String, Required) — `"INCOME"` or `"EXPENSE"`.
  - `amountMinor` (Long, Required).
  - `currencyCode` (String, Required) — ISO 4217 currency code.
  - `frequency` (String, Required) — e.g., `"MONTHLY"`, `"WEEKLY"`.
  - `nextDueDate` (Timestamp, Required).
  - `categoryId` (String, Optional).
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 8. `users/{uid}/goals/{goalId}`
- **Purpose:** Savings or debt payoff goals.
- **Fields & Types:**
  - `goalId` (String, Required) — Document ID.
  - `title` (String, Required).
  - `targetAmountMinor` (Long, Required).
  - `currentAmountMinor` (Long, Required).
  - `currencyCode` (String, Required) — ISO 4217 currency code.
  - `targetDate` (Timestamp, Optional).
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

### 9. `users/{uid}/checkins/{checkinId}`
- **Purpose:** Financial check-ins, journal entries, or periodic summaries.
- **Fields & Types:**
  - `checkinId` (String, Required) — Document ID.
  - `checkinDate` (Timestamp, Required).
  - `monthKey` (String, Required) — `YYYY-MM`.
  - `netWorthMinor` (Long, Optional).
  - `currencyCode` (String, Required) — ISO 4217 currency code.
  - `mood` (String, Optional).
  - `notes` (String, Optional).
  - `createdAt` (Timestamp, Required).
  - `updatedAt` (Timestamp, Required).

---

## B. Money Conventions

1. **Storage Format:** All monetary amounts are stored as integer `Long` minor units (e.g., cents, paise) in fields named `amountMinor`, `targetBudgetMinor`, etc.
2. **Precision & Scale Validation:** Supported scale is between 0 and 4 inclusive (default 2 decimal places). Unrepresentable precision (e.g., passing `"10.123"` when scale is 2) is strictly rejected without silent rounding.
3. **Overflow Handling:** Values exceeding `Long` range (`Long.MAX_VALUE`) throw an exception rather than silently capping or altering amounts.
4. **Currency Identification:** Every monetary collection explicitly identifies currency via a `currencyCode` field storing an ISO 4217 currency string (e.g., `"USD"`).
5. **Floating-Point Prohibition:** `Float` and `Double` are strictly prohibited for monetary representation and calculation.
6. **Crypto Quantities vs. Money:** Cryptocurrency quantities are tracked separately via `quantityBaseUnits` (`Long`) and do not force fractional crypto quantities into fiat minor unit conventions.

---

## C. Date & Month Conventions

1. **Firestore Timestamps:** All dates, times, and deadlines (`createdAt`, `updatedAt`, `expenseDate`, `incomeDate`, `tradeDate`, `checkinDate`, `targetDate`) are stored as Firestore `Timestamp` objects (represented in documentation examples as ISO 8601 strings translating to Timestamps).
2. **Server Timestamps:** `createdAt` and `updatedAt` utilize `FieldValue.serverTimestamp()` on writes where appropriate.
3. **Month Key (`monthKey`):**
   - Format: `YYYY-MM` (e.g., `"2026-10"`).
   - Derived explicitly using an injected or specified business `TimeZone` (e.g., `TimeZone.getTimeZone("UTC")` or user preference) rather than silently relying on device defaults.
   - Must be updated if the source transaction date changes.

---

## D. Relationships & Ownership

1. **Ownership Scope:** All data belongs exclusively to the authenticated user under `users/{uid}`.
2. **References:** Document references (like `categoryId` or `projectId`) are stored as string IDs belonging to the same `users/{uid}` document space.
3. **Deletion Policy:** Deleting a referenced document (e.g., a category or project) does **not** automatically clear or cascade delete its IDs in referencing documents (e.g., expenses). Application logic must handle orphaned references gracefully.
4. **Security Rules:** Enforced via `firestore.rules`:
   ```rules
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;

         match /{subcollection}/{document=**} {
           allow read, write: if request.auth != null && request.auth.uid == userId
               && (subcollection == "incomeSources" ||
                   subcollection == "incomeEntries" ||
                   subcollection == "projects" ||
                   subcollection == "cryptoTrades" ||
                   subcollection == "categories" ||
                   subcollection == "expenses" ||
                   subcollection == "recurring" ||
                   subcollection == "goals" ||
                   subcollection == "checkins");
         }
       }
     }
   }
   ```

---

## E. Query & Index Contract

| Feature / Use Case | Collection | Filter Fields | Sort Fields & Directions | Scope | Required Composite Index | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| List expenses by month | `expenses` | `monthKey == "YYYY-MM"` | `expenseDate DESC` | User (`users/{uid}`) | `monthKey` (ASC), `expenseDate` (DESC) | Proposed |
| List income by month | `incomeEntries` | `monthKey == "YYYY-MM"` | `incomeDate DESC` | User (`users/{uid}`) | `monthKey` (ASC), `incomeDate` (DESC) | Proposed |
| Crypto trades by month | `cryptoTrades` | `monthKey == "YYYY-MM"` | `tradeDate DESC` | User (`users/{uid}`) | `monthKey` (ASC), `tradeDate` (DESC) | Proposed |
| Active projects | `projects` | `status == "ACTIVE"` | `createdAt DESC` | User (`users/{uid}`) | `status` (ASC), `createdAt` (DESC) | Proposed |

---

## F. Team Agreement Checklist

- [ ] Collection and field names agreed.
- [ ] Required fields and relationships agreed.
- [ ] Currency and money precision (`Long` minor units) agreed.
- [ ] Crypto quantity representation agreed.
- [ ] Business timezone and `monthKey` derivation agreed.
- [ ] Query patterns and composite indexes agreed.
- **Review Status:** Pending Team Sign-Off.
