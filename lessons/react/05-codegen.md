# Lesson 05: Recording tests with codegen

**Goal:** use Playwright's test generator to record a flow, then turn the recording into a maintainable test.

**Time:** about 45 minutes

**Docs:** [Test generator](https://playwright.dev/docs/codegen) · [Codegen intro](https://playwright.dev/docs/codegen-intro)

## Concepts

Codegen opens a browser plus the **Playwright Inspector**. As you click and type, it writes Playwright Test code
that uses the best locators it can find. It's excellent for discovering locators and getting a first draft
quickly. **It is not the finished test.**

```bash
cd e2e-ts
npx playwright codegen http://localhost:5173/login
```

Useful flags:

- `--viewport-size="1280,800"`
- `--save-storage=playwright/.auth/manual.json` saves cookies when you close the browser (see lesson 10)

In the Inspector toolbar you can also **record assertions**: "assert visibility", "assert text" and "assert
value". In VS Code, the Playwright extension's "Record new" does the same inside the editor.

## Exercise

1. Start codegen and record this flow: sign in as teller → **Customers** (in the top nav) → **New customer** →
   fill in first name, last name and email → **Save customer**. Use the toolbar to add an "assert text" on the
   green flash message.
2. Paste the generated test body into the starter `e2e-ts/react/lesson05/create-customer-recorded.spec.ts`.
3. Run it **twice**. The second run fails. Why? (Look at the business rules in
   [the reference](../../docs/ledgerline-reference.md).)
4. Clean it up:
   - [ ] Relative URLs (`page.goto('/login')`): the base URL is in `playwright.config.ts`
   - [ ] Delete redundant actions. `fill()` doesn't need a `click()` first, and you don't need `press('Tab')`
   - [ ] Generate **unique** data so the test can be repeated: implement the three functions in the starter
     `e2e-ts/react/support/test-data.ts` (a short `randomUUID()` fragment is enough)
   - [ ] Look at how codegen located the "Customers" link. It probably added `exact: true`. Why? (Go to the
     dashboard: what else is called "Customers…"?) Replace it with a locator that says what you mean: the link
     **inside the Main navigation**
   - [ ] Assert the *outcome*, not just the flash: the h1 shows the new name, and the email is on the page
5. Try "Pick locator" on a few elements you haven't used yet. Compare its suggestions with what you'd write by
   hand.

## Discussion

- What makes a recorded test fragile? List three things you changed and why.
- When is codegen *most* useful in your day-to-day work?

## Checkpoint

- [ ] A cleaned-up test that passes when you run it several times in a row (`--repeat-each=3`)
- [ ] `test-data.ts` done; you'll use it from now on

**Reference:** `lesson05/create-customer-recorded.spec.ts`, `support/test-data.ts`

Next: [06: CRUD through the UI](06-crud-ui.md)
