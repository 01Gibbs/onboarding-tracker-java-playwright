# Answers: Lesson 13, fix the broken suite

> **Spoilers.** Try each case yourself first. The diagnosis is the point of the exercise, not the fix.

Every fix below was checked by running the corrected suite twice.

---

### Case 01: `a deposit shows the new balance`

- **Symptom:** `Expected: "£1250.00"`, `Received: "£1,250.00"`.
- **Evidence:** the error message itself. The app formats money with a thousands separator.
- **Root cause:** the expected value was typed from memory instead of taken from the product.
- **Fix:** `toHaveText('£1,250.00')`.
- **Principle:** read `Expected` and `Received` character by character before opening anything else (lesson 02).

### Case 02: `a withdrawal reduces the balance`

- **Symptom:** `Expected: "90.00"`, `Received: 90`.
- **Evidence:** one is a string, the other a number. The API returns `balance` as a JSON number.
- **Root cause:** comparing a number with a string; `toBe` never treats them as equal.
- **Fix:** `expect(await api.balanceOf(id)).toBe(90)`, or compare the text the page shows (`balanceDisplay`)
  with `'£90.00'`.
- **Principle:** compare money as the type the API returns (lesson 08).

### Case 03: `a teller opens the customers from the dashboard`

- **Symptom:** `strict mode violation: getByRole('link', { name: 'Customers' }) resolved to 2 elements`.
- **Evidence:** the message lists both: the nav link, and the dashboard's "Customers" summary card, which is
  also a link.
- **Root cause:** a locator that's ambiguous on this page (names match by substring by default).
- **Fix:** scope it: `page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'Customers' })`.
  `exact: true` also works here, but scoping says what you mean.
- **Principle:** strictness is a feature; narrow the locator (lessons 03 and 05).

### Case 04: `a deleted customer disappears`

- **Symptom:** `expect(locator).toContainText("deleted")` fails: the flash is not found.
- **Evidence:** in the trace, the click on "Delete customer" happens, but there is no `DELETE /api/customers/…`
  in the Network tab and the page stays put. The button asks for confirmation with `window.confirm`.
- **Root cause:** Playwright **dismisses dialogs** unless a handler is registered, so the delete was cancelled.
- **Fix:** handle the dialog before clicking: `page.once('dialog', d => d.accept())`, or use the page object's
  `deleteAndConfirm()`.
- **Principle:** lesson 06.

### Case 05: `a transfer moves money to another account`

- **Symptom:** a timeout on `selectOption`; the call log says `element is not visible`.
- **Evidence:** the trace snapshot shows "To account" hidden while "Deposit" is selected.
- **Root cause:** the steps are in the wrong order. "To account" only appears after choosing Transfer.
- **Fix:** `await account.chooseType('Transfer')` **before** `account.toAccount.selectOption(...)`.
- **Principle:** auto-waiting waits for an element to become actionable; it can't put your steps in the right
  order (lesson 09).

### Case 06: `a teller freezes an account`

- **Symptom:** a timeout clicking "Freeze account".
- **Evidence:** the trace snapshot shows "Signed in as **Alex Auditor (Auditor)**", and there are no action
  buttons on the page.
- **Root cause:** the test starts from the **auditor's** storage state, and the auditor is read-only.
- **Fix:** `test.use({ storageState: 'playwright/.auth/broken-teller.json' })`.
- **Principle:** when an element "isn't there", check *who* is signed in and *what state* the page is in before
  blaming the locator (lesson 10).

### Case 07: `the dashboard shows an empty state when there is no activity`

- **Symptom:** `expected to contain "No activity yet."`, but the panel shows real activity.
- **Evidence:** the trace's Network tab shows `GET /api/activity?limit=8` answered by the server, not fulfilled
  by the route. The route was added in time, but its pattern is `**/api/activity`.
- **Root cause:** glob patterns must match the **whole** URL, including the query string, so the route never
  matched.
- **Fix:** `page.route('**/api/activity*', ...)`.
- **Principle:** lesson 11. (The other classic: registering the route *after* the request has been made.)

### Case 08: `refreshing shows the latest activity`

- **Symptom:** `expected the new deposit`: `expect(received).toContainEqual(expected)`, where `received` is the
  list from *before* the refresh, or an empty one.
- **Evidence:** `allTextContents()` reads the list **once, right now**. The trace shows it was read while the panel
  still said "Loading activity…" (the activity API takes a second here), and only afterwards did the new entry
  arrive.
- **Root cause:** a **non-retrying** check of something that changes asynchronously, after a request the page
  makes in the background.
- **Fix:** a web-first assertion that retries until it's true:
  `await expect(dashboard.activityItems.filter({ hasText: description })).toBeVisible()`.
- **Principle:** use web-first assertions, never `expect(await locator.allTextContents())` or
  `expect(await locator.count())` on a changing page (lesson 02).

### Case 09: `deposit into the account`

- **Symptom:** `TypeError: Cannot read properties of undefined (reading 'id')`.
- **Evidence:** the file runs its tests in order, and `deposit into the account` comes **first**, before
  `open an account` has set the shared `account` variable. (In parallel, the two could even run in different
  workers, which never share module variables.)
- **Root cause:** one test depends on state another test creates.
- **Fix:** each test arranges its own data: create the account inside `deposit into the account` with
  `api.accountWithBalance('0')`, and delete the now-pointless `open an account` test and the shared variable.
- **Principle:** tests must be independent, with no shared mutable state (lessons 04, 06 and 12).

### Case 10: `a teller creates a customer`

- **Symptom:** passes on the first run against a fresh app, then fails with
  `expected "Customer Quinn Example created"`. The form is shown again with "A customer with this email
  already exists".
- **Evidence:** the trace snapshot after "Save customer" shows the error under Email.
- **Root cause:** hard-coded test data. The first run created the customer, so every later run collides.
- **Fix:** `uniqueLastName()` and `uniqueEmail('quinn')`, and expect the message with that name.
- **Principle:** own your data and make it unique (lessons 05 and 06). Restarting the app only hides the bug.
