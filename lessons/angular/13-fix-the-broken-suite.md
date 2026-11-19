# Lesson 13: Fix the broken suite

**Goal:** practise the job you'll do most often: someone else's test is red, and you have to find out why and
fix it properly, working from the failure output and the trace. Don't guess.

**Time:** about 2–3 hours

**Docs:** [Trace viewer](https://playwright.dev/docs/trace-viewer) · [Debugging](https://playwright.dev/docs/debug) · [Actionability](https://playwright.dev/docs/actionability) · [Best practices](https://playwright.dev/docs/best-practices)

## The set-up

`e2e-angular/broken/` holds ten cases (11 tests): `case01…` to `case10…`. The runner also lists two sign-in set-up steps, which pass. Each has **exactly one** realistic fault planted in
it. Ledgerline is fine; the tests are wrong. They use the reference page objects, fixtures and API client, which
do work.

Run them:

```bash
npm run broken:angular
```

- Actions and assertions give up after **5 seconds** in this suite, so failures are quick. Opening a page and
  the API client's own requests keep the usual allowance, so a busy machine can't fail a case you have fixed.
- Every failing test keeps a **trace**. The HTML report links to each one:
  `cd e2e-angular && npx playwright show-report broken/playwright-report`

> **Run the whole suite at least twice, with the back end running in its own terminal.** One test passes the
> first time against a freshly started back end and fails afterwards. (If Playwright starts the back end for
> you, it starts a fresh one every run, which hides that fault.) "It passed once" is not evidence that a test is
> correct.

## The rules

1. **Diagnose before you edit.** For each case, write down:
   - **Symptom:** what the failure says
   - **Evidence:** what in the error, call log or trace proves the cause
   - **Root cause:** one sentence
   - **Fix:** what you changed
2. **Fix the test, not Ledgerline.** Here Ledgerline behaves correctly.
3. **No band-aids.** None of these count as fixes: `waitForTimeout`, bigger timeouts, `force: true`, retries,
   or `test.skip`.
4. **Keep the test's intent.** Its title says what it should prove, so make it prove that.

## Where to look

| Clue in the output | Usually means |
|--------------------|---------------|
| `Expected: … Received: …` | The expectation is wrong, or the app really is. Read both values carefully, including their types (`"90.00"` vs `90`) |
| `strict mode violation … resolved to 2 elements` | The locator is ambiguous; the message lists every match |
| `Timeout … waiting for …` and a call log | An action waited for something that never became possible. The call log says what it waited *for* |
| A plain `expect(await locator.count())` or `allTextContents()` | A value read once, right now, instead of a retrying assertion |
| `TypeError: Cannot read properties of undefined` | Look at where that value was supposed to come from, and when |
| A route that never seems to apply | Its pattern doesn't match the real URL, or it was added too late |
| Passes once, fails on the next run | Leftover data from the previous run |

In the trace viewer, step through the **Actions** list. For each step, look at the **Before/After** snapshots,
the **Call** and **Log** tabs (what Playwright waited for), and the **Network** tab.

## Exercise

Fix all ten so that `npm run broken:angular` passes **three times in a row**, with the back end running in its
own terminal. Suggested order, roughly easiest first: 01, 02, 03, 04, 05, 06, 07, 08, 09, 10.

Work in pairs if you can. One person drives and the other reads the evidence aloud.

## Debrief (as a group)

- Which faults would **only** show up in CI, or only sometimes?
- Which ones would a code reviewer have caught by reading the test? Which needed the trace?
- For each fault, which earlier lesson would have prevented it?

When you're done, or truly stuck, compare your notes with
[`answers/13-fix-the-broken-suite.md`](answers/13-fix-the-broken-suite.md).

## Checkpoint

- [ ] All 11 tests green (13 passed, with the 2 sign-in set-up steps), 3 runs in a row
- [ ] Symptom, evidence, root cause and fix written down for each case
- [ ] No sleeps, longer timeouts, `force`, retries or skipped tests

Next: [14: Capstone](14-capstone.md)
