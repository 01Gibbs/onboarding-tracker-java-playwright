# Lesson 02: Actions and assertions

**Goal:** drive the UI (fill, click, press) and verify the results with web-first assertions, using the
sign-in flow.

**Time:** about 1 hour

**Docs:** [Actions](https://playwright.dev/docs/input) · [Assertions](https://playwright.dev/docs/test-assertions) · [Auto-waiting](https://playwright.dev/docs/actionability)

## Concepts

### Actions

```ts
await page.getByLabel('Username').fill('teller')          // clears the field, then types
await page.getByLabel('Password').press('Enter')          // keyboard
await page.getByRole('button', { name: 'Sign in' }).click()
await page.getByLabel('Status').selectOption('Frozen')    // <select>, by value or label
await page.getByRole('radio', { name: 'Transfer' }).check()
```

Every action is `await`ed. Forgetting an `await` is the classic Playwright Test bug: the test races ahead of
the browser. The [no-floating-promises](https://typescript-eslint.io/rules/no-floating-promises/) lint rule
catches it.

### Auto-waiting

Before each action, Playwright waits until the element is **attached, visible, stable, enabled and able to
receive events** (see [Actionability](https://playwright.dev/docs/actionability)). On an Angular page that
matters: a button may exist before its data has loaded, and Playwright waits for it to be ready.

### Web-first assertions

| Use this | Not this |
|----------|----------|
| `await expect(locator).toBeVisible()` | `expect(await locator.isVisible()).toBe(true)` |
| `await expect(locator).toHaveText('…')` | `expect(await locator.textContent()).toBe('…')` |
| `await expect(page).toHaveURL(/\/$/)` | `expect(page.url()).toBe('…')` |

The left column **retries**. The right column checks once, immediately, and is the number one cause of flaky
tests, especially on a single-page app, where the page keeps changing after it first appears. Other useful
ones: `toContainText`, `toHaveValue`, `toBeEmpty`, `toHaveAttribute`, `toHaveCount`, `toBeDisabled`,
`toBeChecked`, `toBeHidden`, and `not`: `await expect(locator).not.toBeVisible()`.

## Exercise

Open the starter `e2e-ts/angular/lesson02/login.spec.ts`. The actions for exercise 1 are written for you, and
you add the assertions. For exercises 2–4 the comments list the steps and you write the code.

1. **Happy path.** Sign in as `teller` / `teller-pass`. Expect:
   - the URL to end with `/`
   - the "Dashboard" heading to be visible
   - `getByTestId('current-user')` to contain "Terry Teller"
2. **Wrong password.** Expect:
   - an alert (`getByRole('alert')`) to say exactly `Invalid username or password`
   - the URL to still end with `/login`
   - the username field to still have the value `teller`, and the password field to be empty
3. **Locked user.** Sign in as `locked` / `locked-pass`, this time by pressing **Enter** in the password field
   instead of clicking. Expect the alert to mention "locked".
4. **Sign out.** Sign in, click "Sign out", and expect the flash message (`getByTestId('flash-message')`) to
   say "You have been signed out". Then go to `/customers` and expect to be sent back to the sign-in page.

You will repeat the sign-in steps in every test. That's fine for now: notice the duplication, and lesson 04
removes it.

## Discussion

- In step 2, why `toHaveText` and not `toContainText`? When would you choose each?
- In step 4, nothing in your test waits for the sign-out request to finish. Why does it still work?

## Checkpoint

- [ ] 4 passing tests in `e2e-ts/angular/lesson02`
- [ ] No `waitForTimeout`, and no `expect(await locator.isVisible())`

**Reference:** `lesson02/login.spec.ts`

Next: [03: Locators](03-locators.md)
