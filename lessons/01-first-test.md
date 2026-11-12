# Lesson 01: Your first test

**Goal:** write and run a Playwright test with JUnit 5, and understand what `@UsePlaywright` gives you.

**Time:** about 45 minutes

**Docs:** [Writing tests](https://playwright.dev/java/docs/writing-tests) · [JUnit integration](https://playwright.dev/java/docs/junit) · [Running tests](https://playwright.dev/java/docs/running-tests)

## Concepts

### Two ways to manage Playwright objects

Playwright has four core objects: **Playwright** → **Browser** → **BrowserContext** (an isolated,
incognito-like profile) → **Page** (a tab).

The [Writing tests](https://playwright.dev/java/docs/writing-tests) page shows you how to create them
by hand in `@BeforeAll`/`@BeforeEach`. Playwright's **JUnit integration** does the same work for you.
Annotate the class with `@UsePlaywright` and ask for what you need as method parameters:

```java
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;

@UsePlaywright
class FirstTest {

    @Test
    void loginPageHasTheExpectedTitle(Page page) {
        page.navigate("http://localhost:8080/login");
        assertThat(page).hasTitle("Sign in · Ledgerline");
    }
}
```

| Parameter | What you get | Lifetime |
|-----------|--------------|----------|
| `Page` | a fresh tab | new for every test |
| `BrowserContext` | the context that owns that page | new for every test |
| `Browser` | Chromium by default | shared |
| `Playwright` | the root object | shared per thread |
| `APIRequestContext` | an HTTP client (lesson 08) | new for every test |

> The JUnit integration is marked **experimental** in the docs, but it is stable in practice and is
> the least code. You can still fall back to manual setup.

### Web-first assertions

`assertThat(...)` comes from `com.microsoft.playwright.assertions.PlaywrightAssertions`. It is **not**
the JUnit or AssertJ one. It **retries** until the condition is true or a timeout expires (5 seconds by
default). You almost never need `Thread.sleep` with Playwright.

### Locating by role

```java
page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Sign in to Ledgerline"))
```

finds the element the way a screen reader would: "a heading called *Sign in to Ledgerline*".
Lesson 03 covers locators properly.

## Exercise

Open the starter `e2e-tests/src/test/java/dev/upskill/e2e/workshop/lesson01/FirstTest.java`. Exercise 1 is
already done as a worked example. Read it line by line, run it, then replace each `abort(...)` line
for exercises 2 and 3.

1. **Title.** Navigate to `http://localhost:8080/login` and assert that the title is `Sign in · Ledgerline`.
2. **Heading.** Assert that the heading "Sign in to Ledgerline" is visible.
3. **Redirect.** Navigate to `http://localhost:8080/customers` while signed out, and assert that you end up
   on the login page: `assertThat(page).hasURL(Pattern.compile(".*/login$"))`.
4. **Break it.** Change the expected title to something wrong and run again. Read the failure
   message. Notice that the test waited about 5 seconds before failing. Why? Put it back afterwards.

Run only your class:

```bash
./mvnw -pl e2e-tests test -Dtest='**/workshop/lesson01/*'
```

(Use a path pattern. `-Dtest=FirstTest` would also pick up the reference solution of the same name.)

### Watch it run

Tests are headless by default. To see the browser, set headless off for this class:

```java
@UsePlaywright(FirstTest.Headed.class)
class FirstTest {
    public static class Headed implements OptionsFactory {
        @Override
        public Options getOptions() {
            return new Options().setHeadless(false)
                    .setLaunchOptions(new BrowserType.LaunchOptions().setSlowMo(500));
        }
    }
    ...
}
```

`slowMo` adds a delay (in ms) to each action so you can follow what's happening. Lesson 04 replaces
this with a shared, configurable options class.

> **Docs gotcha:** some versions of the JUnit docs show `setContextOption(...)`, but the real method
> is **`setContextOptions(...)`** (plural).

## Checkpoint

- [ ] 3 passing tests in `workshop/lesson01`
- [ ] You can explain why the failing assertion took about 5 seconds
- [ ] You ran the tests headed at least once

**Reference:** `solutions/lesson01/FirstTest.java`

Next: [02: Actions and assertions](02-actions-and-assertions.md)
