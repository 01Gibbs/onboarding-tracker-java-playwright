# Lesson 02: Actions and assertions

**Goal:** drive the UI (fill, click, press) and verify the results with web-first assertions, using
the sign-in flow.

**Time:** about 1 hour

**Docs:** [Actions](https://playwright.dev/java/docs/input) · [Assertions](https://playwright.dev/java/docs/test-assertions) · [Auto-waiting](https://playwright.dev/java/docs/actionability)

## Concepts

### Actions

```java
page.getByLabel("Username").fill("teller");          // clears the field, then types
page.getByLabel("Password").press("Enter");          // keyboard
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
page.getByLabel("Status").selectOption("Frozen");    // <select>, by value or label
page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Transfer")).check();
```

### Auto-waiting

Before each action, Playwright waits until the element is **attached, visible, stable, enabled and
able to receive events** (see [Actionability](https://playwright.dev/java/docs/actionability)). If a
click starts a page navigation, `click()` also waits for that navigation to begin. This removes most
of the timing problems you may have had with other tools.

### Web-first assertions

| Use this | Not this |
|----------|----------|
| `assertThat(locator).isVisible()` | `assertTrue(locator.isVisible())` |
| `assertThat(locator).hasText("…")` | `assertEquals("…", locator.textContent())` |
| `assertThat(page).hasURL(…)` | `assertEquals(…, page.url())` |

The left column **retries**. The right column checks once, immediately, and is the number one cause of
flaky tests. Other useful ones: `containsText`, `hasValue`, `isEmpty`, `hasAttribute`, `hasCount`,
`isDisabled`, `isChecked`, `isHidden`, and the negation `assertThat(x).not().isVisible()`.

## Exercise

Open the starter `workshop/lesson02/LoginTest.java`. The actions for exercise 1 are written for you,
and you add the assertions. For exercises 2–4, the comments list the steps and you write the code.

1. **Happy path.** Sign in as `teller` / `teller-pass`. Assert:
   - the URL is `BASE_URL + "/"`
   - the "Dashboard" heading is visible
   - `getByTestId("current-user")` contains "Terry Teller"
2. **Wrong password.** Assert that:
   - an alert (`getByRole(AriaRole.ALERT)`) says exactly `Invalid username or password`
   - you're still on `/login`
   - the username field still has the value `teller`, and the password field is empty
3. **Locked user.** Sign in as `locked` / `locked-pass`, this time by pressing **Enter** in the password
   field instead of clicking. Assert that the alert mentions "locked".
4. **Sign out.** Sign in, click "Sign out", and assert that the flash message
   (`getByTestId("flash-message")`) says "You have been signed out". Then navigate to `/customers` and
   assert that you're sent back to the login page.

You will repeat the sign-in steps in every test. That's fine for now: notice the duplication, and
we'll remove it in lesson 04.

## Discussion

- In step 2, why `hasText` and not `containsText`? When would you choose each?
- In step 4, nothing in your test waits for the sign-out to finish. Why does it still work?

## Checkpoint

- [ ] 4 passing tests in `workshop/lesson02`
- [ ] No `Thread.sleep`, and no `assertTrue(locator.isVisible())`

**Reference:** `solutions/lesson02/LoginTest.java`

Next: [03: Locators](03-locators.md)
