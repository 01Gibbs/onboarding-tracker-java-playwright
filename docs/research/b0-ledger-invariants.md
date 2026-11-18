# Mission B0: ledger invariants (design research)

- **Date researched:** 2026-11-12
- **Question:** what can a reusable "ledger invariant" check in `e2e-tests/` actually assert about Ledgerline,
  how should learners attach it to their tests, and how should a planted fault prove that it works?
- **Builds on:** [`banking-upskill.md`](banking-upskill.md) §3 R1 and §4 (B0 is the base the other missions
  build on), [`playwright-java.md`](playwright-java.md) §2 and §6, and
  [`../ledgerline-reference.md`](../ledgerline-reference.md). This note doesn't repeat them.
- **How the app claims were checked.** By reading the source in this repo (cited as `file:line`, with paths
  relative to `app/src/main/java/dev/upskill/ledger/` unless they say otherwise). A local copy of the app was
  also started on port 8093 (dev profile, fresh seed) and probed with a short Python script over the JSON API.
  Those results are marked **[Probed]**. **[Inference]** marks my own judgement.
- **How the library claims were checked.** Against the `microsoft/playwright-java` source at tag `v1.64.0`
  (fetched through the GitHub API), the JUnit 5.14.0 User Guide and Javadoc, and the opentest4j and Gson
  source on GitHub.

---

## 1. TL;DR

1. **Ledgerline guarantees every invariant B0 wants, as long as the money moves through `MoneyService`.**
   `MoneyService` is the only code that changes a balance or writes a ledger entry. Each movement updates the
   balance and writes its entry in the same transaction. The seed data is created the same way, so it
   satisfies every invariant. The probe on a fresh seed found all 6 accounts balanced, the chains intact, one
   correctly paired transfer, and a system total of 7,370.50 = deposits − withdrawals. **[Probed]**
2. **All of it can be seen through the existing JSON API, and B0 needs no new read endpoint.** Two gaps
   matter. There's no system-wide entry listing (`/api/activity` is clamped to 50), and every read is a
   separate request, so a check across the whole ledger is never an atomic snapshot. Neither gap affects a
   check that is **scoped to the accounts the test owns**, which is what B0 should teach.
3. **The one app change B0 needs is a fault switch.** It should be **scoped to one account**
   (`PUT /api/testing/faults/{name}` with `{"accountId": …}`) rather than global. If a fault were global, it
   would corrupt every test running in parallel, and it would contradict lesson 13's premise that "the app is
   fine". Ship five faults, each breaking exactly one invariant, and have `/api/testing/reset` clear them.
4. **Attach the check with an explicit call at the end of each test**, for example
   `invariants.assertBalanced(from, to)`, aggregated with `assertAll`. Mention a JUnit extension only in the
   lesson's Discussion section. An `AfterEachCallback` runs **after** the test's `@AfterEach` (which is where
   lessons 08 and 09 close `LedgerApi`). The only way an extension can reach `@UsePlaywright`'s `Playwright`
   is through the `impl.junit` package, which is marked "subject to change".
5. **Keep the system-wide total out of the parallel reference suite.** If it's taught at all, teach it as a
   stretch goal in an `@Isolated` class. A fault that changes the total can't be undone, because the ledger
   is append-only, so the total stays wrong until the app restarts or is reset.
6. **Two findings for the wider track.**
   - Concurrent writes to one account return **500 with Spring's default error body**, not `ApiError`.
     The invariants still hold afterwards. **[Probed]**
   - The `1E+2` amount bug in `banking-upskill.md` has since been fixed, so B0's proposed
     "canonical-amount fix + `exponent-amounts` fault" is out of date.

---

## 2. Invariants

Signs come from `EntryType.credit`: `DEPOSIT` and `TRANSFER_IN` are **+**, and `WITHDRAWAL` and
`TRANSFER_OUT` are **−** (`ledger/EntryType.java:4-7`). `LedgerEntry.getSignedAmount()` applies this sign
server-side (`ledger/LedgerEntry.java:68-71`), but the DTO doesn't expose it, so the test applies the sign
itself (`api/ApiDtos.java:38-47`).

