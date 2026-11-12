# Lesson 05: Recording tests with codegen

**Goal:** use Playwright's test generator to record a flow, then turn the recording into a
maintainable test.

**Time:** about 45 minutes

**Docs:** [Test generator](https://playwright.dev/java/docs/codegen) · [Codegen intro](https://playwright.dev/java/docs/codegen-intro)

## Concepts

Codegen opens a browser plus the **Playwright Inspector**. As you click and type, it writes Java code
that uses the best locators it can find. It's excellent for discovering locators and getting a first
draft quickly. **It is not the finished test.**

```bash
./mvnw -pl e2e-tests exec:java -Dexec.args="codegen http://localhost:8080/login"
```

Useful flags:

- `--target=java-junit` makes it generate a JUnit class with `@UsePlaywright`
- `--viewport-size="1280,800"`
- `--save-storage=target/auth.json` saves cookies when you close the browser (see lesson 10)

In the Inspector toolbar you can also **record assertions**: "assert visibility", "assert text" and
"assert value".

## Exercise

1. Start codegen (command above, add `--target=java-junit`) and record this flow:
   sign in as teller → **Customers** (in the top nav) → **New customer** → fill in first name, last
   name and email → **Save customer**. Use the Inspector toolbar to add an "assert text" on the green
   flash message.
2. Paste the generated test body into the starter `workshop/lesson05/CreateCustomerRecordedTest.java`.
3. Run it **twice**. The second run fails. Why? (Look at the business rules in
   [the reference](../docs/ledgerline-reference.md).)
4. Clean it up:
   - [ ] Use `@UsePlaywright(LedgerlineOptions.class)` and relative URLs
   - [ ] Delete redundant actions. `fill()` doesn't need a `click()` first, and you don't need `press("Tab")`
   - [ ] Generate **unique** data so the test can be repeated. Implement the three methods in
     the starter `workshop/support/TestData.java` (a short `UUID` fragment is enough)
   - [ ] Look at how codegen located the "Customers" link. It probably added `.setExact(true)`. Why?
     (Go to the dashboard: what else is called "Customers…"?) Replace it with a locator that says what
     you mean: the link **inside the Main navigation**
   - [ ] Assert the *outcome*, not just the flash: the h1 shows the new name, and the email is on the page
5. Try the **Pick locator** tool on a few elements you haven't used yet. Compare its suggestions with
   what you'd write by hand.

## Discussion

- What makes a recorded test fragile? List three things you changed and why.
- When is codegen *most* useful in your day-to-day work?

## Checkpoint

- [ ] A cleaned-up test that passes when you run it several times in a row
- [ ] `TestData` helper created; you'll use it from now on

**Reference:** `solutions/lesson05/CreateCustomerRecordedTest.java`, `solutions/support/TestData.java`

Next: [06: CRUD through the UI](06-crud-ui.md)
