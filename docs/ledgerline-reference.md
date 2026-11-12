# Ledgerline reference

Ledgerline is the system under test: a small, self-contained Spring Boot app for managing
**customers**, their **accounts** and the **money moving between them**. Everything runs in memory
(H2), and all data is fictional. The data is re-seeded every time the app starts.

- UI: <http://localhost:8080>
- API: <http://localhost:8080/api/...>
- Health: <http://localhost:8080/api/health>

## Users

| Username  | Password       | Role    | Can do                                                    |
|-----------|----------------|---------|-----------------------------------------------------------|
| `teller`  | `teller-pass`  | Teller  | Everything                                                |
| `auditor` | `auditor-pass` | Auditor | Read only. Write requests get **403**, and edit controls are hidden |
| `locked`  | `locked-pass`  | Teller  | Nothing. Sign-in is refused with "This user is locked…"   |

## Seeded data

These references and account numbers are predictable because ids restart on every reset.

| Customer       | Ref      | Status    | Accounts                                                                                     |
|----------------|----------|-----------|----------------------------------------------------------------------------------------------|
| Alice Archer   | CUS-0001 | Active    | `10000001` Everyday (Current, £1,250.00), `10000002` Rainy Day (Savings, £5,000.00)          |
| Ben Carter     | CUS-0002 | Active    | `10000003` Main (Current, £320.50), `10000004` Holiday Fund (Savings, £800.00, **Frozen**)    |
| Chloe Davies   | CUS-0003 | Active    | `10000005` Bills (Current, £0.00, no history)                                                |
| Dan Evans      | CUS-0004 | Suspended | `10000006` Old Account (Current, £0.00, **Closed**)                                          |
| Erin Foster    | CUS-0005 | Active    | none (so she can be deleted)                                                                  |

> **Treat seeded data as read-only in tests.** Tests that change data should create their own
> (lesson 06 onwards). That way they can run in any order, in parallel, and repeatedly.

## Business rules (good things to test)

| Rule | UI behaviour | API behaviour |
|------|--------------|---------------|
| Customer email must be unique (case-insensitive) | Error under Email | `409` |
| Customers with accounts cannot be deleted | Red flash message | `409` |
| Suspended customers cannot open accounts | "Open account" hidden; form error | `422` |
| Amount must be > 0, ≤ 1,000,000, max 2 decimals | Error under Amount | `422` |
| No withdrawal/transfer beyond the balance | "Insufficient funds. Available balance is £X" | `422` |
| Frozen/closed accounts cannot send or receive money | "Move money" hidden | `422` |
| Cannot transfer to the same account | Error under To account | `422` |
| Only zero-balance accounts can be closed; closed is final | Red flash message | `409` |
| Accounts with history cannot be deleted (close them instead) | "Delete account" hidden | `409` |
| Auditors cannot change anything | Controls hidden; direct POST → 403 page | `403` |

The UI waits **~750 ms** whenever money is moved (`ledgerline.processing-delay-ms`). This simulates a
slow downstream system and shows a "Processing…" button. It's there to show off Playwright's
auto-waiting.

## UI map

| Page | URL | Notable elements |
|------|-----|------------------|
| Sign in | `/login` | Labels "Username", "Password"; button "Sign in"; errors use `role="alert"` |
| Dashboard | `/` | Summary cards (`data-testid="stat-customers"`, `stat-active-accounts`, `stat-total-balance`); **Recent activity** region loaded by `fetch('/api/activity?limit=8')`, with loading, empty and error states and a "Refresh" button |
| Customers | `/customers` | Search form (`role="search"`, "Search customers", placeholder "Name or email", "Status"); table named "Customers"; rows `data-testid="customer-row"`; `data-testid="result-count"` |
| New / edit customer | `/customers/new`, `/customers/{id}/edit` | "First name", "Last name", "Email", "Phone (optional)"; button "Save customer"; each error is the field's accessible description (`aria-describedby`), plus `aria-invalid` and `data-testid="error-summary"` |
| Customer | `/customers/{id}` | Buttons "Edit", "Suspend customer"/"Reactivate customer", "Delete customer" (**confirm dialog**); "Accounts" table; "Open account" link |
| Accounts | `/accounts` | Filters "Status", "Type", button "Apply filters"; rows `data-testid="account-row"` |
| Open account | `/accounts/new?customerId=` | "Account holder" select, "Account type" radios (Current/Savings), "Nickname" |
| Account | `/accounts/{id}` | `data-testid="account-balance"`, `account-number`, `status-badge`; "Freeze/Unfreeze account", "Close account" and "Delete account" (both **confirm dialogs**); **Move money** region: "Type" radios Deposit/Withdrawal/Transfer, "Amount", "To account" (only visible for Transfer), "Description (optional)", "Submit" (shows "Processing…"); "Transactions" table with a "Show" filter; rows `data-testid="transaction-row"` |

