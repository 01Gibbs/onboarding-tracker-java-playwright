# Lesson 07: Page Object Model

**Goal:** move locators and page interactions into page objects, so tests read as user intent and a UI change
means editing one file.

**Time:** about 2 hours

**Docs:** [Page object models](https://playwright.dev/docs/pom)

## Concepts

A page object wraps a `Page`, creates its locators once (they're lazy, so that's free), and offers
**intent-level** methods:

```ts
export class LoginPage {
  readonly username: Locator
  readonly password: Locator
  readonly signInButton: Locator

  constructor(private readonly page: Page) {
    this.username = page.getByLabel('Username')
    this.password = page.getByLabel('Password')
    this.signInButton = page.getByRole('button', { name: 'Sign in' })
  }

  async signInAs(user: User): Promise<DashboardPage> {
    await this.page.goto('/login')
    await this.username.fill(user.username)
    await this.password.fill(user.password)
    await this.signInButton.click()
    await this.page.getByTestId('current-user').waitFor()   // signed in: the page is ready
    return new DashboardPage(this.page)   // navigation methods return the page you land on
  }
}
```

Guidelines that keep page objects healthy:

- **Methods return the page you land on**, so a test reads as a story: `(await nav.customers()).newCustomer()`.
  In TypeScript every step is `await`ed; one step per line often reads better than a long chain.
- **Keep assertions in tests.** Page objects expose locators (`flash`, `rows`, `balance`) and tests decide what
  to expect. A short "wait until the page is ready" (`locator.waitFor()`) inside a page object is fine: here
  signing in is a background request, and the next step must not run before it has finished.
- **Shared parts of the page get their own class** (for example a `NavBar` used by every page).
- **Don't model every element.** Add methods when a test needs them.

## Exercise

1. Look at `e2e-ts/angular/support/users.ts` (provided): `TELLER`, `AUDITOR` and `LOCKED`.
2. Study `e2e-ts/angular/pages/login-page.ts`. It's a complete worked example of the pattern. Then complete the
   other starters in `e2e-ts/angular/pages/`, whose method signatures are already there:
   - `NavBar`: `customers()` → `CustomersPage`, `accounts()`, `signOut()`, `currentUser`
   - `CustomersPage`: `open()`, `search(text)`, `newCustomer()` → `CustomerFormPage`, `rows`,
     `openCustomer(fullName)` → `CustomerDetailPage`, `emptyMessage`
   - `CustomerFormPage`: `fill(first, last, email)`, `save()` → `CustomerDetailPage`, `saveExpectingErrors()`
     → itself, `errorSummary`. Its fields are public, so tests can
     `await expect(form.email).toHaveAccessibleDescription('…')`
   - `CustomerDetailPage`: `open(id)`, `nav`, `heading`, `flash`, `detail(term)`, `deleteAndConfirm()`
3. Complete `e2e-ts/angular/lesson07/customer-pages.spec.ts`:
   - Create a customer via the nav, search for them, open them, and check that Phone shows "Not provided"
   - Delete a customer, then check that a search finds nothing
   - Validation errors (empty first name, bad email)
   - Search for something that doesn't exist
4. **Refactor test.** Imagine the designers rename "Save customer" to "Save". How many files would you change
   now? How many before?

## Discussion

- `CustomerFormPage.save()` returns `CustomerDetailPage`, but what if the save fails validation? How does
  `saveExpectingErrors()` make the test clearer?
- Is the dashboard's "Recent activity" panel a page or a component?
- Lesson 09 hands page objects to tests as **fixtures** instead of `new`-ing them. What would that save?

## Checkpoint

- [ ] Your lesson 07 tests contain **no** `getByRole`, `getByLabel` or other locators. Those live only in `pages/`
- [ ] You could explain the "return the next page" pattern to a colleague

**Reference:** `pages/`, `lesson07/`

Next: [08: API testing](08-api-testing.md)
