# Lesson 07: Page Object Model

**Goal:** move locators and page interactions into page objects, so tests read as user intent and a
UI change means editing one file.

**Time:** about 2 hours

**Docs:** [Page object models](https://playwright.dev/java/docs/pom)

## Concepts

A page object wraps a `Page`, creates its `Locator`s once (they're lazy, so that's free), and exposes
**intent-level** methods:

```java
public class LoginPage {
    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator signIn;

    public LoginPage(Page page) {
        this.page = page;
        this.username = page.getByLabel("Username");
        this.password = page.getByLabel("Password");
        this.signIn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
    }

    public DashboardPage loginAs(User user) {
        page.navigate("/login");
        username.fill(user.username());
        password.fill(user.password());
        signIn.click();
        return new DashboardPage(page);   // navigation methods return the next page
    }
}
```

Guidelines that keep page objects healthy:

- **Methods return the page you land on**, so calls chain into a readable story.
- **Keep assertions in tests.** Page objects expose `Locator`s (`flash()`, `rows()`, `balance`) and
  tests decide what to assert. (Small "is the page ready" waits inside page objects are fine.)
- **Shared chrome gets its own component** (for example, a `NavBar` used by every page).
- **Don't model every element.** Add methods when a test needs them.

> **Docs gotcha:** the POM docs page shows `import com.microsoft.playwright;`, which isn't valid Java.
> Use `import com.microsoft.playwright.*;` or import individual classes.

## Exercise

1. Look at `workshop/support/User.java` (provided): a record with `TELLER`, `AUDITOR` and `LOCKED`.
2. Study `workshop/pages/LoginPage.java`. It's a complete worked example of the pattern. Then complete
   the other starters in `workshop/pages/`, whose method signatures are already there:
   - `LoginPage`: `open()`, `submit(user, pass)`, `loginAs(User)` → `DashboardPage`, `error()`
   - `NavBar`: `customers()` → `CustomersPage`, `accounts()`, `signOut()`, `currentUser()`
   - `DashboardPage`: `nav`, `heading()`
   - `CustomersPage`: `open()`, `search(text)`, `newCustomer()` → `CustomerFormPage`, `rows()`,
     `row(text)`, `openCustomer(fullName)` → `CustomerDetailPage`, `emptyMessage()`
   - `CustomerFormPage`: `fill(first, last, email)`, `save()` → `CustomerDetailPage`,
     `saveExpectingErrors()` → itself, `errorSummary()`. Its fields are public, so tests can assert
     `assertThat(form.email).hasAccessibleDescription("…")`
   - `CustomerDetailPage`: `open(id)`, `heading()`, `status()`, `flash()`, `detail(term)`, `edit()`,
     `deleteAndConfirm()`, `nav()`
3. Complete `workshop/lesson07/CustomerPagesTest.java`:
   - Create a customer via the nav, search for them, open them, and check that Phone shows "Not provided"
   - Delete a customer, then confirm that a search finds nothing
   - Validation errors (empty first name, bad email)
   - Search for something that doesn't exist
4. **Refactor test.** Imagine the designers rename "Save customer" to "Save". How many files would
   you change now? How many before?

## Discussion

- `CustomerFormPage.save()` returns `CustomerDetailPage`, but what if the save fails validation?
  How does `saveExpectingErrors()` make the test clearer?
- Is a page object for the dashboard's "Recent activity" panel a page or a component?

## Checkpoint

- [ ] Your lesson 07 tests contain **no** `getByRole`, `getByLabel` or other locators. Those live only
      in `pages/`
- [ ] You could explain the "return the next page" pattern to a colleague

**Reference:** `solutions/pages/`, `solutions/lesson07/`

Next: [08: API testing](08-api-testing.md)
