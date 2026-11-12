# Answers: Lesson 13, fix the broken suite

> **Spoilers.** Try each case yourself first. The diagnosis is the point of the exercise, not the fix.

Every fix below was verified by running the corrected suite twice against a freshly started app.

---

### Case 01: `depositShowsTheNewBalance`

- **Symptom:** `Locator expected to have text. Expected: £1250.00, Received: £1,250.00`
- **Evidence:** the error message itself. The app formats money with a thousands separator.
- **Root cause:** the expected value was typed from memory instead of taken from the product.
- **Fix:** `hasText("£1,250.00")`
- **Principle:** read `Expected` and `Received` character by character before you open anything else
  (lesson 02).

### Case 02: `withdrawalReducesTheBalance`

- **Symptom:** `expected: <90> but was: <90.00>`
- **Evidence:** the two values are numerically equal, but `BigDecimal.equals` also compares **scale**
  (`90` has scale 0, `90.00` has scale 2).
- **Root cause:** a Java gotcha, not a Playwright one.
- **Fix:** `assertEquals(new BigDecimal("90.00"), …)`, or
  `assertEquals(0, new BigDecimal("90").compareTo(balance))` to ignore scale.
- **Principle:** compare money with `compareTo`, or use values whose scale matches (lesson 08).

### Case 03: `tellerOpensCustomersFromTheDashboard`

- **Symptom:** `strict mode violation: getByRole(LINK, name "Customers") resolved to 2 elements`
- **Evidence:** the message lists both matches: the nav link, and the dashboard's "Customers" summary card,
  which is also a link.
- **Root cause:** a locator that's ambiguous on this page. (Names match by substring by default.)
- **Fix:** scope the locator to the main navigation:
  `page.getByRole(NAVIGATION, name "Main").getByRole(LINK, name "Customers")`. `setExact(true)` also works
  here, but scoping says what you mean.
- **Principle:** strictness is a feature; narrow the locator (lessons 03 and 05).

### Case 04: `deletedCustomerDisappears`

- **Symptom:** `Locator expected to contain text "deleted"`, with no flash message at all.
- **Evidence:** in the trace, the click on "Delete customer" happens, but the page never navigates, and
  there's no `POST …/delete` in the Network tab. The button is protected by a `confirm()` dialog
  (`data-confirm` on the form, which you can see in the DOM snapshot), and nothing handled it.
- **Root cause:** Playwright **auto-dismisses dialogs** unless a handler is registered, so the
  confirmation was cancelled.
- **Fix:** `page.onceDialog(dialog -> dialog.accept());` **before** the click (or use the page object's
  `deleteAndConfirm()`).
- **Principle:** lesson 06.

### Case 05: `transferMovesMoneyToAnotherAccount`

- **Symptom:** `Timeout 5000ms exceeded` on `selectOption`. The call log says `element is not visible`.
- **Evidence:** the trace snapshot shows the "To account" field is hidden while "Deposit" is selected.
- **Root cause:** the steps are in the wrong order. "To account" only appears after choosing Transfer.
- **Fix:** call `account.chooseType("Transfer")` **before** `toAccount.selectOption(...)`.
- **Principle:** auto-waiting waits for an element to become *actionable*. It can't make your steps happen
  in the right order (lesson 09).

### Case 06: `tellerFreezesAnAccount`

- **Symptom:** a timeout clicking "Freeze account".
- **Evidence:** the trace snapshot shows "Signed in as **Alex Auditor (Auditor)**", and the action buttons
  aren't on the page.
- **Root cause:** the test signs in as the wrong user. `User.AUDITOR` is read-only.
- **Fix:** `loginAs(User.TELLER)`.
- **Principle:** when an element "isn't there", check *who* is signed in and *what state* the page is
  in before you blame the locator (lesson 10).

### Case 07: `dashboardShowsAnEmptyStateWhenThereIsNoActivity`

- **Symptom:** `expected to contain "No activity yet."`, but the panel shows real activity.
- **Evidence:** the snapshot shows real activity entries, which the mock (`[]`) could never produce. In
  the Actions list, `navigate` (inside `open()`) comes **before** `route`.
- **Root cause:** the route was registered **after** the page had already made the request.
- **Fix:** call `page.route(...)` **before** `dashboard.open()`.
- **Principle:** set up routes before the request happens (lesson 11).

### Case 08: `refreshingShowsRecentActivity`

- **Symptom:** `expected some recent activity but found 0`. This can occasionally pass on a fast machine,
  but it's still broken.
- **Evidence:** this is a plain JUnit assertion, and `count()` returns *immediately*. The trace shows the
  count happened while the panel said "Loading activity…".
- **Root cause:** a **non-retrying** check of a value that changes asynchronously after `fetch()` returns.
- **Fix:** `assertThat(dashboard.activityItems().first()).isVisible();` or `assertThat(items).not().hasCount(0)`.
- **Principle:** use web-first assertions, never `assertTrue(locator.count() > 0)` (lesson 02).

### Case 09: `depositIntoTheAccount`

- **Symptom:** `NullPointerException … accountId is null`.
- **Evidence:** `@TestMethodOrder(MethodName)` runs `depositIntoTheAccount` **before** `openAccount`, so the
  static field hasn't been set yet. (Without the ordering annotation, the order would be undefined and the
  test would fail only some of the time.)
- **Root cause:** one test depends on state that another test creates.
- **Fix:** each test arranges its own data. Create the account inside `depositIntoTheAccount` (through
  `LedgerApi`), and delete the now-pointless `openAccount` test and the static field.
- **Principle:** tests must be independent, with no shared mutable state (lessons 04, 06 and 12).

### Case 10: `tellerCreatesACustomer`

- **Symptom:** passes on the first run, then fails with
  `expected "Customer Quinn Example created"`. The page shows the form again with
  "A customer with this email already exists".
- **Evidence:** the trace snapshot after "Save customer" shows the validation error under Email.
- **Root cause:** hard-coded test data. The first run created the customer, so every later run collides.
- **Fix:** use `TestData.uniqueEmail("quinn")` and `TestData.uniqueLastName()`, and assert on those values.
- **Principle:** own your data and make it unique (lessons 05 and 06). Restarting the app only hides the bug.
