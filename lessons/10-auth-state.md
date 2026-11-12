# Lesson 10: Authentication state and roles

**Goal:** stop signing in through the UI in every test. Sign in once, save the browser's storage
state, and reuse it. Then test what a read-only user can and can't do.

**Time:** about 1.5 hours

**Docs:** [Authentication](https://playwright.dev/java/docs/auth) · [API testing: reuse authentication state](https://playwright.dev/java/docs/api-testing)

## Concepts

### Storage state

A signed-in session is just cookies (and sometimes local storage). Playwright can save them to a
JSON file and load them into a new context:

```java
// save
context.storageState(new BrowserContext.StorageStateOptions().setPath(Path.of("target/.auth/auditor.json")));
// reuse
browser.newContext(new Browser.NewContextOptions().setStorageStatePath(Path.of("target/.auth/auditor.json")));
```

> Storage state files contain live session cookies, so treat them as **secrets**. Keep them under
> `target/` (or `playwright/.auth/`) and git-ignore them.

### Doing it with the JUnit fixtures

`@UsePlaywright` can inject `Browser.NewContextOptions` (already set up from your `OptionsFactory`,
including the base URL). Unlike `Page`, you can inject it into `@BeforeAll`:

```java
@BeforeAll
static void signInOnce(Browser browser, Browser.NewContextOptions options) {
    try (BrowserContext context = browser.newContext(options)) {
        new LoginPage(context.newPage()).loginAs(User.AUDITOR);
        context.storageState(new BrowserContext.StorageStateOptions().setPath(AUDITOR_STATE));
    }
}

@BeforeEach
void startSignedIn(Browser browser, Browser.NewContextOptions options) {
    context = browser.newContext(options.setStorageStatePath(AUDITOR_STATE));
    page = context.newPage();
}

@AfterEach
void closeContext() { context.close(); }
```

Here the test creates and owns its context, so it must also close it.

### Even faster: log in without a browser

`APIRequestContext` keeps cookies as well. Post the login **form** with it, then save *its* storage
state:

```java
request.post("/login", RequestOptions.create().setForm(FormData.create()
        .set("username", "teller").set("password", "teller-pass")));
request.storageState(new APIRequestContext.StorageStateOptions().setPath(TELLER_STATE));
```

### Test the server, not just the UI

Hiding a button is not security. `page.request()` sends HTTP requests **with the page's cookies**, so
you can check that the server itself refuses a write from a read-only user.

## Exercise

1. `workshop/lesson10/AuditorRoleTest.java` (the starter has test names only; you add the hooks):
   - `@BeforeAll`: sign in as **auditor** through the UI once and save the state to
     `target/.auth/auditor.json`.
   - `@BeforeEach`/`@AfterEach`: open and close a signed-in context.
   - Test: `/` shows "Alex Auditor (Auditor)", with no sign-in step.
   - Test: no "New customer" link; no "Delete customer" button on Alice Archer; no "Move money"
     region on account `1`, but the transaction rows **are** visible.
   - Test: `page.request().post("/accounts/1/status", form status=FROZEN)` → `403`.
2. Create `workshop/lesson10/TellerSessionViaApiTest.java` yourself: create the teller state in `@BeforeAll` using only
   an `APIRequestContext` (inject `Playwright playwright`), then assert that `/customers` shows "Terry
   Teller (Teller)".
3. **Stretch:** create the state with codegen, using
   `codegen --save-storage=target/.auth/manual.json http://localhost:8080/login`, then load it in a test.
4. **Stretch:** refactor your lesson 09 tests to start from saved teller state. Measure the difference.

## Discussion

- When would sharing one signed-in state between tests be a *bad* idea? (Think about tests that sign
  out, or change the user's settings.)
- Why does the 403 test matter even though the button is hidden?

## Checkpoint

- [ ] Auditor tests pass without signing in per test
- [ ] `.auth` files are git-ignored

**Reference:** `solutions/lesson10/`

Next: [11: Network interception and mocking](11-network.md)
