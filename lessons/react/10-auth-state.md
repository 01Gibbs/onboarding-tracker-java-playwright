# Lesson 10: Authentication state and roles

**Goal:** stop signing in through the UI in every test. Sign in once, save the browser's storage state, and
reuse it. Then test what a read-only user can and can't do.

**Time:** about 1.5 hours

**Docs:** [Authentication](https://playwright.dev/docs/auth) · [Projects and dependencies](https://playwright.dev/docs/test-projects) · [API testing](https://playwright.dev/docs/api-testing)

## Concepts

### Storage state

A signed-in session is just cookies (and sometimes local storage). Ledgerline's React app keeps nothing in
local storage: being signed in is the server's `JSESSIONID` cookie. Playwright can save that to a JSON file and
start new contexts from it.

> Storage state files contain live session cookies, so treat them as **secrets**. They go in
> `e2e-ts/playwright/.auth/`, which is git-ignored.

### A setup project

The Playwright way is a **setup project**: a file of "tests" that sign in and save state, which runs before
the tests that need it.

```ts
// playwright.config.ts
projects: [
  { name: 'setup', testMatch: /.*\.setup\.ts/ },
  { name: 'chromium', use: { ...devices['Desktop Chrome'] }, dependencies: ['setup'] },
],
```

```ts
// auth.setup.ts
import { expect, test as setup } from '@playwright/test'

setup('sign in as the auditor', async ({ page }) => {
  await page.goto('/login')
  // ...fill in and submit the sign-in form
  await expect(page.getByTestId('current-user')).toContainText('Alex Auditor')
  await page.context().storageState({ path: 'playwright/.auth/auditor.json' })
})
```

```ts
// a spec file
test.use({ storageState: 'playwright/.auth/auditor.json' })   // every test here starts signed in
```

Playwright won't let a spec file import from a test file, so keep the file paths in a small module of their
own (say `support/auth-state.ts`) that both import.

### Even faster: sign in without a browser

The `request` fixture keeps cookies too. Sign in through the API, then save *its* state:

```ts
setup('sign in as the teller', async ({ request }) => {
  await request.post('/api/auth/session', { data: { username: 'teller', password: 'teller-pass' } })
  await request.storageState({ path: 'playwright/.auth/teller.json' })
})
```

`POST /api/auth/session` is exactly what the React sign-in page calls: open DevTools → Network and watch.

### Test the server, not just the UI

Hiding a button is not security. `page.request` sends HTTP requests **with the page's cookies**, so you can
check that the server itself refuses a write from a read-only user:

```ts
const response = await page.request.patch('/api/accounts/1', { data: { status: 'FROZEN' } })
expect(response.status()).toBe(403)
```

### Asserting that something is *not* there

`await expect(link).toHaveCount(0)` passes the moment it runs if the page hasn't loaded yet: no data, no link.
On a React page, first wait for something that **is** there (a row, a heading), then assert what isn't.

## Exercise

1. Add a `setup` project to `e2e-ts/react/playwright.config.ts` (the TODO there) and make `chromium` depend
   on it. Create `e2e-ts/react/lesson10/auth.setup.ts` that signs in as **auditor** through the UI and saves
   the state.
2. In `e2e-ts/react/lesson10/auditor-role.spec.ts`, start every test from that state with `test.use`. Then:
   `/` shows "Alex Auditor (Auditor)", with no sign-in step.
3. No "New customer" link; no "Delete customer" button on Alice Archer; no "Move money" region on account `1`,
   but its transaction rows **are** visible. Wait for something present before each "not there" check.
4. `page.request.patch('/api/accounts/1', ...)` to freeze the account → `403`.
5. Add a second setup that signs in as **teller** through the API only (`request`, no page), and a new spec
   file that uses it: `/customers` shows "Terry Teller (Teller)".
6. **Stretch:** save state with codegen, from the `e2e-ts` folder so the file lands next to the others:
   `cd e2e-ts && npx playwright codegen --save-storage=playwright/.auth/manual.json http://localhost:5173/login`.
   Then load it in a test.

## Discussion

- When would sharing one signed-in state between tests be a *bad* idea? (Think about tests that sign out, or
  change the user's settings.)
- Why does the 403 test matter even though the button is hidden?
- If the React app kept its sign-in token in `sessionStorage` instead of a cookie, what would break here?
  (Look up what `storageState` saves.)

## Checkpoint

- [ ] The auditor tests pass without signing in per test
- [ ] `playwright/.auth/` is git-ignored (`git status` shows nothing there)

**Reference:** `lesson10/`, `support/auth-state.ts`, and the `setup` project in `solutions/playwright.config.ts`

Next: lessons 11–14 are being written.
