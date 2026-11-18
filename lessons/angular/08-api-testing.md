# Lesson 08: API testing with the `request` fixture

**Goal:** test the JSON API directly, with no browser, and build a small API client that later lessons use to
set up data.

**Time:** about 2 hours

**Docs:** [API testing](https://playwright.dev/docs/api-testing) · [APIRequestContext](https://playwright.dev/docs/api/class-apirequestcontext) · [API assertions](https://playwright.dev/docs/api/class-apiresponseassertions)

## Concepts

### Requests

The `request` fixture is an HTTP client that uses the config's `baseURL`, so paths are relative:

```ts
const response = await request.post('/api/auth/login', {
  data: { username: 'teller', password: 'teller-pass' },     // sent as JSON
})
await expect(response).toBeOK()                               // any 2xx
const { token } = await response.json()

await request.get('/api/customers', {
  headers: { Authorization: `Bearer ${token}` },
  params: { status: 'SUSPENDED' },
})
```

Also useful: `response.status()`, `response.headers()` (lower-case names), `form: {...}` for HTML forms, and
`request.put/patch/delete`. A test that uses only `request` never starts a browser, so it's fast.

> The Angular front end passes `/api/...` on to the back end, so these requests go through it. The API is the
> same one the Angular app itself calls: everything it shows comes from these endpoints.

### Your own client

For a client with a fixed token, create a context yourself and **dispose** of it:

```ts
const api = await playwright.request.newContext({
  baseURL,
  extraHTTPHeaders: { Authorization: `Bearer ${token}` },
})
// ...
await api.dispose()
```

### Numbers, not strings

The API returns money as JSON numbers (`"balance": 59.75`) next to the text the pages show
(`"balanceDisplay": "£59.75"`). Compare numbers with numbers: `expect(account.balance).toBe(59.75)`.
`toBe('59.75')` fails, because a string is never a number.

### What to test at the API level

Status codes, response shapes, validation messages, authorisation and business rules. This is **much** faster
than going through the UI. Keep UI tests for what only the UI can show. [The
reference](../../docs/ledgerline-reference.md#json-api) lists every endpoint and status code.

## Exercise

### Part 1: raw requests (starter: `e2e-ts/angular/lesson08/customer-api.spec.ts`)

Get a token in `beforeEach`.

1. `GET /api/customers` without a token → `401`.
2. List customers with `?status=SUSPENDED`. Every item has status `SUSPENDED`.
3. One test that does **create → read → update → delete** on a customer. Check `201` and the `location` header,
   `200`, the changed field, `204`, and finally `404`.
4. Invalid input → `400`. `fieldErrors.firstName` and `fieldErrors.email` have the expected messages.
5. Duplicate email → `409`.
6. Sign in as **auditor**. `GET` works, but `POST` → `403`.

### Part 2: a typed client (`e2e-ts/angular/support/ledger-api.ts`)

Complete the starter. `signIn`, `raw` and `createCustomer` are done for you as worked examples; follow the same
pattern for the rest:

```ts
const api = await LedgerApi.signIn(playwright.request, baseURL!, TELLER)
const customer = await api.createCustomer()                        // unique data
const account  = await api.openAccount(customer.id, 'CURRENT', 'Spending')
await api.deposit(account.id, '100.00')
await api.withdraw(account.id, '30.00', 'Cash')
await api.setAccountStatus(account.id, 'FROZEN')
const balance  = await api.balanceOf(account.id)                   // 70
const history  = await api.transactions(account.id)
const funded   = await api.accountWithBalance('250.00')            // customer + account + deposit
await api.dispose()
```

Then complete `e2e-ts/angular/lesson08/account-api.spec.ts` using it (sign in in `beforeEach`, dispose in
`afterEach`):

7. A new account is `ACTIVE`, with a zero balance and an 8-digit account number.
8. Deposit 100, withdraw 40.25 → balance `59.75`, 2 transactions, newest first.
9. Withdraw 10.01 from 10.00 → `422` with the exact message, and the balance is unchanged.
10. Transfer 75 between two new accounts → both balances are right, and both entries share a `transferId`.
    `LedgerApi` has no transfer method yet (that comes in mission B0), so post to `/api/transfers` with
    `api.raw` (body: `fromAccountId`, `toAccountId`, `amount`).
11. A frozen account refuses deposits (`422`).
12. An account with money can't be closed (`409`).

## Checkpoint

- [ ] 12 API tests that each run in well under a second
- [ ] `LedgerApi` exists. You'll use it in every remaining lesson

**Reference:** `lesson08/`, `support/ledger-api.ts`

Next: [09: Hybrid tests (API setup, UI verification)](09-hybrid-tests.md)
