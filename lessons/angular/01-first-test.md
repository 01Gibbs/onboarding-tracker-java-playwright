# Lesson 01: Your first test

**Goal:** write and run a Playwright test, and understand what the `page` fixture gives you.

**Time:** about 45 minutes

**Docs:** [Writing tests](https://playwright.dev/docs/writing-tests) · [Fixtures](https://playwright.dev/docs/test-fixtures) · [Running tests](https://playwright.dev/docs/running-tests)

## Concepts

### A test, and its fixtures

```ts
import { expect, test } from '@playwright/test'

test('the sign-in page has the expected title', async ({ page }) => {
  await page.goto('/login')

  await expect(page).toHaveTitle('Sign in · Ledgerline')
})
```

`test` takes a name and an async function. The function asks for **fixtures** by name, here `{ page }`, and
Playwright Test creates them for you:

| Fixture | What you get | Lifetime |
|---------|--------------|----------|
| `page` | a fresh tab | new for every test |
| `context` | the browser context that owns that page: its cookies and storage | new for every test |
| `browser` | Chromium (from `playwright.config.ts`) | shared by the tests in a worker |
| `request` | an HTTP client for API calls (lesson 08) | new for every test |

`page.goto('/login')` is a **relative** URL: `playwright.config.ts` sets `baseURL` to the Angular app,
`http://localhost:4200`. Lesson 04 looks at that file properly.

### Web-first assertions

`expect(page).toHaveTitle(...)` **retries** until the condition is true or a timeout expires (5 seconds by
default). That's why every assertion on the page is `await`ed. You almost never need to wait or sleep yourself.

### Locating by role

```ts
page.getByRole('heading', { name: 'Sign in to Ledgerline' })
```

finds the element the way a screen reader would: "a heading called *Sign in to Ledgerline*". Lesson 03 covers
locators properly.

## Exercise

Open the starter `e2e-angular/lesson01/first.spec.ts`. Exercise 1 is already done as a worked example. Read
it line by line, run it, then replace each `test.skip(...)` line for exercises 2 and 3.

1. **Title.** Go to `/login` and expect the title `Sign in · Ledgerline`.
2. **Heading.** Expect the heading "Sign in to Ledgerline" to be visible.
3. **Redirect.** Go to `/customers` while signed out, and expect to end up on the sign-in page:
   `await expect(page).toHaveURL(/\/login$/)`.
4. **Break it.** Change the expected title to something wrong and run again. Read the failure message. (The
   `npm error … command failed` lines after it only mean a test failed: npm passes on Playwright's exit code.)
   Notice the test waited about 5 seconds before failing. Why? Put it back afterwards.

Run only this lesson:

```bash
npm run test:angular -- lesson01
```

### Watch it run

Tests are headless by default. Two ways to see them:

```bash
npm run test:angular -- lesson01 --headed     # a real browser window
npm run test:angular -- --ui                  # UI mode: pick tests, watch, and step back through each action
```

**UI mode** is worth getting used to now: its timeline shows the page before and after every action, and
"Pick locator" suggests a locator for anything you click.

> In exercise 3 the Angular app decides you aren't signed in only after asking the back end
> (`GET /api/auth/me`). `toHaveURL` retries, so the test doesn't need to know that.

## Checkpoint

- [ ] 3 passing tests in `e2e-angular/lesson01`
- [ ] You can explain why the failing assertion took about 5 seconds
- [ ] You ran the tests headed, and in UI mode, at least once

**Reference:** `lesson01/first.spec.ts`

Next: [02: Actions and assertions](02-actions-and-assertions.md)
