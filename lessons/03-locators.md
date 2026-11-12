# Lesson 03: Locators

**Goal:** find elements robustly, the way a user would, and work with lists and tables.

**Time:** about 1.5 hours

**Docs:** [Locators](https://playwright.dev/java/docs/locators) · [Other locators](https://playwright.dev/java/docs/other-locators)

## Concepts

### Which locator, in order of preference

| Locator | Example | Use when |
|---------|---------|----------|
| `getByRole` | `getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search"))` | Almost always. It matches what users and assistive tech see |
| `getByLabel` | `getByLabel("Email")` | Form fields |
| `getByPlaceholder` | `getByPlaceholder("Name or email")` | Inputs without a label (rare) |
| `getByText` | `getByText("No customers match your search.")` | Non-interactive text |
| `getByTestId` | `getByTestId("account-balance")` | When there's no good user-facing handle |
| `locator("css")` | `locator("#movement-form")` | Last resort. Ask whether the page is missing an accessible name instead |

Text matching is **case-insensitive and substring** by default. Use `.setExact(true)` for an exact match.

### Strictness

Actions such as `click()` and `fill()` require the locator to match **exactly one** element. If it
matches more, Playwright throws a *strict mode violation* that lists the matches. That's a feature:
it stops you clicking the wrong thing. Fix it by narrowing the locator.

### Narrowing: chaining and filtering

```java
Locator mainNav = page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Main"));
mainNav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customers")).click();

Locator table = page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName("Customers"));
Locator row = table.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText("Dan Evans"));
assertThat(row.getByTestId("status-badge")).hasText("Suspended");
```

`filter` also accepts `setHas(otherLocator)`, `setHasNot(...)` and `setHasNotText(...)`.
Lists: `first()`, `last()`, `nth(i)`, `all()`, `count()`, and `assertThat(list).hasCount(n)`.

> Locators are **lazy**. Creating one does nothing. It's looked up fresh every time you act on it or
> assert on it, so you can define one early and reuse it after the page changes.

### Finding a locator interactively

Run a test headed with `page.pause()` in it. The **Playwright Inspector** opens, and its "Pick
locator" button suggests a locator for whatever you click. (Lesson 05 covers this tool in depth.)

## Exercise

Open the starter `workshop/lesson03/LocatorsTest.java`. The `@BeforeEach` sign-in is provided, and each
test is just a name. Its goal is below.

1. **Navigation.** Click "Customers" *inside the Main navigation*, and assert that the "Customers"
   heading is visible. Then assert that the nav link has `aria-current="page"`.
   *Try `page.getByRole(LINK, name "Customers").click()` from the dashboard first, and read the
   error. What else matches?*
2. **Search.** On `/customers`, fill the field with the placeholder "Name or email" with `archer` and
   click "Search". Assert that exactly 1 `customer-row` is shown, that it contains "Alice Archer", and
   that `result-count` says "1 customer".
3. **Table rows.** On `/customers`, find Dan Evans's row **inside the "Customers" table**. Assert
   that its status badge says "Suspended" and that its first cell says "CUS-0004".
4. **Text.** Assert that `getByText("alice.archer@")` is visible (substring match), and that
   `getByText("Erin Foster")` with `setExact(true)` is visible.
5. **Every row.** On `/accounts`, filter Status = Frozen. Assert that the "Holiday Fund" row is visible,
   and that **every** row's status badge says "Frozen". Hint: loop over `.all()`.
6. **`has` filter.** From Alice Archer's customer page, click the account link in the row of the
   "Accounts" table that **has** a cell with exactly the text "Savings". Assert that the h1 contains
   "Rainy Day" and that the account number is `10000002`.

## Discussion

- Why does `getByRole` beat a CSS selector like `.topbar nav ul li:nth-child(2) a`?
- In exercise 5, why not `assertThat(rows).hasCount(1)`? (Hint: other tests may freeze accounts too.)

## Checkpoint

- [ ] 6 passing tests in `workshop/lesson03`
- [ ] You triggered and fixed at least one strict mode violation
- [ ] You used the Inspector's "Pick locator" at least once

**Reference:** `solutions/lesson03/LocatorsTest.java`

Next: [04: Options, hooks and test isolation](04-options-hooks-isolation.md)