| # | Invariant | Guaranteed? | Evidence | Observable via API? |
|---|---|---|---|---|
| I1 | `balance == Σ signed(entry.amount)` per account | **Yes** | The only `credit`/`debit` callers are in `MoneyService`, each followed by `entries.save(...)` in the same `@Transactional` method (`ledger/MoneyService.java:18-19,48-49,58-59,80-84`; checked by grepping for `.credit(`, `.debit(` and `new LedgerEntry(`). A new account starts at `0.00` (`account/Account.java:44-45`), and there's no setter for `balance`. **[Probed]** holds for all seed accounts | Yes: `GET /api/accounts/{id}` (`balance`) plus `GET /api/accounts/{id}/transactions` (all entries, no paging: `api/AccountApi.java:77-80`, `ledger/LedgerEntryRepository.java:13-18`) |
| I2 | `balanceAfter` chain: sort entries by **`id` ascending**, start the running total at `0.00`, add each signed amount; each `balanceAfter` equals the running total, and the last equals `balance` | **Yes** | `balanceAfter` is copied from `account.getBalance()` in the constructor (`ledger/LedgerEntry.java:61`), and every caller credits or debits **before** constructing the entry (`ledger/MoneyService.java:48-49,58-59,80-84`). A concurrent writer to the same account fails the `@Version` check and rolls back (`account/Account.java:54-55`), so chains don't interleave. **[Probed]**: 120 concurrent movements on 2 accounts left both chains intact | Yes. The API returns entries **newest first by id** (`order by e.id desc`, `ledger/LedgerEntryRepository.java:16`), so the test must reverse or sort them. **Use `id`, not `createdAt`.** `createdAt` comes from `LedgerClock`, which any test can pin app-wide with `PUT /api/testing/clock` (`common/LedgerClock.java:19-31`). Both transfer legs share one `Instant` (`ledger/MoneyService.java:81-84`), and seed entries are microseconds apart |
| I3 | Transfer pairing: each `transferId` has **exactly 2** entries, one `TRANSFER_OUT` and one `TRANSFER_IN`, with equal `amount`, swapped `counterpartyAccountNumber` (each leg names the other leg's account), equal `createdAt`, and equal `description` | **Yes** | `ledger/MoneyService.java:78-84`: one UUID, one `value`, one `now`, one `text`. The OUT leg names `to`, and the IN leg names `from` | Yes, but only per account. There is no lookup by `transferId`. To find the other leg, map `counterpartyAccountNumber` → id through `GET /api/accounts` (it returns `accountNumber` and includes closed accounts when no filter is given: `account/AccountRepository.java:10-19`), then read that account's transactions |
| I4 | Transfer legs have **consecutive ids** | **No** | Both legs are inserted in one transaction, but IDENTITY values are not reserved per transaction. **[Inference]** Don't assert this | — |
| I5 | No negative balance | **Yes** (serially and under concurrency) | `requireFunds` runs before every debit (`ledger/MoneyService.java:57,76,95-100`). Optimistic locking rejects a stale concurrent debit (`account/Account.java:54-55`). **[Probed]**: no negative balance after a concurrent burst of withdrawals and crossing transfers | Yes: `balance >= 0` from `GET /api/accounts/{id}`, and every `balanceAfter >= 0` |
| I6 | `CLOSED` ⇒ `balance == 0.00` | **Yes** | Closing requires a zero balance (`account/AccountService.java:78-81`); a closed account can't change status again (`:75-77`), and money can't move on a non-`ACTIVE` account (`ledger/MoneyService.java:47,56,74-75,88-93`). Seed: `Old Account` is withdrawn to 0 and then closed (`seed/DataSeeder.java:85-89`) | Yes: `status` and `balance` on `GET /api/accounts/{id}` |
| I7 | No entries after an account was frozen or closed | **Yes** (by `requireActive`) | `ledger/MoneyService.java:88-93` | **No.** There is no status-change timestamp or history, so a test can only compare its own before and after snapshots |
| I8 | Amounts have **scale 2**, are `> 0` and `≤ 1,000,000.00`; balances have scale 2 | **Yes** | `Amount.of` rejects `≤ 0`, `> 1,000,000.00` and more than 2 decimal places, then calls `setScale(2)` (`ledger/Amount.java:12-24`). The columns are `precision 15, scale 2` (`ledger/LedgerEntry.java:35-39`, `account/Account.java:44-45`). `1E+2` is normalised to `100.00` (`app/src/test/.../ledger/AmountTest.java:16-18`) and echoed as `"amount":100.00` (`app/src/test/.../api/ApiContractTest.java:99-105`). There is **no maximum on a balance**, only on each movement | Yes. **[Probed]**: every `balance`, `amount` and `balanceAfter` arrives as a JSON number with exactly 2 decimals (`1250.00`, `0.00`). Gson keeps the scale: `JsonPrimitive.getAsBigDecimal()` parses the original string ([Gson `JsonPrimitive.java` at 2.14.0, lines 182-186](https://github.com/google/gson/blob/gson-parent-2.14.0/gson/src/main/java/com/google/gson/JsonPrimitive.java)). Check scale with `.scale() == 2` and compare values with `compareTo`, so a scale fault and a value fault fail with different messages (lesson 13 Case 02's lesson) |
| I9 | System-wide: `Σ balance(all accounts) == Σ DEPOSIT − Σ WITHDRAWAL`, which (given I1) is the same as `Σ TRANSFER_IN == Σ TRANSFER_OUT`. Transfers net to zero | **Yes** at every committed state | Follows from I1 and I3. There's no external, suspense or fee account. An account can only be deleted if it has no history (`account/AccountService.java:86-94`), and a customer only if they have no accounts (`customer/CustomerService.java:62-69`), so no entries are orphaned. **[Probed]**: 7,370.50 on a fresh seed, and 9,342.50 = 9,342.50 after the concurrent burst | **Only with N+1 requests, not as a snapshot.** List accounts, then fetch each account's transactions. `/api/activity` can't help: it's clamped to 1–50 (`ledger/MoneyService.java:39-42`, `app/src/test/.../api/ApiContractTest.java:89`). Meanwhile, parallel tests are committing transfers (see §4.4) |

**Invariants the seed data violates:** none. **[Probed]** on a fresh seed: 6 accounts, all with I1/I2/I5/I6/I8
holding, the one transfer (`Monthly saving`, 1,000.00, 10000001 → 10000002) correctly paired, and I9 at
7,370.50. This matches the hand calculation from `seed/DataSeeder.java:77-86`. Three things to remember:

- `Bills` (10000005) has **no entries**, so the check must handle an empty history: balance `0.00`, empty
  chain.
- `Holiday Fund` is `FROZEN` with 800.00. Frozen accounts may hold money; only `CLOSED` requires zero.
- `POST /api/testing/reset` truncates the tables and **restarts the identity columns**
  (`seed/DataSeeder.java:56-62`). An id a test is holding can then point at a different account.

**What the app does *not* guarantee that a learner might assume:**

- consecutive ids for transfer legs (I4)
- `createdAt` increasing with `id` (I2 note)
- an observable status history (I7)
- an error body in the `ApiError` shape under concurrency. **[Probed]** 83 of 120 concurrent writes to 2
  accounts returned **500** with Spring's default body (`ObjectOptimisticLockingFailureException … where
  id=? and version=?`), because `ApiExceptionHandler` has no handler for it (`api/ApiExceptionHandler.java:20-50`).
  The invariants still held, because the failed transactions rolled back. This contradicts
  `banking-upskill.md`'s concurrency probe (4×422, no lock errors), probably because of the different
  workload; see §8. It belongs in B2, not B0.

---

## 3. API gaps and minimal app changes

| Gap | Effect on B0 | Minimal change | Recommendation |
|---|---|---|---|
| G1. No system-wide entry listing (`/api/activity` max 50) | I9 needs `GET /api/accounts` plus one request per account | `GET /api/activity?limit=` without the clamp, or a paged `/api/entries` | **Don't change it.** Per-account checks don't need it, and the N+1 version is a fine stretch exercise |
| G2. Reads aren't atomic across requests (balance and entries are separate calls; every account is a separate call) | Harmless for accounts the test owns. False failures for I9 while other tests run | A read-only "trial balance" endpoint, e.g. `GET /api/ledger/summary` → `{accountCount, totalBalance, totalDeposits, totalWithdrawals, totalTransfersIn, totalTransfersOut}`, computed in one `@Transactional(readOnly = true)` | **Defer to B2.** It's an atomic snapshot, but the app would then be grading itself. B0's point is that the test cross-checks two independently stored facts (`accounts.balance` vs `ledger_entries`) |
| G3. No lookup by `transferId` | The second leg must be found through `counterpartyAccountNumber` → id | `GET /api/transfers/{transferId}` returning both legs | **Don't change it.** Building the number→id map is a good small exercise, and `GET /api/accounts` already gives it |
| G4. No status timestamps (I7 unobservable) | Can't assert "nothing after close" except from the test's own snapshots | `statusChangedAt` on `AccountDto` | **Don't change it.** Mention it in the Discussion section |
| G5. Concurrency failures are 500s in Spring's error shape, not `ApiError` | None for B0's GETs | Map `ObjectOptimisticLockingFailureException` → 409 `ApiError` in `ApiExceptionHandler` | **B2 change**, flagged here |
| G6. Nothing to make the app break an invariant | Learners can't prove their check catches anything | Fault switch (§5) | **Required for B0** |

`LedgerApi` (e2e side) needs one small addition. Neither the workshop nor the solutions `LedgerApi` has
`transfer(...)`. Lesson 08's reference posts `/api/transfers` by hand
(`e2e-tests/src/solutions/java/dev/upskill/e2e/solutions/lesson08/AccountApiTest.java:82`). B0 should add
`transfer(long from, long to, String amount)` and `allAccounts()`. The broken suite compiles against
`src/solutions` (`e2e-tests/pom.xml:155-158`), so only **add** methods: don't change existing signatures.

---

## 4. Attaching the check

### 4.1 Shape of the check (whichever way it's attached)

```java
public final class LedgerInvariants {
    public LedgerInvariants(LedgerApi api) { … }
    /** Checks I1, I2, I3, I5, I6, I8 for these accounts and the other leg of each of their transfers. */
    public void assertBalanced(long... accountIds) { … }
}
```

Use `assertAll(heading, executables)`: one heading per account (`"account 10000123"`), with one executable
per invariant. The JUnit Javadoc says that if any executable throws, "all remaining executables will still
be executed, and all exceptions will be aggregated and reported in a `MultipleFailuresError`". It also says
that each one is "added as suppressed exceptions", and that the heading is included in the message
([`Assertions.assertAll(String, Stream)`](https://docs.junit.org/5.14.0/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html)).
The User Guide (§2.5) shows nesting: "Within a code block, if an assertion fails the subsequent code in the
same block will be skipped" ([User Guide 5.14.0](https://docs.junit.org/5.14.0/user-guide/)).

This lets the outer block fetch the data with a fail-fast `assertThat(response).isOK()`, and the inner
`assertAll` report **every** broken invariant at once. That matters when a fault breaks one invariant and a
learner wants to see that the others still hold. `MultipleFailuresError extends AssertionError`
([opentest4j source](https://github.com/ota4j-team/opentest4j/blob/main/src/main/java/org/opentest4j/MultipleFailuresError.java),
line 33), so a fault-proof test can use `assertThrows(AssertionError.class, …)` and inspect `getFailures()`.

Use a **read-only auditor client** for the check (`User.AUDITOR`). Writes from that role get 403
(`auth/ApiAuthInterceptor.java:30-32`), so the checker can't change the ledger it's checking. The framing
"an auditor checks the books" fits. **[Inference]**

### 4.2 Options

| Option | How | Pros | Cons |
|---|---|---|---|
| **A. Explicit call** (recommended) | Last line of each money test: `invariants.assertBalanced(from, to);` | Visible in the test and in the trace. Uses the test's own client and the accounts it actually touched. Fits lesson 09's "belt and braces" (`lessons/09-hybrid-tests.md:47-50`). No new JUnit concepts | Learners can forget it. Code review (lesson 14's definition of done) has to catch that |
| B. `@AfterEach` in a base class or interface default method | Tests register touched ids; the base `@AfterEach` checks them | One place | JUnit runs superclass and interface `@AfterEach` **after** the subclass's (User Guide §5.18.2). The lesson 08/09 pattern of `api.close()` in the test's own `@AfterEach` would close the client first. Inheritance also hides behaviour |
| C. Extension (`AfterEachCallback`) | `@ExtendWith(LedgerInvariantsExtension.class)`; the test class implements e.g. `LedgerAware { LedgerApi ledger(); Set<Long> touched(); }`, read through `ExtensionContext.getRequiredTestInstance()` | Can't be forgotten once registered. JUnit fails the test if an extension throws (User Guide §2.7.1) | Ordering trap: in the User Guide §5.18.1 table, `@AfterEach` runs **before** `AfterEachCallback`, so the test's client is already closed. Work-arounds are to use `AfterTestExecutionCallback`, which runs before `@AfterEach` (§5.10.1), or to close the client with `@AutoClose` (closed via `TestInstancePreDestroyCallback`, §2.23.2). Each is one more concept. See §4.3 for getting a `Playwright` |

**Recommendation: A for B0, with C as the Discussion topic.** B0 is the first mission. The new ideas should
be the invariants and the fault proof, not JUnit's callback ordering. Once B1–B9 make "every money test ends
with the check" a rule, C is a natural refactor. Teach it with `AfterTestExecutionCallback` and the
`LedgerAware` interface, and let the test own the client. **[Inference]**

### 4.3 Can an extension get `@UsePlaywright`'s objects?

- `@UsePlaywright` is a meta-annotation for seven extensions:
  `@ExtendWith({OptionsExtension, PlaywrightExtension, BrowserExtension, BrowserContextExtension, PageExtension, APIRequestContextExtension, BrowserContextOptionsExtension})`
  ([`UsePlaywright.java:87-88` at v1.64.0](https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/junit/UsePlaywright.java)).
  Parameters resolve only if the outermost test class carries `@UsePlaywright`
  ([`ExtensionUtils.java:29-49`](https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/impl/junit/ExtensionUtils.java)).
- `Playwright` is held in a `ThreadLocal`, created on first use, registered in a root-store registry, and
  closed at the end of the run. A **public static** `PlaywrightExtension.getOrCreatePlaywright(ExtensionContext)`
  exists, but its Javadoc says "this method is subject to change"
  ([`PlaywrightExtension.java:33,87-108`](https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/impl/junit/PlaywrightExtension.java)).
  It sits in `com.microsoft.playwright.impl.junit`, not in the public `junit` package (`playwright-java.md` §2).
- `APIRequestContext` works the same way: a per-thread `ThreadLocal`, cleared in `beforeEach`, with a public
  static `getOrCreateAPIRequestContext(ExtensionContext)`, also "subject to change"
  ([`APIRequestContextExtension.java:28-68`](https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/impl/junit/APIRequestContextExtension.java)).
  Two catches:
  - It's built only from `Options.apiRequestOptions`, which `LedgerlineOptions` sets to just the base URL
    (`e2e-tests/src/solutions/.../support/LedgerlineOptions.java:22`). It has no bearer token, so every
    Ledgerline API call through it gets 401 (`auth/ApiAuthInterceptor.java:26-29`).
  - The extension never calls `dispose()` on it; it only drops the reference (lines 32-39).
- **Conclusion:** an extension *can* reach the same `Playwright` (and from it, `LedgerApi.loginAs(playwright,
  User.AUDITOR)`), but only through an internal API that is marked unstable. Don't build course material on
  it. Use `LedgerAware` (option C) to get the client from the test instance instead.

### 4.4 Parallel runs and the system-wide total (I9)

- The reference suite runs **classes concurrently and methods within a class sequentially**, with fixed
  parallelism 4 (`e2e-tests/pom.xml:121-133`). Lesson 12 teaches the same mode with the `dynamic` strategy
  (`lessons/12-debugging-and-scale.md:58-70`).
- **Per-account checks (I1–I8) are parallel-safe with no locks**, because each test checks only accounts it
  created. This is the "own your data" rule from lesson 06 onwards (`lessons/12-debugging-and-scale.md:72-74`;
  `docs/ledgerline-reference.md:31-32`).
- **I9 is not.** Each transfer commits atomically, but the check reads accounts one request at a time.
  Suppose account A is read, then a parallel test transfers B → A, then B is read. The OUT leg is counted
  and the IN leg is not, so the check reports a false failure. **[Inference]** (this follows from G2)
- **JUnit options**, from the [User Guide §2.22.2](https://docs.junit.org/5.14.0/user-guide/) and the
  [`@Isolated` Javadoc](https://docs.junit.org/5.14.0/api/org.junit.jupiter.api/org/junit/jupiter/api/parallel/Isolated.html):
  - `@ResourceLock("ledger", mode = …)`: "no conflicting tests are run in parallel". Two `READ` holders may
    overlap, but neither may run while a `READ_WRITE` holder runs. The lock is "acquired before any
    `@BeforeEach` methods are executed and released after all `@AfterEach` methods". To protect I9, **every**
    money-moving class must declare `READ`, and the I9 check `READ_WRITE`. The names are the opposite of what
    they mean here (movers "read", the checker "writes"). A single class without the annotation silently
    breaks the guarantee.
  - `@Isolated` (since 5.7): "no other test class is executed concurrently". One annotation on the I9 class,
    and nothing on anyone else.
- **Recommendation:** put the I9 check (if taught) in its own `@Isolated` class, as a **stretch goal**.
  Explain that `@Isolated` only isolates from **other test classes in the same JVM**. It doesn't protect
  against a second Maven run, a learner clicking in the UI, or a shared app instance. Also, I9 stays broken
  after any fault that changes the total (§5) until the app restarts. `POST /api/testing/reset` would fix
  it, but it wipes other tests' data mid-run (`lessons/12-debugging-and-scale.md:73-74`). Keep I9 out of the
  default `-Psolutions` run, or run it alone with `-Dtest=…`. **[Inference]**

---

## 5. Fault switch design

### 5.1 Contract

| Method | Path | Body | Result |
|---|---|---|---|
| PUT | `/api/testing/faults/{name}` | `{"accountId": 123}` | `200 {"name","accountId"}`. Unknown name → `404` (`ApiError`); unknown account → `404` |
| DELETE | `/api/testing/faults/{name}?accountId=123` | none | `204` |
| DELETE | `/api/testing/faults` | none | `204`, clears all faults |
| GET | `/api/testing/faults` | none | `200 [{"name","accountId"}]` |

- **Gating.** The faults go in `TestingApi`, behind the existing `requireEnabled()` (`api/TestingApi.java:55-59`),
  so they're `404` unless `ledgerline.test-support-enabled=true`. That's off by default
  (`app/src/main/resources/application.yml:27-29`) and on in the dev profile
  (`app/src/main/resources/application-dev.yml:7-8`). Like `reset`, `/api/testing/**` needs no token
  (`auth/WebConfig.java:22-23`).
- **State.** Keep a `ledger/Faults` component holding a `ConcurrentHashMap<Fault, Set<Long>>`, in the same
  style as `LedgerClock`'s `AtomicReference` (`common/LedgerClock.java:17-31`). `MoneyService` asks
  `faults.isArmed(Fault.X, account.getId())`.
- **Scope to one account, not the app.** Three reasons:
  1. Parallel safety. Only the arming test's own account misbehaves, so no `@Isolated` is needed.
  2. It keeps lesson 13 honest ("The app is fine; the tests are wrong", `lessons/13-fix-the-broken-suite.md:13`).
     Every broken-suite case creates its own accounts (e.g. `Case01DepositTest.java:21`,
     `Case05TransferTest.java:22-23`), so a leftover fault can't reach them.
  3. It matches the reference doc's rule that seeded data is read-only.
- **`/api/testing/reset` must clear faults.** Identity columns restart on reset (`seed/DataSeeder.java:59-61`),
  so a fault armed for account 7 would otherwise hit the *new* account 7.
- **Runtime switch rather than the property `banking-upskill.md` R1 proposes** (`ledgerline.faults.lost-update=true`).
  A property is global and needs a restart. The same registry can carry B2's `lost-update` fault later.
  **[Inference]**

### 5.2 Fault catalogue (each breaks exactly one invariant)

| Name | Armed on | Change in `MoneyService` | Breaks | Still holds (that's the lesson) |
|---|---|---|---|---|
| `stale-balance-after` | any account | Build the entry **before** `credit`/`debit`, so `balanceAfter` is the old balance (`ledger/LedgerEntry.java:61` reads the balance at construction) | I2 | I1, I3, I5, I9: the balance is right; only the history lies |
| `skip-withdrawal-entry` | source account | `withdraw` debits but doesn't `save` the entry (`ledger/MoneyService.java:58-60`) | I1 (and I2 too if more movements follow) | I3, I5. **Tell:** the 201 response has `"id": null`, because the entry isn't persisted. Either accept that as a clue, or have the fault save and then delete the entry |
| `transfer-in-short` | destination account | IN leg records **and** credits `value − 0.01` (`ledger/MoneyService.java:83-84`) | I3 (amounts differ) and I9 | I1, I2 on **both** accounts. This is the key demo: per-account checks pass, and only pairing catches it |
| `unpaired-transfer` | destination account | IN leg gets a fresh UUID instead of `transferId` (`ledger/MoneyService.java:84`) | I3 (each id has 1 leg) | I1, I2, I9 |
| `overdraft` | source account | Skip `requireFunds` (`ledger/MoneyService.java:57,76`) | I5 | I1, I2, I3, I9: the books are consistent but wrong. Good for the point that invariants aren't business rules |

Faults left out on purpose:

- **Scale.** Values are normalised by `Amount.of` and stored as `NUMERIC(15,2)`, so a scale fault could only
  be a *serialisation* fault in `ApiDtos` (e.g. `stripTrailingZeros()`). That doesn't fit "`MoneyService`
  breaks one invariant", so it's optional at most.
- **`close-with-balance`.** This lives in `AccountService` (`account/AccountService.java:78-81`), not
  `MoneyService`. Add it later if needed.

**App-side tests.** Add one `MoneyServiceTest` per fault asserting exactly which invariant breaks, in the
style of `app/src/test/.../MoneyServiceTest.java:46-82`. Extend
`ApiContractTest.testSupportIsOffUnlessTheDevProfileTurnsItOn` (`:108`) to cover `/api/testing/faults`.

### 5.3 The learner's fault-proof test

The reference would be a `@ParameterizedTest` over the five names: arm → move money → disarm in `finally`
→ `assertThrows(AssertionError.class, () -> invariants.assertBalanced(...))`. Then assert that the failure
names the expected invariant. This shows the check catches the fault and points at the right cause.

This mirrors lesson 13 in reverse, and the lesson should say so. Lesson 13 plants faults in **tests** against
a correct app; B0 plants faults in the **app** against a test that must notice. Lesson 13's rule that each
case has "exactly one realistic fault" carries over as "each fault breaks exactly one invariant".

---

## 6. Lesson template and README impact

**Template that existing lessons follow** (07, 08, 09, 13, 14):

1. `# Lesson NN: Title`, then the `**Goal:**`, `**Time:**` and `**Docs:**` lines. The Docs line is
   playwright.dev/java links joined with ` · ` (`lessons/08-api-testing.md:1-8`, `lessons/09-hybrid-tests.md:1-8`).
2. `## Concepts` with short, compilable Java snippets, plus `> **Docs gotcha:**` call-outs where the docs
   are wrong (`lessons/07-page-objects.md:47`).
3. `## Exercise`, numbered, naming the starter file and the **test names** to implement
   (`lessons/09-hybrid-tests.md:52-72`).
4. `## Discussion` with 2–3 questions.
5. `## Checkpoint` with `- [ ]` items. Lesson 14 calls this "Definition of done"; no lesson uses the phrase
   "Done when" (`lessons/14-capstone.md:46-52`).
6. `**Reference:** solutions/...` and `Next: [..](..)` (`lessons/09-hybrid-tests.md:84-86`).

**Starters and fading:**

- Starter tests are `@UsePlaywright(LedgerlineOptions.class)` classes whose methods call
  `abort("TODO lesson NN, exercise N")`, which JUnit reports as *skipped*
  (`e2e-tests/src/test/java/dev/upskill/e2e/workshop/lesson09/MoveMoneyTest.java:3,16`).
- Support starters throw `UnsupportedOperationException("TODO lesson 08")`
  (`e2e-tests/src/test/java/dev/upskill/e2e/workshop/support/LedgerApi.java:61-95`).
- Fading reaches zero at lesson 12: "From this lesson on there are no starter files"
  (`lessons/12-debugging-and-scale.md:133`).
- B0 comes after lesson 14, so the faded format applies: **no test starters**. One exception is worth
  making: a `workshop/support/LedgerInvariants.java` skeleton with the method signatures from §4.1, throwing
  `UnsupportedOperationException("TODO mission B0")`. B1–B9 call it, so its contract must be stable.
  **[Inference]**

**Suggested B0 lesson outline** (file `lessons/B0-ledger-invariants.md` or similar):

- **Goal** and **motivation**: OWASP ASVS 5.0 2.3.3, see §7.
- **Concepts:** the invariant table from §2, cut down to I1, I2, I3, I5, I6 and I8; `assertAll` nesting;
  the auditor client.
- **Exercise:**
  1. Write `LedgerInvariants`.
  2. Add it to two earlier tests (one API test from lesson 08, one UI test from lesson 09: "act in the UI,
     verify via the API").
  3. Prove it with the five faults.
  4. Stretch: the I9 check in an `@Isolated` class.
- **Discussion:** extension vs explicit call (§4.2 C); why I9 can't run in parallel.
- **Checkpoint:**
  - [ ] All five faults caught, each with the right invariant named
  - [ ] Green 3× in parallel with faults disarmed
  - [ ] No seeded data modified

**README impact** (`README.md:12-16,50-66,72-83,102-106`):

| Claim | Today | After B0 |
|---|---|---|
| Default suite | 2 pass, 62 skipped (62 = the sum of `abort("TODO` calls across `workshop/`; checked by counting) | **Unchanged** if B0 adds no test starters, as recommended. Each starter test with an `abort` would add 1 skipped |
| `-Psolutions` | 69 tests (69 `@Test` methods in `src/solutions`; checked by counting) | 69 + M. A `@ParameterizedTest` counts once per invocation. For example, 4 invariant tests + 5 fault cases → **78** (illustrative) |
| `-Pbroken` | fails 9 of 11 on a fresh app, then 10 | **Unchanged**, as long as nothing is added to `src/broken` and `solutions/support` changes are additive (the broken profile compiles `src/solutions`, `e2e-tests/pom.xml:155-158`) and faults are account-scoped |
| "15 lessons" and the course table | 00–14 | Add a "Banking track" row or table, and update the wording |
| Layout block | lists `docs/research/playwright-java.md` | Optionally list this note |

Also update:

- `docs/ledgerline-reference.md:94-99`: add the four `/api/testing/faults` rows, and note that reset clears
  them.
- `docs/research/banking-upskill.md` §4 B0 row: its app change ("canonical-amount fix + `exponent-amounts`
  fault") is already done (`ledger/Amount.java:19-23`), so it should read "fault switch".

---

## 7. Motivation sources (optional, brief)

- **OWASP ASVS 5.0, requirement 2.3.3 (L2):** "Verify that transactions are being used at the business
  logic level such that either a business logic operation succeeds in its entirety or it is rolled back to
  the previous correct state."
  ([V2, line 52](https://github.com/OWASP/ASVS/blob/master/5.0/en/0x11-V2-Validation-and-Business-Logic.md);
  re-read for this note). The invariants are how a test *observes* whether that holds. For example, I1 and
  I3 fail if a transfer half-commits.
- I couldn't retrieve a second primary source (BCBS 239 Principle 3 on reconciling with accounting data).
  See §8.

---

## 8. Sources

**This repo:**

- `app/src/main/java/dev/upskill/ledger/`: `ledger/{Amount,EntryType,LedgerEntry,LedgerEntryRepository,MoneyService}.java`,
  `account/{Account,AccountService,AccountRepository,AccountStatus}.java`, `customer/CustomerService.java`,
  `api/{AccountApi,ApiDtos,TestingApi,ApiExceptionHandler}.java`, `auth/{ApiAuthInterceptor,WebConfig}.java`,
  `common/LedgerClock.java`, `seed/DataSeeder.java`
- `app/src/main/resources/application.yml`, `application-dev.yml`
- `app/src/test/java/dev/upskill/ledger/{MoneyServiceTest,ledger/AmountTest,api/ApiContractTest}.java`
- `e2e-tests/pom.xml`; `e2e-tests/src/{test,solutions,broken}/java/...`
- `lessons/07, 08, 09, 12, 13, 14`, `lessons/answers/13-fix-the-broken-suite.md`, `README.md`,
  `docs/ledgerline-reference.md`, `docs/research/{banking-upskill,playwright-java}.md`

**Playwright for Java (tag v1.64.0):**

- https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/junit/UsePlaywright.java
- https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/impl/junit/PlaywrightExtension.java
- https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/impl/junit/APIRequestContextExtension.java
- https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/impl/junit/ExtensionUtils.java
- https://playwright.dev/java/docs/junit (thread-safety guidance, quoted in `playwright-java.md` §2)

**JUnit 5.14:**

- User Guide: https://docs.junit.org/5.14.0/user-guide/. Sections used: §2.5 (grouped assertions),
  §2.7.1, §2.22.2 (Synchronization, `@Isolated`), §2.23.2 (`@AutoClose`), §5.10.1, §5.18.1 (User and
  Extension Code), §5.18.2 (Wrapping Behavior of Callbacks)
- `Assertions.assertAll`: https://docs.junit.org/5.14.0/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html
- `@Isolated`: https://docs.junit.org/5.14.0/api/org.junit.jupiter.api/org/junit/jupiter/api/parallel/Isolated.html
- opentest4j `MultipleFailuresError`: https://github.com/ota4j-team/opentest4j/blob/main/src/main/java/org/opentest4j/MultipleFailuresError.java

**Other:**

- Gson `JsonPrimitive.getAsBigDecimal`: https://github.com/google/gson/blob/gson-parent-2.14.0/gson/src/main/java/com/google/gson/JsonPrimitive.java
- OWASP ASVS 5.0 V2: https://github.com/OWASP/ASVS/blob/master/5.0/en/0x11-V2-Validation-and-Business-Logic.md

---

## 9. Unverified

1. **Probe scope.** All [Probed] results come from one run on 2026-11-12, using a fresh app on port 8093
   (`./mvnw -o -pl app spring-boot:run`, dev profile) and a Python `urllib` script. The concurrency probe was
   120 requests on 16 threads: 30 rounds of deposit 1, withdraw 2, and crossing 3.00 transfers between two
   new accounts. The result was 37×201 and 83×500, after which every invariant held. These were not written
   as tests. Why this differs from `banking-upskill.md`'s probe (4×422, no 500s) was **not investigated**.
   Two plausible causes are the mixed workload and two-account transfers.
2. **I4 (consecutive transfer ids)** is inferred from how IDENTITY columns usually behave in H2. I didn't
   test it under concurrency.
3. **`@AutoClose` vs `AfterEachCallback` ordering.** The User Guide says non-static `@AutoClose` fields close
   via `TestInstancePreDestroyCallback` "after the execution of each test method". The §5.18.1 table doesn't
   list `TestInstancePreDestroyCallback`, so its ordering relative to `AfterEachCallback` is **not
   verified**. `AfterTestExecutionCallback`, which runs before `@AfterEach`, avoids the question.
4. **When an `@Isolated` class runs** within a parallel run (first, last, or whenever the scheduler reaches
   it) was not checked. The Javadoc only promises that no other class runs at the same time.
5. **Surefire's count for `@ParameterizedTest`** (one per invocation) is from experience, not checked against
   Surefire docs for 3.5.6.
6. **BCBS 239.** `https://www.bis.org/publ/bcbs239.pdf` returned an HTML page to `curl` and only metadata to
   the fetch tool. I only saw the wording of Principle 3 (reconciliation with accounting data) in secondary
   summaries, so it isn't cited as a source above.
7. **Jackson's `BigDecimal` output** (plain `1250.00`, no exponent) was observed **[Probed]** and is covered by
   `ApiContractTest:99-105` for one deposit. I didn't check it against Jackson's documentation for the
   Spring Boot 4.1 line.
8. **Effort and outline** (§6) are judgement. No pilot has been run.
