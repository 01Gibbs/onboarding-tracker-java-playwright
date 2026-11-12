# Lesson 08: API testing with `APIRequestContext`

**Goal:** test the JSON API directly with Playwright, with no browser involved, and build a small
API client that later lessons use to set up data.

**Time:** about 2 hours

**Docs:** [API testing](https://playwright.dev/java/docs/api-testing) · [APIRequestContext](https://playwright.dev/java/docs/api/class-apirequestcontext) · [APIResponseAssertions](https://playwright.dev/java/docs/api/class-apiresponseassertions)

## Concepts

### Requests

Inject `APIRequestContext request`. Its base URL comes from `setApiRequestOptions` in
`LedgerlineOptions` (lesson 04).

```java
APIResponse response = request.post("/api/auth/login", RequestOptions.create()
        .setData(Map.of("username", "teller", "password", "teller-pass")));   // serialised as JSON

assertThat(response).isOK();                                  // any 2xx
JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();   // Gson
String token = body.get("token").getAsString();

request.get("/api/customers", RequestOptions.create()
        .setHeader("Authorization", "Bearer " + token)
        .setQueryParam("status", "SUSPENDED"));
```

Other useful pieces: `response.status()`, `response.headers()` (lower-case keys), `setForm(FormData…)`
for HTML forms, and `request.put/patch/delete`.

### Your own client

For a long-lived client with a fixed token, create a context yourself and **dispose** of it:

```java
APIRequestContext api = playwright.request().newContext(new APIRequest.NewContextOptions()
        .setBaseURL(LedgerlineOptions.BASE_URL)
        .setExtraHTTPHeaders(Map.of("Authorization", "Bearer " + token)));
...
api.dispose();
```

### What to test at the API level

Status codes, response shapes, validation messages, authorisation, and business rules. This is
**much** faster than going through the UI. Use the UI tests for things only the UI can show.
[The reference](../docs/ledgerline-reference.md#json-api) lists every endpoint and status code.

## Exercise

### Part 1: raw requests (starter: `workshop/lesson08/CustomerApiTest.java`)

Use `@UsePlaywright(LedgerlineOptions.class)`, and get a token in `@BeforeEach`.

1. `GET /api/customers` without a token → `401`.
2. List customers with `?status=SUSPENDED`. Every item has status `SUSPENDED`.
3. One test that does **create → read → update → delete** on a customer. Check `201` and the
   `location` header, `200`, the changed field, `204`, and finally `404`.
4. Invalid input → `400`. `fieldErrors.firstName` and `fieldErrors.email` have the expected messages.
5. Duplicate email → `409`.
6. Log in as **auditor**. `GET` works, but `POST` → `403`.

### Part 2: a typed client (`workshop/support/LedgerApi.java`)

Complete the starter `workshop/support/LedgerApi.java`. `loginAs`, `createCustomer(first, last, email)`
and `json` are done for you as worked examples; follow the same pattern for the rest:

```java
LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER);
JsonObject customer = api.createCustomer();                     // unique data
JsonObject account  = api.openAccount(customerId, "CURRENT", "Spending");
api.deposit(accountId, "100.00");
api.withdraw(accountId, "30.00", "Cash");
api.setAccountStatus(accountId, "FROZEN");
BigDecimal balance  = api.balanceOf(accountId);
JsonArray history   = api.transactions(accountId);
JsonObject funded   = api.accountWithBalance("250.00");         // customer + account + deposit
api.close();                                                    // dispose()
```

Then complete `workshop/lesson08/AccountApiTest.java` using it (inject `Playwright playwright` into
`@BeforeEach` and close the client in `@AfterEach`):

7. A new account is `ACTIVE`, with a zero balance and an 8-digit account number.
8. Deposit 100, withdraw 40.25 → balance `59.75`, 2 transactions, newest first.
9. Withdraw 10.01 from 10.00 → `422` with the exact message, and the balance is unchanged.
10. Transfer 75 between two new accounts → both balances are correct, and both entries share a
    `transferId`.
11. A frozen account rejects deposits (`422`).
12. An account with money can't be closed (`409`).

> Compare money with `BigDecimal`, not `double`. `new BigDecimal("59.75")` equals the value the
> API returns because both have scale 2. To compare values regardless of scale, use `compareTo`.

## Checkpoint

- [ ] 12 API tests that run in well under a second each
- [ ] `LedgerApi` exists. You'll use it in every remaining lesson

**Reference:** `solutions/lesson08/`, `solutions/support/LedgerApi.java`

Next: [09: Hybrid tests (API setup, UI verification)](09-hybrid-tests.md)
