# Lesson 14: Capstone

**Goal:** bring everything together. Write a realistic end-to-end journey, then plan and build out coverage for
a feature area by yourself.

**Time:** about half a day

## Part 1: The customer journey (guided)

Create `e2e-react/lesson14/customer-journey.spec.ts` as **one** test, driven through the UI with your page
objects and fixtures. Add what your page objects don't have yet: an `OpenAccountPage` for `/accounts/new`, and on
`CustomerDetailPage` an `openAccount()` method and `status` and `accountRows` locators. Group the test into `test.step`s so
the report and trace read as the story:

1. Sign in as teller and create a new customer (unique data). Expect them to be "Active" with no accounts.
2. From their page, open a **Current** account called "Spending", then a **Savings** account called "Goals".
   Read the ids from the URLs (`/accounts/{id}`).
3. Deposit **500.00** into Spending, then transfer **200.00** to Goals. Check the balance after each step.
4. Go back to the customer page. Expect 2 account rows, with Spending at £300.00 and Goals at £200.00.
5. Try to **close Goals** (accept the dialog). Expect "Account balance must be zero before it can be closed".
6. Transfer the 200.00 back, and close Goals. Expect it to show "Closed".
7. Finally, **check through the API**: Spending is 500, Goals is `CLOSED`, and Spending has exactly 3 transactions.

**Reference:** `lesson14/customer-journey.spec.ts`

> Long journey tests are valuable, but there should be **few** of them. They're slower and harder to diagnose.
> Most coverage belongs in focused UI tests (lessons 06 and 09) and API tests (lesson 08).

## Part 2: Own a feature (independent)

Pick **one** area the reference answers don't fully cover. Write a short test plan (5–10 bullet points: what,
at which level, and why), then build it. Ideas:

- **Accounts list filters:** status and type combinations, "Showing N accounts", and the empty state. How do you
  check counts when other tests create accounts at the same time?
- **Opening accounts:** validation (no holder, no type), that suspended customers can't be chosen, and the
  nickname limit: the field stops at 40 characters (`maxlength`), so the server's "too long" rule is one to
  check through the API.
- **Customer search:** by email, partial name, combined with the status filter, the "Clear" link, and what the
  URL says after each search.
- **Account deletion:** "Delete account" only appears on accounts with no history. Delete one, and check where
  you land and the message.
- **Dashboard totals:** create, fund and close accounts through the API, and watch "Total balance held" and
  "Active accounts" change. What can you assume while other tests run in parallel?
- **Error pages:** `/accounts/999999` shows "Page not found"; a page whose API call fails shows "Something went
  wrong" (mock it with `page.route`).
- **Cross-browser:** add `firefox` and `webkit` projects to your config (`devices['Desktop Firefox']`,
  `devices['Desktop Safari']`), install those browsers (`npx playwright install firefox webkit`), and run the
  smoke tests on all three.
- **Visual check:** `await expect(page).toHaveScreenshot()` on a page with fixed data. What makes it flaky,
  and how do `mask` and mocked data help?

### Definition of done

- [ ] Runs green 3 times in a row, in parallel
- [ ] No seeded data changed; no `waitForTimeout`
- [ ] Locators only in page objects; data set up through `LedgerApi` where it isn't the thing under test
- [ ] Each test's title says what behaviour it checks
- [ ] A colleague reviewed it

## Where to go next

- [ARIA snapshots](https://playwright.dev/docs/aria-snapshots): check a page's accessible structure in one go
- [Visual comparisons](https://playwright.dev/docs/test-snapshots): `toHaveScreenshot`
- [Emulation](https://playwright.dev/docs/emulation): devices, locale, time zone and colour scheme
- [Clock](https://playwright.dev/docs/clock): control time in the browser
- [Release notes](https://playwright.dev/docs/release-notes): read them before each upgrade
