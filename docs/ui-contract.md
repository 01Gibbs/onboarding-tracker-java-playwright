# The UI contract

Every Ledgerline front end (Thymeleaf, React and Angular) keeps this contract, so the same tests work
against all of them, and each track's course teaches the same skills. It is what a test may rely on: URLs,
roles and accessible names, test ids, and the exact text of messages. Styling and markup beyond that are free.

**How it is checked.** Each TypeScript track's reference answers (`e2e-react/solutions/`, `e2e-angular/solutions/`) run against every front end:
`npm run contract`. A front end that fails them breaks the contract, even if its own tests pass.

## Pages and URLs

| URL | Title | What a test can find |
|-----|-------|----------------------|
| `/login` | `Sign in · Ledgerline` | heading "Sign in to Ledgerline"; fields labelled "Username" and "Password"; button "Sign in" |
| `/` | `Dashboard · Ledgerline` | heading "Dashboard"; test ids `stat-customers` (link to `/customers`), `stat-active-accounts` (link to `/accounts?status=ACTIVE`), `stat-total-balance`; region "Recent activity" |
| `/customers` | `Customers · Ledgerline` | form `role="search"` with "Search customers" (placeholder "Name or email"), "Status", button "Search", link "Clear" while searching; `result-count` ("1 customer", "5 customers"); table "Customers" with rows `customer-row`; link "New customer" (tellers) |
| `/customers/new`, `/customers/{id}/edit` | `New customer · Ledgerline` / `Edit customer · Ledgerline` | fields "First name", "Last name", "Email", "Phone (optional)"; button "Save customer"; link "Cancel" |
| `/customers/{id}` | the customer's full name | h1 with the name and status badge; groups "Reference", "Email", "Phone" ("Not provided"), "Customer since"; buttons "Suspend customer"/"Reactivate customer", "Delete customer"; link "Edit"; table "Accounts" with rows `account-row` or "This customer has no accounts yet."; link "Open account" (tellers, active customers) |
| `/accounts` | `Accounts · Ledgerline` | form "Filter accounts" with "Status", "Type", button "Apply filters"; `result-count` ("Showing 1 account"); table "Accounts" with rows `account-row`; link "Open account" (tellers) |
| `/accounts/new?customerId=` | `Open account · Ledgerline` | "Account holder" select (active customers, "Name (CUS-0001)"); group "Account type" (a fieldset) with radios "Current" and "Savings"; "Nickname"; button "Open account" |
| `/accounts/{id}` | the account's nickname | h1 with nickname and status badge; `account-number`; `account-balance`; buttons "Freeze account"/"Unfreeze account", "Close account", "Delete account" (only when it can be deleted); region "Move money"; table "Transactions" with rows `transaction-row`, filtered by "Show" |
| anything else, or a record that can't be loaded | `Error · Ledgerline` | heading "Page not found" (404), "Access denied" (403) or "Something went wrong"; `error-message`; link "Back to dashboard" |

Anonymous visitors to any page except `/login` end up on `/login`. A signed-in user who opens `/login` goes to `/`.

## On every signed-in page

- Navigation "Main" with links "Dashboard", "Customers", "Accounts"; the current section's link has
  `aria-current="page"`.
- `current-user`: "Signed in as Terry Teller (Teller)".
- Button "Sign out". Afterwards `/login` shows the info message "You have been signed out".
- Status badges: `status-badge`, text "Active", "Frozen", "Closed" or "Suspended".

## Messages

After an action, a message appears with `data-testid="flash-message"`: `role="status"` for success and info,
`role="alert"` for errors. Its text comes from the API where the API provides it (a movement's `message`, an
error's `message`); otherwise it is one of these:

| After | Message |
|-------|---------|
| Wrong password, locked user | the API's message, e.g. "Invalid username or password" (username kept, password emptied) |
| Creating / updating a customer | "Customer Ada Lovelace created" / "Customer Ada Lovelace updated" |
| Suspending / reactivating | "Customer Ada Lovelace is now suspended" / "… is now active" |
| Deleting a customer | "Customer Ada Lovelace deleted" (on `/customers`) |
| Opening an account | "Account 10000007 opened" (on the new account's page) |
| Freezing, unfreezing, closing | "Account 10000001 is now frozen" / "… active" / "… closed" |
| Deleting an account | "Account 10000007 deleted" (on the holder's page) |
| Moving money | the receipt's message, e.g. "Deposit of £25.50 completed" |

When recent activity can't be loaded, the dashboard's activity panel shows the alert "Could not load recent
activity. Use Refresh to try again." (`role="alert"`).

Deleting a customer, closing an account and deleting an account ask first with the browser's confirm dialog:
"Delete customer Ada Lovelace? This cannot be undone.", "Close account 10000001? Closed accounts cannot be
reopened.", "Delete account 10000001?".

## Forms

- Each invalid field has `aria-invalid="true"`, and its error is the field's accessible description
  (`aria-describedby`), so `toHaveAccessibleDescription("Nickname is required")` works everywhere.
- Every field error, the same in the forms and in the API's `fieldErrors`:

  | Field | Messages |
  |-------|----------|
  | First name, Last name | "First name is required", "First name must be 50 characters or fewer" (and the same for Last name) |
  | Email | "Email is required", "Enter a valid email address", "Email must be 120 characters or fewer", "A customer with this email already exists" |
  | Phone | "Phone must contain 7-20 digits and may start with +" |
  | Account holder, Account type, Nickname | "Choose an account holder", "Accounts cannot be opened for a suspended customer", "Choose an account type", "Nickname is required", "Nickname must be 40 characters or fewer" (renaming: "Nickname must be between 1 and 40 characters") |
  | Type | "Choose a type" |
  | Amount | "Amount is required", "Amount must be greater than zero", "Amount must not exceed 1,000,000.00", "Amount can have at most 2 decimal places", "Insufficient funds. Available balance is …" |
  | To account | "Choose an account to transfer to", "Cannot transfer to the same account", "Account … is frozen and cannot be used" |
  | Description | "Description must be 80 characters or fewer" |

- On the customer and open-account forms, field errors also show `error-summary` ("Please fix the highlighted
  fields.", `role="alert"`). "Move money" has none.
- An error that belongs to no field shows above the form with `role="alert"`, e.g. moving money on an account
  that can't be used: "Account 10000004 is frozen and cannot be used".
- "Move money": radios "Deposit", "Withdrawal", "Transfer" (Deposit first and checked); "Amount";
  "To account" (only shown for Transfer, options "Everyday (10000001) – Alice Archer"); "Description
  (optional)"; button "Submit", which reads "Processing…" and is disabled while the deliberately slow request
  runs.

## Who sees what

Auditors see no "New customer", "Edit", "Suspend/Reactivate", "Delete", "Open account", account status
buttons, or "Move money". The server refuses their writes with `403` whatever the page shows.

## Notes for single-page front ends

- Data arrives after the page appears. A test that asserts something is **absent** passes instantly on a page
  that hasn't loaded yet, so assert something that is present first.
- Until the summary arrives, the dashboard's figures show "…", so each card's name already differs from the nav
  link's ("Customers…", not "Customers"). Thymeleaf renders the figures with the page.
- After a filter or search, the previous rows can stay on screen until the new ones arrive. Use assertions
  that retry (`toHaveCount`, `filter({ hasNot })`) rather than looping over a snapshot from `all()`.
- Sign in with the server session (`POST /api/auth/session`), so storage state and `page.request` behave
  as they do for the Thymeleaf pages.
