# Lesson 00: Setup and a tour of the app

**Goal:** get Ledgerline running with its React front end, install Playwright's browser, and run your first
(provided) test.

**Time:** about 30 minutes

**Docs:** [Installation](https://playwright.dev/docs/intro) · [Browsers](https://playwright.dev/docs/browsers)

## 1. Prerequisites

- **Node 22, 24 or 26** with npm (`node -v`). The repo's `.nvmrc` says 24.
- **JDK 21 or newer** (`java -version`). Ledgerline's back end is a Spring Boot app; the repo includes the
  Maven Wrapper (`./mvnw`, or `mvnw.cmd` on Windows), so you don't need Maven.
- VS Code with the [Playwright Test extension](https://playwright.dev/docs/getting-started-vscode) is
  recommended, but any editor works.

## 2. Install

From the repo root:

```bash
npm install
npx playwright install chromium
```

`npm install` installs the front ends and the test project together (they are npm workspaces). Playwright
drives a real browser build that it downloads itself; add `--with-deps` on Linux. Newer npm versions may end
with "packages have install scripts not yet covered by allowScripts": that's expected, and nothing failed.

## 3. Start Ledgerline

Ledgerline is the "system under test": a small banking app with customers, accounts and money movements. All
data is fictional and resets every time it starts. Run each part in **its own terminal** and leave it running.

**Terminal 1**, the back end. Wait for `Started LedgerlineApplication`:

```bash
./mvnw -pl app spring-boot:run
```

**Terminal 2**, the React app:

```bash
npm run dev:react
```

Open <http://localhost:5173> and sign in as `teller` / `teller-pass`. The React app talks to the back end
through its `/api`, which Vite passes on to port 8080.

> You don't strictly need these terminals for tests: Playwright starts both servers itself if they aren't
> running (lesson 04 shows how). Starting them yourself is quicker, and you can explore the app.

## 4. Run the setup check

**Terminal 3:**

```bash
npm run test:react
```

This runs everything in `e2e-react/`, which is where **your** tests live. You should see 2 passed (the setup
check, and lesson 01's worked example) and the rest **skipped**: those are the exercises still to do.

> If the setup check fails with "Ledgerline is not answering", the back end isn't running: start it (step 3).

## 5. Explore the app (do this, it matters)

Good tests come from understanding the product. Spend 15 minutes clicking around, with
[`docs/ledgerline-reference.md`](../../docs/ledgerline-reference.md) as your map:

1. Sign in as **teller**. Find Alice Archer's "Everyday" account and deposit £10.
2. Try to withdraw more than the balance. What message do you get, and where does it appear?
3. Try a **Transfer**. Notice that the "To account" field only appears when Transfer is selected.
4. Open Ben Carter's "Holiday Fund". Why is there no "Move money" section?
5. Create a customer, then try to delete Alice Archer. Notice the **browser confirmation dialog**.
6. Sign out and sign in as **auditor**. What's missing?
7. Open DevTools → Network and reload the dashboard. Every number on the page came from a request to
   `/api/...`. Which ones?

Keep notes. Each of these behaviours becomes a test later in the course.

## How the course works

```
lessons/react/NN-*.md            ← what to learn and do
e2e-react/                      ← everything you run and write for this track
  ├─ playwright.config.ts        ← YOUR configuration (lesson 04)
  ├─ lessonNN/*.spec.ts          ← YOUR tests: starter files, one folder per lesson
  ├─ pages/, support/            ← YOUR page objects and helpers (lessons 07–09)
  ├─ servers.ts                  ← where the back end and the React app run
  ├─ solutions/                  ← reference answers (npm run solutions:react)
  └─ broken/                     ← lesson 13's broken suite
frontends/react/                 ← the React app you are testing
```

### Starter files

Each lesson has **starter files**. Early ones are nearly complete: lesson 01 includes a finished worked
example. Later ones contain only test names, and eventually there are none: you write everything yourself.

An exercise you haven't done yet calls `test.skip(true, 'TODO …')`, so it shows as **skipped**, not failed. On
a fresh checkout `npm run test:react` is green, with lots of skips. Replace each `test.skip(...)` line with
your code.

### Reference answers

Every "**Reference:**" path in the lessons is relative to `e2e-react/solutions/`. **Try first; compare after.**
To run them against the React app:

```bash
npm run solutions:react
```

Next: [01: Your first test](01-first-test.md)
