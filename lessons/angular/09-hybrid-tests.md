# Lesson 09: Hybrid tests (arrange with the API, act and assert in the UI)

**Goal:** test the "Move money" features quickly and reliably. Create exactly the data each test needs through
the API, then exercise the UI, with fixtures doing the wiring.

**Time:** about 2 hours

**Docs:** [Fixtures](https://playwright.dev/docs/test-fixtures) · [API testing: preparing server state](https://playwright.dev/docs/api-testing) · [Auto-waiting](https://playwright.dev/docs/actionability)

## Concepts

### Custom fixtures

`page` and `request` are fixtures: you ask for them by name and Playwright builds them, hands them over, and
tidies up afterwards. You can add your own with `test.extend`:

```ts
export const test = base.extend<{ api: LedgerApi; accountPage: AccountPage }>({
  api: async ({ playwright, baseURL }, use) => {
    const api = await LedgerApi.signIn(playwright.request, baseURL!, TELLER)   // set up
    await use(api)                                                             // the test runs here
    await api.dispose()                                                        // tidy up
  },
  accountPage: async ({ page }, use) => use(new AccountPage(page)),
})
```

Tests then import that `test` instead of the one from `@playwright/test`, and ask for what they need:

```ts
test('a deposit increases the balance', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('100.00')   // arrange (API)
  await account.open(id)
  await account.deposit('25.50')                          // act (UI)
  await expect(account.balance).toHaveText('£125.50')     // assert (UI)
})
```

A fixture is only built for a test that asks for it, and every test gets **its own account**, so no test can
affect another and you never need to reset the database.

### Auto-waiting across a slow server

Moving money takes about 750 ms on purpose (it simulates a slow downstream system). The Angular app shows
"Processing…" and disables the button meanwhile, then updates the page when the answer arrives. You don't need
waits: `await expect(account.balance).toHaveText(...)` retries until the new balance appears.

Because the Angular page updates *in place* while the request runs, you can also assert the in-between state:
`await expect(page.getByRole('button', { name: 'Processing…' })).toBeDisabled()`. Playwright checks the page often enough to see it.

### Belt and braces

After acting in the UI, check the result through the API too (`await api.balanceOf(id)`). This catches a UI
that shows one thing while the system recorded another.

## Exercise

1. Complete the fixtures in `e2e-ts/angular/support/fixtures.ts`: `api` is done; add `signedIn` (signs in as the
   teller and gives the test a `DashboardPage`) and `accountPage`.
2. Complete `e2e-ts/angular/pages/account-page.ts`: `open(id)`, `status`, `flash`, `moveMoney` (the region),
   `amount`, `toAccount`, `submit`, `transactionRows`, `chooseType(label)`, `deposit(v)`, `withdraw(v)`,
   `transfer(v, toId)`, `filterTransactions(label)`, `click(name)` and `clickAndConfirm(name)`.
   Tip: `toAccount.selectOption(String(id))` selects by option *value*.
3. Complete `e2e-ts/angular/lesson09/move-money.spec.ts`, with a fresh account in every test:
   1. **Deposit** 25.50 into 100 → flash "Deposit of £25.50 completed", balance £125.50, newest row shows
      "Deposit" and "+£25.50".
   2. **Auto-wait.** Withdraw 10, then immediately expect the balance £90.00. No sleeps.
   3. **Processing.** Deposit 1, then expect a disabled "Processing…" button, then "Submit" enabled again.
   4. **Insufficient funds.** Withdraw 50.01 from 50 → the exact message as Amount's accessible description,
      `aria-invalid`, the typed value kept, the balance unchanged **in the UI and through the API**.
   5. **Validation.** Deposit `12.345` → Amount's description is "Amount can have at most 2 decimal places".
   6. **Conditional field.** "To account" is hidden → choose Transfer → visible → choose Deposit → hidden.
   7. **Transfer** 120 from 300 to another new account → flash mentions the destination account number; source
      £180.00; newest row "to <number>"; destination £120.00 (open it in the UI) and through the API.
   8. **Transfer with no destination** → "To account" is still visible and marked invalid.
   9. **Frozen account** (freeze it through the API) → badge "Frozen", no "Move money" section.
   10. **Freeze → unfreeze → close** through the buttons (closing needs a confirm dialog).
   11. **Filter history.** Deposit + withdrawal (API) → 2 rows → filter "Withdrawal" → 1 row.

## Discussion

- Compare this file's run time with lesson 06. Where did the time go in lesson 06?
- Which of these tests would you also want at the API level only? Which only make sense in the UI?
- In test 9, why wait for the "Frozen" badge before checking that "Move money" is hidden?

## Checkpoint

- [ ] 11 passing tests that never touch seeded data
- [ ] No `waitForTimeout`

**Reference:** `lesson09/move-money.spec.ts`, `pages/account-page.ts`, `support/fixtures.ts`

Next: [10: Authentication state and roles](10-auth-state.md)
