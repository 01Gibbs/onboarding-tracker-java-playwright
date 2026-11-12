# Lesson 04: Options, hooks and test isolation

**Goal:** remove the duplication from lessons 01–03. Put configuration in one place, use JUnit hooks
for common setup, and understand why every test starts with a clean browser.

**Time:** about 1 hour

**Docs:** [JUnit: Customizing options](https://playwright.dev/java/docs/junit) · [Browser contexts / isolation](https://playwright.dev/java/docs/browser-contexts) · [Test runners](https://playwright.dev/java/docs/test-runners)

## Concepts

### One shared `OptionsFactory`

Repeating `BASE_URL = System.getProperty(...)` and the browser settings in every test class means every
class changes when the environment does. Put the configuration in one class instead, and point every test class at it:

```java
public class LedgerlineOptions implements OptionsFactory {

    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "true"));

    @Override
    public Options getOptions() {
        return new Options()
                .setBaseUrl(BASE_URL)        // page.navigate("/customers") now works
                .setHeadless(HEADLESS)
                .setApiRequestOptions(new APIRequest.NewContextOptions().setBaseURL(BASE_URL));
    }
}

@UsePlaywright(LedgerlineOptions.class)
class SomeTest { ... }
```

`e2e-tests/pom.xml` already passes `baseUrl` and `headless` through to the tests as system properties:

```bash
./mvnw -pl e2e-tests test -Dheadless=false
./mvnw -pl e2e-tests test -DbaseUrl=http://staging.internal:8080
```

> `Options.setBaseUrl` only configures the **browser**. The `APIRequestContext` fixture needs its own
> base URL through `setApiRequestOptions` (you'll use this in lesson 08).

### Hooks receive fixtures too

Fixtures are injected into `@BeforeEach` and `@AfterEach` as well as into tests. Within one test, the
`Page` is the **same object** in the hook and in the test:

```java
@BeforeEach
void signIn(Page page) {
    page.navigate("/login");
    ...
}

@Test
void something(Page page) {   // already signed in
}
```

### Isolation

Every test gets a **new `BrowserContext`**: no cookies, no local storage and no session from earlier
tests. That's why each test must sign in, and why tests can run in any order (or in parallel, as you'll
see in lesson 12). Don't fight this. Tests that depend on each other's state are the second biggest
source of flakiness.

## Exercise

1. Complete the starter `workshop/support/LedgerlineOptions.java` (as above). Leave the lesson 12 TODO for now.
2. Complete `workshop/lesson04/OptionsAndHooksTest.java` (already annotated with `@UsePlaywright(LedgerlineOptions.class)`):
   - `@BeforeEach` signs in as teller using **relative** URLs only, and asserts that the
     `current-user` element is visible. That way a failed sign-in fails here, with a clear message.
   - Test 1: the three dashboard stat cards (`stat-customers`, `stat-active-accounts`,
     `stat-total-balance`) are visible and contain their labels.
   - Test 2: `/accounts` shows a link named `10000001`.
3. Complete `workshop/lesson04/IsolationTest.java` to *prove* isolation:
   - Use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order` (only for this demo).
   - Test 1 signs in and asserts that `context.cookies()` contains a `JSESSIONID` cookie.
     (Inject `BrowserContext context`.)
   - Test 2 asserts that `context.cookies()` is empty, and that `/customers` redirects to `/login`.
4. Run the suite with `-Dheadless=false`.
5. Go back to your lesson 02 and 03 tests and switch them to `LedgerlineOptions` with relative URLs.

## Discussion

- What would break if test 2 in `IsolationTest` could see test 1's cookies?
- Where else would you put environment-specific values? (Hint: system properties, environment variables, profiles.)

## Checkpoint

- [ ] No `http://localhost:8080` left in any of your tests
- [ ] `-Dheadless=false` works
- [ ] `IsolationTest` passes

**Reference:** `solutions/support/LedgerlineOptions.java`, `solutions/lesson04/`

Next: [05: Codegen](05-codegen.md)
