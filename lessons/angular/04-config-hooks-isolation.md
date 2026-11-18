# Lesson 04: Configuration, hooks and test isolation

**Goal:** understand `playwright.config.ts`, remove the duplication from lessons 01–03 with hooks, and see why
every test starts with a clean browser.

**Time:** about 1 hour

**Docs:** [Test configuration](https://playwright.dev/docs/test-configuration) · [Web server](https://playwright.dev/docs/test-webserver) · [Isolation](https://playwright.dev/docs/browser-contexts) · [Test CLI](https://playwright.dev/docs/test-cli)

## Concepts

### `playwright.config.ts`

Open `e2e-angular/playwright.config.ts`. Everything that would otherwise be repeated in every test lives here
(shown without its reporter setting):

```ts
export default defineConfig({
  testDir: '.',
  testIgnore: ['**/solutions/**', '**/broken/**'],   // the answers and lesson 13 have their own configs
  fullyParallel: true,
  use: {
    baseURL: process.env.BASE_URL ?? frontEnd.url,         // page.goto('/customers') → :4200/customers
    trace: 'retain-on-failure',
  },
  // No servers to start when BASE_URL points at an environment that's already running
  webServer: process.env.BASE_URL ? undefined : [backEnd, frontEnd.webServer],
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
```

- **`use`** sets options for every test: the base URL, the browser's viewport, locale, timezone, and more.
  `test.use({ ... })` at the top of a file overrides them for that file.
- **`webServer`** starts the back end and the Angular app before the tests, waits until their URLs answer, and
  stops them afterwards. `reuseExistingServer: true` (in `e2e-angular/servers.ts`) means: if you already started
  one in a terminal, use that. So tests work whether or not you started Ledgerline yourself.
- **`projects`** run the same tests with different settings (browsers, devices, signed-in roles). Lesson 10
  adds one.
- Environment-specific values come from environment variables: `BASE_URL=http://staging.example npm run
  test:angular` points every test at another environment without changing code.

### Hooks

```ts
test.beforeEach(async ({ page }) => {
  await page.goto('/login')
  // ...sign in
})

test('something', async ({ page }) => {   // the same page, already signed in
})
```

`test.beforeEach` runs before every test in the file (or in the `test.describe` block it's in), and gets the
same fixtures. There's also `afterEach`, `beforeAll` and `afterAll`. A hook that ends with an assertion
("signed in? `current-user` is visible") makes a broken sign-in fail *there*, with a clear message, instead of
somewhere confusing later.

### Isolation

Every test gets a **new browser context**: no cookies, no storage, no session from earlier tests. That's why
each test must sign in, and why tests can run in any order, or in parallel (`fullyParallel: true`). Don't
fight this. Tests that depend on each other's state are the second biggest source of flakiness.

## Exercise

1. Complete `e2e-angular/lesson04/options-and-hooks.spec.ts`:
   - `test.beforeEach` signs in as teller using relative URLs only, and expects `current-user` to be visible.
   - Test 1: the three dashboard stat cards (`stat-customers`, `stat-active-accounts`, `stat-total-balance`)
     contain their labels ("Customers", "Active accounts", and a "£").
   - Test 2: `/accounts` shows a link named `10000001`.
2. Complete `e2e-angular/lesson04/isolation.spec.ts` to *prove* isolation:
   - Make the two tests run in order with `test.describe.configure({ mode: 'default' })` (only for this demo:
     the config otherwise runs them in parallel).
   - Test 1 signs in and expects `await context.cookies()` to include a cookie named `JSESSIONID`.
   - Test 2 expects `await context.cookies()` to be empty, and `/customers` to send you to `/login`.
3. Try the command line:
   - `npm run test:angular -- lesson04 --headed`
   - `npm run test:angular -- -g "summary cards"` runs tests whose title matches
   - `npm run test:angular -- --last-failed` re-runs only what failed last time (if nothing failed, it says "No tests found")
4. Stop the Angular app in terminal 2, and run `npm run test:angular -- lesson04` again. What happened, and how
   long did it take?

## Discussion

- What would break if test 2 in the isolation file could see test 1's cookies?
- The `JSESSIONID` cookie is the **server's** session, set through the Angular app's `/api` proxy. What does that
  tell you about where "being signed in" lives in this app?
- When would you use `test.use({ ... })` in a file instead of changing `playwright.config.ts`?

## Checkpoint

- [ ] No `http://localhost` in any of your tests
- [ ] `--headed`, `-g` and `--last-failed` all work for you
- [ ] The isolation tests pass

**Reference:** `lesson04/`

Next: [05: Codegen](05-codegen.md)
