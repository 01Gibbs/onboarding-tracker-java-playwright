# Lesson 14: Capstone

**Goal:** bring everything together. Write a realistic end-to-end journey, then plan and build out
coverage for a feature area by yourself.

**Time:** about half a day

## Part 1: The customer journey (guided)

Create `workshop/lesson14/CustomerJourneyTest.java` as **one** test, driven through the UI using your
page objects:

1. Sign in as teller and create a new customer (unique data). Assert that they are "Active" with no accounts.
2. From their page, open a **Current** account called "Spending", then a **Savings** account called "Goals".
   Read the ids from the URLs (`/accounts/{id}`).
3. Deposit **500.00** into Spending, then transfer **200.00** to Goals. Assert the balance after each step.
4. Go back to the customer page. Assert 2 account rows, with Spending at £300.00 and Goals at £200.00.
5. Try to **close Goals** (accept the dialog). Assert "Account balance must be zero before it can be closed".
6. Transfer the 200.00 back, and close Goals. Assert that it shows "Closed".
7. Finally, **verify through the API**: Spending is 500.00, Goals is `CLOSED`, and Spending has exactly 3 transactions.

**Reference:** `solutions/lesson14/CustomerJourneyTest.java`

> Long journey tests are valuable, but there should be **few** of them. They're slower and harder to
> diagnose. Most coverage belongs in the focused UI tests (lessons 06/09) and API tests (lesson 08).

## Part 2: Own a feature (independent)

Pick **one** area that the reference solutions don't fully cover. Write a short test plan (5–10
bullet points: what, at which level, and why), then implement it. Ideas:

- **Accounts list filters:** status and type combinations, "Showing N accounts", and the empty state.
  How do you assert counts when other tests create accounts at the same time?
- **Opening accounts:** validation (no holder, no type, nickname too long), and that suspended
  customers can't be chosen.
- **Customer search:** by email, partial name, combined with the status filter, and the "Clear" link.
- **Account deletion:** "Delete account" only appears on accounts with no history. Delete one, and
  check the redirect and the flash message.
- **Dashboard totals:** create, fund and close accounts through the API, and check how "Total balance
  held" and "Active accounts" change. Be careful: what assumptions can you make while other tests
  run in parallel?
- **Error pages:** `/accounts/999999` shows "Page not found"; the auditor's direct POST shows "Access denied".
- **Cross-browser:** run the smoke suite on Firefox and WebKit using `Options.setBrowserName(...)`, driven
  by a `-Dbrowser=` system property.

### Definition of done

- [ ] Runs green 3 times in a row, in parallel, with `-Dheadless=true`
- [ ] No seeded data modified; no `Thread.sleep`
- [ ] Locators only in page objects; data set up through `LedgerApi` where it isn't the thing under test
- [ ] Each test name says what behaviour it checks
- [ ] A colleague reviewed it

## Where to go next

- [ARIA snapshots](https://playwright.dev/java/docs/aria-snapshots): assert a page's accessible structure in one go (screenshot-comparison assertions are only in Playwright's Node.js test runner, not Java)
- [Emulation](https://playwright.dev/java/docs/emulation): devices, locale, time zone and colour scheme
- [Clock](https://playwright.dev/java/docs/clock): control time in the browser
- [Release notes](https://playwright.dev/java/docs/release-notes): check them before each upgrade
