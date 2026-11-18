# Playwright for React: the React track

End-to-end testing with [Playwright Test](https://playwright.dev/docs/intro) and TypeScript, against
Ledgerline's **React** front end. This series stands on its own: start at 00 and work through in order.

You write your tests in `e2e-ts/react/`. Each lesson has starter files there; the reference answers are in
`e2e-ts/solutions/`. Try first, compare after.

| # | Lesson | You'll learn | Playwright docs |
|---|--------|--------------|-----------------|
| 00 | [Setup and tour](00-setup.md) | Running the back end and the React app, installing browsers, exploring the product | [Installation](https://playwright.dev/docs/intro) |
| 01 | [Your first test](01-first-test.md) | `test`, `expect`, the `page` fixture, `goto`, first assertions, headed and UI mode | [Writing tests](https://playwright.dev/docs/writing-tests) |
| 02 | [Actions and assertions](02-actions-and-assertions.md) | `fill`/`click`/`press`, web-first assertions, auto-waiting | [Actions](https://playwright.dev/docs/input) |
| 03 | [Locators](03-locators.md) | `getByRole`/`Label`/`Text`/`TestId`, strictness, chaining, filtering, tables | [Locators](https://playwright.dev/docs/locators) |
| 04 | [Configuration, hooks, isolation](04-config-hooks-isolation.md) | `playwright.config.ts`, `baseURL`, `webServer`, `beforeEach`, fresh browser contexts | [Test configuration](https://playwright.dev/docs/test-configuration) |
| 05–09 | *Being written* | Codegen, CRUD, page objects, API testing, hybrid tests | |
| 10 | [Auth state and roles](10-auth-state.md) | Setup projects, `storageState`, read-only users, server-side checks | [Authentication](https://playwright.dev/docs/auth) |
| 11–14 | *Being written* | Network mocking, debugging and scale, a broken suite to fix, a capstone | |

You need **Node 22, 24 or 26** and **JDK 21+** (the back end is a Spring Boot app). Allow about one lesson per session.
