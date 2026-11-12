# Lesson 09: Hybrid tests (arrange with the API, act and assert in the UI)

**Goal:** test the "Move money" features quickly and reliably. Create exactly the data each test
needs through the API, then exercise the UI.

**Time:** about 2 hours

**Docs:** [API testing: preparing server state](https://playwright.dev/java/docs/api-testing) · [Auto-waiting](https://playwright.dev/java/docs/actionability) · [Navigations](https://playwright.dev/java/docs/navigations)

## Concepts

### Arrange, act, assert

```java
@BeforeEach
void setUp(Playwright playwright, Page page) {
    api = LedgerApi.loginAs(playwright, User.TELLER);   // fast data setup
    new LoginPage(page).loginAs(User.TELLER);           // browser session
    account = new AccountPage(page);
}

@Test
void depositIncreasesTheBalance() {
    long id = api.accountWithBalance("100.00").get("id").getAsLong();   // arrange (API)
    account.open(id);
    account.deposit("25.50");                                           // act (UI)
    assertThat(account.balance).hasText("£125.50");                     // assert (UI)
}
```

Every test gets **its own account**. No test can affect another, and you never need to reset the
database.

### Auto-waiting across slow server calls

Moving money takes about 750 ms on purpose (it simulates a slow downstream system). You don't need
waits:

- `click()` on a submit button **waits for the navigation it starts**.
- `assertThat(...).hasText(...)` retries for up to 5 seconds.
- Locators wait for an in-progress navigation to finish before they look at the page.

That last point has a consequence. The "Processing…" button only exists *during* the navigation, so
Playwright deliberately won't show it to you. That's usually what you want. Lesson 11 shows how to
observe in-flight states for `fetch()` requests.

### Belt and braces

After acting in the UI, you can verify the result through the API too (`api.balanceOf(id)`). This
catches cases where the UI shows one thing but the system recorded another.

## Exercise

1. Complete the starter `workshop/pages/AccountPage.java`: `open(id)`, `balance`, `amount`, `toAccount`, `submit`,
   `chooseType(label)`, `deposit(v)`, `withdraw(v)`, `transfer(v, toId)`, `flash()`, `status()`,
   `transactionRows()`, `moveMoneySection()`, `filterTransactions(label)`,
   `click(name)` and `clickAndConfirm(name)`.
   Tip: `toAccount.selectOption(String.valueOf(id))` selects by option *value*.
2. Complete `workshop/lesson09/MoveMoneyTest.java` (test names only), with a fresh account in every test:
   1. **Deposit** 25.50 into 100 → flash "Deposit of £25.50 completed", balance £125.50, newest
      row shows "Deposit" and "+£25.50".
   2. **Auto-wait.** Withdraw 10, then immediately assert the balance £90.00. No sleeps.
   3. **Insufficient funds.** Withdraw 50.01 from 50 → exact message as Amount's accessible description, `aria-invalid`,
      typed value kept, balance unchanged **in the UI and through the API**.
   4. **Validation.** Deposit `12.345` → Amount's description is "Amount can have at most 2 decimal places".
   5. **Conditional field.** "To account" is hidden → choose Transfer → visible → choose Deposit → hidden.
   6. **Transfer** 120 from 300 to another new account → flash mentions the destination account number;
      source £180.00; newest row "to <number>"; destination £120.00 (open it in the UI) and through the API.
   7. **Transfer with no destination** → "To account" is still visible and marked invalid.
   8. **Frozen account** (freeze it through the API) → badge "Frozen", no "Move money" section.
   9. **Freeze → unfreeze → close** through the buttons (closing needs a confirm dialog).
   10. **Filter history.** Deposit + withdrawal (API) → 2 rows → filter "Withdrawal" → 1 row.

## Discussion

- Compare the run time of this class with lesson 06. Where did the time go in lesson 06?
- Which of these tests would you also want at the API level only? Which only make sense in the UI?

## Checkpoint

- [ ] 10 passing tests that never touch seeded data
- [ ] No `Thread.sleep`, no `waitForTimeout`

**Reference:** `solutions/lesson09/MoveMoneyTest.java`, `solutions/pages/AccountPage.java`

Next: [10: Authentication state and roles](10-auth-state.md)
