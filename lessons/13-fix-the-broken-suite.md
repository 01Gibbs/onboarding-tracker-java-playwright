# Lesson 13: Fix the broken suite

**Goal:** practise the job you'll do most often: someone else's test is red, and you have to find out
why and fix it properly, working from the failure output and the trace. Don't guess.

**Time:** about 2–3 hours

**Docs:** [Trace viewer](https://playwright.dev/java/docs/trace-viewer) · [Debugging](https://playwright.dev/java/docs/debug) · [Actionability](https://playwright.dev/java/docs/actionability) · [Best practices](https://playwright.dev/docs/best-practices)

## The set-up

`e2e-tests/src/broken/java/dev/upskill/e2e/broken/` contains ten tests: `Case01…` to `Case10…`.
Each one has **exactly one** realistic fault planted in it. The app is fine; the tests are wrong.
They use the reference page objects and support code, which do work.

Run them:

```bash
./mvnw -pl e2e-tests test -Pbroken
```

- Actions time out after **5 seconds** in this suite (see `BrokenSuiteOptions`), so failures are quick.
- Every failing test keeps a **trace** under `e2e-tests/test-results/<test>/trace.zip`. Open one with:

```bash
./mvnw -pl e2e-tests exec:java -Dexec.args="show-trace test-results/<folder>/trace.zip"
```

> **Run the whole suite at least twice.** One test passes the first time and fails afterwards.
> Another can occasionally pass on a fast machine, but it's still broken. "It passed once" is not
> evidence that a test is correct.

## The rules

1. **Diagnose before you edit.** For each case, write down the following, in your notes or as a comment:
   - **Symptom:** what the failure says
   - **Evidence:** what in the error, call log or trace proves the cause
   - **Root cause:** one sentence
   - **Fix:** what you changed
2. **Fix the test, not the app.** Here the app behaves correctly.
3. **No band-aids.** None of these count as fixes: `Thread.sleep`, bigger timeouts, `force(true)`,
   retries, or `@Disabled`.
4. **Keep the test's intent.** The method name says what it should prove, so make it prove that.

## Where to look

| Clue in the output | Usually means |
|--------------------|---------------|
| `Expected: … Received: …` | The test's expectation is wrong, or the app really is wrong. Read both values carefully. |
| `strict mode violation … resolved to 2 elements` | The locator is ambiguous; the message lists every match |
| `Timeout … waiting for …` and a call log | An action waited for something that never became possible. The call log says what it was waiting *for* (visible? enabled?) |
| A plain JUnit `expected: <x> but was: <y>` | A non-Playwright assertion, which checks once and never retries |
| `NullPointerException` in a test | Look at where that value was supposed to come from |
| Passes alone, fails in a run (or the reverse) | Shared state or test-order dependence |
| Passes once, fails on the next run | Leftover data from the previous run |

In the trace viewer, step through the **Actions** list. For each step, look at the **Before/After**
snapshots, the **Call** and **Log** tabs (what Playwright waited for), and the **Network** tab.

## Exercise

Fix all ten so that this passes **three times in a row**:

```bash
./mvnw -pl e2e-tests test -Pbroken
```

Suggested order (roughly easiest first): 01, 02, 03, 04, 05, 06, 07, 08, 09, 10.

Work in pairs if you can. One person drives and the other reads the evidence aloud.

## Debrief (as a group)

- Which faults would **only** show up in CI, or only sometimes?
- Which ones would a code reviewer have caught by reading the test? Which needed the trace?
- For each fault, which earlier lesson's guideline would have prevented it?

When you're done, or truly stuck, compare your notes with
[`answers/13-fix-the-broken-suite.md`](answers/13-fix-the-broken-suite.md).

## Checkpoint

- [ ] 10/10 green, 3 runs in a row
- [ ] Symptom, evidence, root cause and fix written down for each case
- [ ] No sleeps, longer timeouts, `force`, retries or disabled tests

Next: [14: Capstone](14-capstone.md)
