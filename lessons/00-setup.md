# Lesson 00: Setup and a tour of the app

**Goal:** get the app running, install the Playwright browsers, and run your first (provided) test.

**Time:** about 30 minutes

**Docs:** [Installation](https://playwright.dev/java/docs/intro) · [Browsers](https://playwright.dev/java/docs/browsers)

## 1. Prerequisites

- **JDK 21 or newer** (`java -version`). You don't need to install Maven, because the repo includes the
  Maven Wrapper (`./mvnw`, or `mvnw.cmd` on Windows).
- An IDE with JUnit support (IntelliJ IDEA or VS Code with the Java extensions).
- Network access to Maven Central and the Playwright CDN for the first build. After that, everything
  is cached locally.

## 2. Start the app

The app is the "system under test". Run it in **its own terminal** and leave it running. The command
doesn't return, and the tests need the app to be up. Wait for `Started LedgerlineApplication`:

```bash
./mvnw -pl app spring-boot:run
```

Open <http://localhost:8080> and sign in as `teller` / `teller-pass`.

## 3. Install the browsers

Playwright drives real browser builds that it downloads itself. In a second terminal:

```bash
./mvnw -pl e2e-tests test-compile exec:java -Dexec.args="install chromium"
```

The exec plugin is already set up in `e2e-tests/pom.xml` to run Playwright's CLI
(`com.microsoft.playwright.CLI`). You'll use it again for `codegen` (lesson 05) and `show-trace`
(lesson 12). Install all browsers with `-Dexec.args="install"`.

## 4. Run the setup check

```bash
./mvnw -pl e2e-tests test
```

This runs everything in `e2e-tests/src/test/java/dev/upskill/e2e/workshop/`, which is where **your**
tests will live. At the moment it only contains `SetupCheckTest`, which calls the app's health
endpoint. You should see `Tests run: 1, Failures: 0`.

> If it fails with `ECONNREFUSED` / "Ledgerline is not running", start the app in another terminal
> (step 2) and run the tests again.

## 5. Explore the app (do this, it matters)

Good tests come from understanding the product. Spend 15 minutes clicking around, using
[`docs/ledgerline-reference.md`](../docs/ledgerline-reference.md) as your map:

1. Sign in as **teller**. Find Alice Archer's "Everyday" account and deposit £10.
2. Try to withdraw more than the balance. What message do you get, and where does it appear?
3. Try a **Transfer**. Notice that the "To account" field only appears when Transfer is selected.
4. Open Ben Carter's "Holiday Fund". Why is there no "Move money" section?
5. Create a customer, then try to delete Alice Archer. Notice the **browser confirmation dialog**.
6. Sign out and sign in as **auditor**. What's missing?
7. Open your browser's DevTools → Network tab, and reload the dashboard. Find the request that loads
   "Recent activity".

Keep notes. Each of these behaviours becomes a test later in the course.

## How the course works

```
lessons/NN-*.md                       ← what to learn and do
lessons/answers/                      ← answer notes (spoilers) where a lesson needs them
e2e-tests/src/
  ├─ test/java/.../e2e/workshop/      ← YOUR code: starter files for each lesson (run by default)
  │    ├─ lessonNN/                   ← one package per lesson
  │    ├─ pages/                      ← page objects you build (lessons 07, 09, 11)
  │    └─ support/                    ← helpers you build (lessons 04, 05, 08)
  ├─ solutions/java/.../e2e/solutions/ ← reference answers, same layout (only built with -Psolutions)
  └─ broken/java/.../e2e/broken/       ← lesson 13's broken suite (only built with -Pbroken)
```

### Starter files

Each lesson has **starter files** in `workshop/`. Early lessons are nearly complete: lesson 01 includes a
finished worked example and asks you to fill in the rest. Later starters contain only method names and
signatures, and from lesson 12 onwards there are none: you write everything yourself.

An exercise you haven't done yet calls `abort("TODO …")`, so it shows as **skipped**, not failed. On a
fresh checkout, `./mvnw -pl e2e-tests test` is green, with lots of skips. Replace each `abort(...)` line
with your code. Unfinished page objects and helpers throw `UnsupportedOperationException("TODO lesson NN")`,
so you'll know exactly what's missing.

### Reference answers

Every "**Reference:**" path in the lessons is relative to
`e2e-tests/src/solutions/java/dev/upskill/e2e/`. They're kept out of the normal build on purpose, so
they don't appear in your IDE's completion or test runner. **Try first; compare after.** To run them
against your app (and to see them in IntelliJ, tick the `solutions` profile in the Maven tool window):

```bash
./mvnw -pl e2e-tests test -Psolutions
```

Next: [01: Your first test](01-first-test.md)
