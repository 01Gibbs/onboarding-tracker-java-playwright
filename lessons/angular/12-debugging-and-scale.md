# Lesson 12: Debugging, tracing and scaling the suite

**Goal:** find out *why* a test failed without re-running it, then make the suite fast (parallel), selective
(tags) and CI-ready.

**Time:** about 2 hours

**Docs:** [Trace viewer](https://playwright.dev/docs/trace-viewer) · [Debugging](https://playwright.dev/docs/debug) · [UI mode](https://playwright.dev/docs/test-ui-mode) · [Parallelism](https://playwright.dev/docs/test-parallel) · [Sharding](https://playwright.dev/docs/test-sharding) · [CI](https://playwright.dev/docs/ci-intro)

## Part 1: Debugging

### Traces: a recording of everything

A trace is a zip file with, for each action, before/after DOM snapshots, screenshots, network traffic, console
logs and the source line. Your `playwright.config.ts` already keeps one for every failing test:

```ts
use: { trace: 'retain-on-failure' }    // 'off' | 'on' | 'retain-on-failure' | 'on-first-retry'
```

Traces land in `e2e-ts/test-results/angular/<test>/trace.zip`, and the HTML report links to them:

```bash
cd e2e-ts
npx playwright show-report playwright-report/angular
npx playwright show-trace test-results/angular/<folder>/trace.zip
```

You can also drag the zip into <https://trace.playwright.dev>, which runs entirely in your browser.

### Other tools

- **UI mode** (`npm run test:angular -- --ui`): watch mode, a timeline, and the trace of every run, live.
- **Headed and paused:** `npm run test:angular -- --debug` opens the Inspector and pauses at the first action, so
  you can step through. `await page.pause()` pauses at a chosen line.
- **Screenshots:** `await page.screenshot({ path, fullPage: true })`, or `locator.screenshot(...)` for one
  element. `testInfo.outputPath('name.png')` gives each test its own folder, and `testInfo.attach(...)` puts a
  file in the HTML report.
- **Steps:** `await test.step('fund the account', async () => { ... })` groups actions in reports and traces.
- **Verbose logs:** `DEBUG=pw:api npm run test:angular`.

## Part 2: Scaling

### Parallel by default

Playwright Test runs test files in parallel **workers**, and `fullyParallel: true` (in your config) runs the
tests inside a file in parallel too. Choose how many with `--workers=4`, or `workers` in the config.

Parallel runs only work if your tests are **independent**. Because you've owned your data since lesson 06,
they already are. (A suite that called `POST /api/testing/reset` before every test would break as soon as two
tests ran at once. Think about why.)

### Tags and filters

```ts
test.describe('smoke', { tag: '@smoke' }, () => { ... })
```

```bash
npm run test:angular -- --grep @smoke             # only smoke
npm run test:angular -- --grep-invert @slow       # everything except slow
npm run test:angular -- lesson09                  # one lesson's files
```

### CI

A minimal GitHub Actions job (the same steps work in any CI system):

```yaml
name: e2e-angular
on: [push, pull_request]
jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: maven }
      - uses: actions/setup-node@v4
        with: { node-version: 24, cache: npm }
      - run: npm ci
      - run: npx playwright install --with-deps chromium
      - run: npm run test:angular   # webServer starts the back end and the Angular front end
        env: { CI: true }
      - uses: actions/upload-artifact@v4
        if: ${{ !cancelled() }}
        with: { name: playwright-report, path: e2e-ts/playwright-report }
```

For big suites, split the run across machines with `--shard=1/4` … `--shard=4/4`.

## Exercise

From this lesson on there are no starter files: you create every file yourself.

1. Break a test on purpose (for example, expect the wrong balance in lesson 09), run it, and open the trace.
   Find the failing action, the DOM snapshot at that moment, and the network call that changed the balance.
   Then fix the test.
2. Write `e2e-ts/angular/lesson12/tracing-and-screenshots.spec.ts`: record a trace by hand and save a full-page
   screenshot plus a screenshot of just the balance, then expect the files to exist. (Hint: while the config's
   `trace` option is on, Playwright owns tracing; `test.use({ trace: 'off' })` in this file hands it to you.)
3. Tag 2 fast, essential tests `@smoke` and run only those.
4. Time the whole workshop suite with `--workers=1` and with the default. Record the numbers.
5. Make one of your tests deliberately order-dependent (it relies on data another test creates). Run the suite a
   few times and watch it become flaky. Then remove it.

## Checkpoint

- [ ] You can open a trace and explain a failure from it alone
- [ ] The suite runs in parallel with no failures, 3 runs in a row
- [ ] The smoke subset runs on its own

**Reference:** `lesson12/`

Next: [13: Fix the broken suite](13-fix-the-broken-suite.md)
