# Lesson 12: Debugging, tracing and scaling the suite

**Goal:** find out *why* a test failed without re-running it, then make the suite fast (parallel),
selective (tags) and CI-ready.

**Time:** about 2 hours

**Docs:** [Trace viewer](https://playwright.dev/java/docs/trace-viewer) · [Debugging](https://playwright.dev/java/docs/debug) · [Screenshots](https://playwright.dev/java/docs/screenshots) · [JUnit: parallel](https://playwright.dev/java/docs/junit) · [CI](https://playwright.dev/java/docs/ci)

## Part 1: Debugging

### Traces: a recording of everything

A trace is a zip file containing, for each action, before/after DOM snapshots, screenshots, network
traffic, console logs and the source line. The JUnit integration can keep traces **automatically for
failing tests**:

```java
return new Options()
        ...
        .setTrace(Options.Trace.RETAIN_ON_FAILURE);   // OFF | ON | RETAIN_ON_FAILURE
```

Traces are written to `e2e-tests/test-results/<TestClass>.<method>-chromium/trace.zip`.

> `setTrace` is in the 1.64 source code but isn't mentioned on the JUnit docs page.

Open a trace:

```bash
./mvnw -pl e2e-tests exec:java -Dexec.args="show-trace test-results/<folder>/trace.zip"
```

You can also drag the zip into <https://trace.playwright.dev>. It runs entirely in your browser, so
nothing is uploaded.

Manual tracing, for full control:

```java
context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
... // start tracing BEFORE creating the page
context.tracing().stop(new Tracing.StopOptions().setPath(Path.of("target/traces/my.zip")));
```

### Other tools

- **Headed + slow:** `-Dheadless=false`, plus `setSlowMo(…)` in your launch options.
- **Pause:** add `page.pause()` and run headed. The Inspector lets you step through actions and try
  locators against the live page.
- **Screenshots:** `page.screenshot(new Page.ScreenshotOptions().setPath(…).setFullPage(true))`, or
  `locator.screenshot(…)` for one element.
- **Verbose logs:** set the environment variable `DEBUG=pw:api` before running Maven.

## Part 2: Scaling

### Run in parallel

Create `e2e-tests/src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = same_thread
junit.jupiter.execution.parallel.mode.classes.default = concurrent
junit.jupiter.execution.parallel.config.strategy = dynamic
junit.jupiter.execution.parallel.config.dynamic.factor = 0.5
```

This runs test **classes** concurrently and the tests within a class sequentially. Playwright objects
aren't thread-safe, and `@UsePlaywright` gives each thread its own `Playwright`, so this is the
recommended mode.

Parallel runs only work if your tests are **independent**. Because you've owned your data since lesson
06, they already are. (A suite that calls `POST /api/testing/reset` in every test would break as soon
as two tests ran at the same time. Think about why.)

### Tag and filter

```java
@Tag("smoke")
@UsePlaywright(LedgerlineOptions.class)
class SmokeTest { ... }
```

Only the smoke tests:

```bash
./mvnw -pl e2e-tests test -Dgroups=smoke
```

Everything except tests tagged `slow`:

```bash
./mvnw -pl e2e-tests test -DexcludedGroups=slow
```

One lesson's tests:

```bash
./mvnw -pl e2e-tests test -Dtest='**/workshop/lesson09/*'
```

### CI

A minimal GitHub Actions job (the same steps work in any CI system):

```yaml
name: e2e
on: [push, pull_request]
jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: maven }
      - name: Build and start the app
        run: |
          ./mvnw -B -pl app -DskipTests package
          java -jar app/target/ledgerline-app-*.jar &
          for i in $(seq 1 60); do curl -sf localhost:8080/api/health && break; sleep 1; done
      - name: Install browsers
        run: ./mvnw -B -pl e2e-tests test-compile exec:java -Dexec.args="install --with-deps --only-shell chromium"
      - name: Run tests
        run: ./mvnw -B -pl e2e-tests test
      - uses: actions/upload-artifact@v4
        if: failure()
        with: { name: traces, path: e2e-tests/test-results }
```

## Exercise

1. Do the lesson 12 TODO in your `LedgerlineOptions`: add `.setTrace(Options.Trace.RETAIN_ON_FAILURE)`.
   From this lesson on there are no starter files. You create every class yourself.
2. Break a test on purpose (for example, expect the wrong balance in lesson 09), run it, and open the
   trace. Find the failing action, the DOM snapshot at that moment, and the network call that changed
   the balance. Then fix the test.
3. Write `workshop/lesson12/TracingAndScreenshotsTest.java`. It records a manual trace and saves a
   full-page screenshot plus a screenshot of just the balance element under `target/`. Assert that
   the files exist.
4. Create a `@Tag("smoke")` class with 2 fast, essential tests, and run only that class with `-Dgroups=smoke`.
5. Add `junit-platform.properties`, time the full workshop suite before and after, and record the
   numbers.
6. Make one of your tests deliberately order-dependent (for example, it relies on data another test
   creates). Run in parallel a few times and watch it become flaky. Then remove it.

## Checkpoint

- [ ] You can open a trace and explain a failure from it alone
- [ ] Suite runs in parallel with no failures, 3 runs in a row
- [ ] Smoke subset runs on its own

**Reference:** `solutions/lesson12/`, and the `solutions` profile in `e2e-tests/pom.xml`, which runs the
reference suite in parallel.

Next: [13: Fix the broken suite](13-fix-the-broken-suite.md)
