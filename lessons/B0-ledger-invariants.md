# Mission B0: Ledger invariants

**Goal:** write one reusable check that proves the books balance after any test, then prove the check
works by switching on bugs planted in the app. A screen can show the right balance while the system has
recorded something else; a ledger's rules hold no matter what happened (money is only ever moved, never
created or lost), so checking them catches whole classes of bugs that no single assertion was written for.
It's the first mission of the banking track; every later mission ends with this check.

**Time:** about 3 hours

**Docs:** [API testing](https://playwright.dev/java/docs/api-testing) · [JUnit `assertAll`](https://docs.junit.org/5.14.0/user-guide/#writing-tests-assertions) · [Parameterized tests](https://docs.junit.org/5.14.0/user-guide/#writing-tests-parameterized-tests)

Background and sources: [`docs/research/b0-ledger-invariants.md`](../docs/research/b0-ledger-invariants.md).

This is lesson 13 turned around. There, the app was fine and the **tests** had one fault each. Here, the
tests must be right, and the **app** gets one fault at a time.

## Concepts

### The invariants

Each account has a `balance` (`GET /api/accounts/{id}`) and a list of entries
(`GET /api/accounts/{id}/transactions`, newest first). Deposits and transfers in add; withdrawals and
transfers out subtract.

| Invariant | The rule |
|---|---|
| `BALANCE_MATCHES_ENTRIES` | `balance` = the signed sum of the account's entries |
| `RUNNING_BALANCE` | replaying the entries **oldest first by `id`**, each `balanceAfter` = the running total, starting at 0.00 |
| `TRANSFER_PAIRING` | each `transferId` has exactly one `TRANSFER_OUT` and one `TRANSFER_IN`, for the same amount, each naming the other's account |
| `NO_NEGATIVE_BALANCE` | no `balance` or `balanceAfter` is below zero |
| `CLOSED_IS_EMPTY` | a `CLOSED` account holds 0.00 (a `FROZEN` one may hold money) |
| `AMOUNTS_IN_PENCE` | every amount, balance and `balanceAfter` has exactly 2 decimal places |

> **Order by `id`, not time.** `createdAt` comes from the server clock, which any test can pin with
> `PUT /api/testing/clock`, and both legs of a transfer share one timestamp.

> **Money is `BigDecimal`.** Compare values with `compareTo` and check `scale()` separately, so a wrong
> value and a wrong format fail with different messages (remember lesson 13, Case 02).

### One check, every failure

```java
public void assertBalanced(long... accountIds) {
    List<Violation> violations = check(accountIds);
    assertAll("ledger invariants", violations.stream().map(v -> (Executable) () -> fail(v.toString())));
}
```

`assertAll` runs every executable and reports **all** failures together, so when one rule breaks you
also see that the others held. `check(...)` returns the violations as data, which is what the
fault-proof tests assert on.

Read with an **auditor** client: it can't write (`403`), so the checker can never change the ledger it is
checking. Only check accounts **your test created**; other tests are moving money in parallel.

### Planted faults

With the dev profile (`./mvnw -pl app spring-boot:run`), the app can be told to misbehave for **one account**:

```java
teller.plantFault("transfer-in-short", toAccountId);   // PUT /api/testing/faults/{name} {"accountId": ...}
try {
    teller.transfer(from, to, "10.00");
} finally {
    teller.clearFault("transfer-in-short", toAccountId); // DELETE /api/testing/faults/{name}?accountId=...
}
```

| Fault | Arm it on | What the app does | Should break |
|---|---|---|---|
| `stale-balance-after` | the account | writes the entry before changing the balance | `RUNNING_BALANCE` |
| `skip-withdrawal-entry` | the account | withdraws without recording an entry (the response's `id` is `null`: a clue) | `BALANCE_MATCHES_ENTRIES` |
| `transfer-in-short` | the destination | the transfer-in leg is a penny short | `TRANSFER_PAIRING` |
| `unpaired-transfer` | the destination | the transfer-in leg gets its own transfer id | `TRANSFER_PAIRING` |
| `overdraft` | the source | skips the funds check | `NO_NEGATIVE_BALANCE` |

`transfer-in-short` is the interesting one: both accounts on their own look perfectly consistent. Only a
check that looks **across** accounts catches it. `POST /api/testing/reset` clears all faults.

## Exercise

There are no starter tests: you've done enough lessons to set up a test class yourself.

1. **The check.** Implement `workshop/support/LedgerInvariants.java`. The enum, the record and the method
   signatures are fixed (later missions call them). Add `transfer`, `allAccounts`, `testSupportEnabled`,
   `plantFault` and `clearFault` to your `LedgerApi`. For transfer pairing, the other leg can be on an account
   you weren't asked about: map `counterpartyAccountNumber` to an id with `allAccounts()` and read its entries too.
2. **It passes on good data.** Create `workshop/b0/LedgerInvariantsTest.java` with fresh accounts in every test:
   1. `anAccountWithNoHistoryIsBalanced`
   2. `depositsWithdrawalsAndTransfersStayBalanced`: withdraw, transfer and deposit through the API, then check both accounts
   3. `aTransferMadeInTheUiIsBalanced`: act through your `AccountPage`, verify with the check
3. **It catches bad data.** `eachPlantedFaultIsCaughtAndNamed`: a `@ParameterizedTest` over the five faults.
   Arm the fault, make the one movement it corrupts, disarm it in `finally`, then assert that `check(...)`
   reports **exactly** the expected invariant and nothing else, and that `assertBalanced` throws.
   Skip (with `assumeTrue`) when `testSupportEnabled()` is false, with a message saying how to start the app.
4. **Use it.** Add `invariants.assertBalanced(...)` to the end of one of your lesson 08 API tests and one of
   your lesson 09 UI tests.

## Discussion

- Why an explicit call at the end of a test rather than a JUnit extension that runs after every test?
  (Hint: what does `@AfterEach` close, and how would an extension get a logged-in client?)
- **Stretch: the whole bank (I9).** Across *all* accounts, the sum of balances = all deposits − all
  withdrawals (transfers net to zero). Write it in its own class marked `@Isolated`, so JUnit runs it while
  nothing else moves money. It still can't live in the reference suite: once `transfer-in-short` has run,
  a penny has left the books for good, and only restarting the app brings it back. What does that tell you
  about checks that span data your test didn't create?
- `skip-withdrawal-entry` returns `201` with `"id": null`. Should a test have caught that from the
  response alone? What does the invariant check give you that the response check doesn't?

## Checkpoint

- [ ] All five faults caught, each naming only the invariant it breaks
- [ ] Green three times in a row in parallel (`-Psolutions` style) without restarting the app
- [ ] No seeded data modified; every test made its own accounts
- [ ] The check uses the auditor login

**Reference:** `solutions/support/LedgerInvariants.java`, `solutions/b0/LedgerInvariantsTest.java`

Next: the banking track continues with the missions in [`docs/research/banking-upskill.md`](../docs/research/banking-upskill.md).
