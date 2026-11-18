# Playwright for React: the React track

End-to-end testing with [Playwright Test](https://playwright.dev/docs/intro) and TypeScript, against
Ledgerline's **React** front end. This series stands on its own: start at 00 and work through in order.

You write your tests in `e2e-react/`. Each lesson has starter files there; the reference answers are in
`e2e-react/solutions/`. Try first, compare after.

| # | Lesson | You'll learn | Playwright docs |
|---|--------|--------------|-----------------|
| 00 | [Setup and tour](00-setup.md) | Running the back end and the React app, installing browsers, exploring the product | [Installation](https://playwright.dev/docs/intro) |
| 01 | [Your first test](01-first-test.md) | `test`, `expect`, the `page` fixture, `goto`, first assertions, headed and UI mode | [Writing tests](https://playwright.dev/docs/writing-tests) |
| 02 | [Actions and assertions](02-actions-and-assertions.md) | `fill`/`click`/`press`, web-first assertions, auto-waiting | [Actions](https://playwright.dev/docs/input) |
| 03 | [Locators](03-locators.md) | `getByRole`/`Label`/`Text`/`TestId`, strictness, chaining, filtering, tables | [Locators](https://playwright.dev/docs/locators) |
| 04 | [Configuration, hooks, isolation](04-config-hooks-isolation.md) | `playwright.config.ts`, `baseURL`, `webServer`, `beforeEach`, fresh browser contexts | [Test configuration](https://playwright.dev/docs/test-configuration) |
| 05 | [Codegen](05-codegen.md) | Recording tests, then making them robust; unique test data | [Codegen](https://playwright.dev/docs/codegen) |
| 06 | [CRUD through the UI](06-crud-ui.md) | The full life cycle, validation, confirm dialogs, `test.describe` | [Dialogs](https://playwright.dev/docs/dialogs) |
| 07 | [Page objects](07-page-objects.md) | The Page Object Model, shared components, returning the next page | [POM](https://playwright.dev/docs/pom) |
| 08 | [API testing](08-api-testing.md) | The `request` fixture, tokens, status codes, a typed API client | [API testing](https://playwright.dev/docs/api-testing) |
| 09 | [Hybrid tests](09-hybrid-tests.md) | Custom fixtures; arrange with the API, act and assert in the UI | [Fixtures](https://playwright.dev/docs/test-fixtures) |
| 10 | [Auth state and roles](10-auth-state.md) | Setup projects, `storageState`, read-only users, server-side checks | [Authentication](https://playwright.dev/docs/auth) |
| 11 | [Network](11-network.md) | `route`/`fulfill`/`abort`/`fetch`, `waitForResponse`, loading states | [Mock APIs](https://playwright.dev/docs/mock) |
| 12 | [Debugging and scale](12-debugging-and-scale.md) | Traces, UI mode, screenshots, parallel workers, tags, sharding, CI | [Trace viewer](https://playwright.dev/docs/trace-viewer) |
| 13 | [Fix the broken suite](13-fix-the-broken-suite.md) | Diagnosing 10 realistic failures from errors and traces; no band-aids | [Debugging](https://playwright.dev/docs/debug) |
| 14 | [Capstone](14-capstone.md) | A full journey test, then owning a feature area | — |

You need **Node 22, 24 or 26** and **JDK 21+** (the back end is a Spring Boot app). Allow roughly **3–4 days**, or
about one lesson per session.