Flash messages after an action use `data-testid="flash-message"`. Success messages have
`role="status"` and errors have `role="alert"`.

## JSON API

Authenticate with `POST /api/auth/login` and send `Authorization: Bearer <token>` on later requests.
A signed-in browser session works too: the dashboard's own `fetch()` uses it.

| Method | Path | Body / query | Success |
|--------|------|--------------|---------|
| POST | `/api/auth/login` | `{"username","password"}` | `200 {"token","username","displayName","role"}` |
| GET | `/api/health` | none (no auth needed) | `200 {"status":"UP"}` |
| GET | `/api/customers` | `?q=&status=ACTIVE\|SUSPENDED` | `200 [Customer]` |
| GET | `/api/customers/{id}` | none | `200 Customer` |
| POST | `/api/customers` | `{"firstName","lastName","email","phone"?}` | `201 Customer` + `Location` |
| PUT | `/api/customers/{id}` | same as POST | `200 Customer` |
| PATCH | `/api/customers/{id}/status` | `{"status":"SUSPENDED"}` | `200 Customer` |
| DELETE | `/api/customers/{id}` | none | `204` |
| GET | `/api/accounts` | `?status=&type=&customerId=` | `200 [Account]` |
| GET | `/api/accounts/{id}` | none | `200 Account` |
| POST | `/api/accounts` | `{"customerId","type":"CURRENT\|SAVINGS","nickname"}` | `201 Account` |
| PATCH | `/api/accounts/{id}` | `{"nickname"?, "status"?:"ACTIVE\|FROZEN\|CLOSED"}` | `200 Account` |
| DELETE | `/api/accounts/{id}` | none | `204` |
| GET | `/api/accounts/{id}/transactions` | `?type=DEPOSIT\|WITHDRAWAL\|TRANSFER_IN\|TRANSFER_OUT` | `200 [Entry]` (newest first) |
| POST | `/api/accounts/{id}/deposits` | `{"amount", "description"?}` | `201 Entry` |
| POST | `/api/accounts/{id}/withdrawals` | `{"amount", "description"?}` | `201 Entry` |
| POST | `/api/transfers` | `{"fromAccountId","toAccountId","amount","description"?}` | `201 Account` (the source account) |
| GET | `/api/activity` | `?limit=8` (1–50) | `200 [Entry]` across all accounts |
| POST | `/api/testing/reset` | none (no auth needed) | `200 {"customers":5,"accounts":6}`. Re-seeds everything |

Errors always have this shape:

```json
{ "status": 422, "error": "Unprocessable Content",
  "message": "Insufficient funds. Available balance is £10.00",
  "fieldErrors": { "amount": "Insufficient funds. Available balance is £10.00" } }
```

| Status | Meaning |
|--------|---------|
| 400 | Bean validation failed (`fieldErrors` lists each field) or the JSON was malformed |
| 401 | Missing, invalid or unknown token |
| 403 | Signed in as auditor and tried to write |
| 404 | Unknown id |
| 409 | Conflict with current state (duplicate email, delete with accounts, close with balance…) |
| 422 | Business rule broken (insufficient funds, frozen account…) |

## Configuration

`app/src/main/resources/application.yml`. You can override any value on the command line, e.g.
`./mvnw -pl app spring-boot:run -Dspring-boot.run.arguments="--server.port=9090 --ledgerline.processing-delay-ms=2000"`.
