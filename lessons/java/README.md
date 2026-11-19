# Playwright for Java: the Java track

End-to-end testing with [Playwright for Java](https://playwright.dev/java/docs/intro) and JUnit 5, against
Ledgerline's **Thymeleaf** front end (served by the app on :8080). This series stands on its own: start at 00 and
work through in order.

You write your tests in `e2e-java/src/test/java/dev/upskill/e2e/workshop/`. Each lesson has starter files there;
the reference answers are in `e2e-java/src/solutions/`, kept out of the default build. Try first, compare after.

| # | Lesson | You'll learn | Playwright docs |
|---|--------|--------------|-----------------|
| 00 | [Setup and tour](00-setup.md) | Running the app, installing browsers, exploring the product | [Intro](https://playwright.dev/java/docs/intro) |
| 01 | [Your first test](01-first-test.md) | `@UsePlaywright`, fixtures, `navigate`, first assertions, headed mode | [Writing tests](https://playwright.dev/java/docs/writing-tests) |
| 02 | [Actions and assertions](02-actions-and-assertions.md) | `fill`/`click`/`press`, web-first assertions, auto-waiting | [Actions](https://playwright.dev/java/docs/input) |
| 03 | [Locators](03-locators.md) | `getByRole`/`Label`/`Text`/`TestId`, strictness, chaining, filtering, tables | [Locators](https://playwright.dev/java/docs/locators) |
| 04 | [Options, hooks, isolation](04-options-hooks-isolation.md) | `OptionsFactory`, base URL, system properties, `@BeforeEach`, contexts | [JUnit](https://playwright.dev/java/docs/junit) |
| 05 | [Codegen](05-codegen.md) | Recording tests, then making them robust; unique test data | [Codegen](https://playwright.dev/java/docs/codegen) |
| 06 | [CRUD through the UI](06-crud-ui.md) | Full life cycle, validation, confirmation dialogs, `@Nested` | [Dialogs](https://playwright.dev/java/docs/dialogs) |
| 07 | [Page objects](07-page-objects.md) | Page Object Model, components, fluent navigation | [POM](https://playwright.dev/java/docs/pom) |
| 08 | [API testing](08-api-testing.md) | `APIRequestContext`, auth tokens, status codes, a typed API client | [API testing](https://playwright.dev/java/docs/api-testing) |
| 09 | [Hybrid tests](09-hybrid-tests.md) | Arrange with the API, act and assert in the UI; slow servers without sleeps | [Actionability](https://playwright.dev/java/docs/actionability) |
| 10 | [Auth state and roles](10-auth-state.md) | `storageState`, signing in once, read-only users, server-side checks | [Auth](https://playwright.dev/java/docs/auth) |
| 11 | [Network](11-network.md) | `route`/`fulfill`/`abort`/`fetch`, `waitForResponse`, loading states | [Mock APIs](https://playwright.dev/java/docs/mock) |
| 12 | [Debugging and scale](12-debugging-and-scale.md) | Trace viewer, screenshots, parallel runs, tags, CI | [Trace viewer](https://playwright.dev/java/docs/trace-viewer) |
| 13 | [Fix the broken suite](13-fix-the-broken-suite.md) | Diagnosing 10 realistic failures from errors and traces; no band-aids | [Debugging](https://playwright.dev/java/docs/debug) |
| 14 | [Capstone](14-capstone.md) | A full journey test, then independently owning a feature area | — |

**Banking track.** Missions that test what a bank cares about, each ending with the ledger check from B0.

| # | Mission | You'll learn | Docs |
|---|---------|--------------|------|
| B0 | [Ledger invariants](B0-ledger-invariants.md) | One reusable check that the books balance (`assertAll`), proven against faults planted in the app | [API testing](https://playwright.dev/java/docs/api-testing) |

Allow roughly **3–4 days** to work through the whole course, or about one lesson per session.

## Quick start

Requires **JDK 21+**.

You need **two terminals**, because the app keeps running in the first one.

**Terminal 1:** start the app and leave it running. Wait for `Started LedgerlineApplication`, then
open <http://localhost:8080> and sign in as `teller` / `teller-pass`.

```bash
./mvnw -pl app spring-boot:run
```

**Terminal 2:** install a browser (once), then run the setup check.

```bash
./mvnw -pl e2e-java test-compile exec:java -Dexec.args="install chromium"
./mvnw -pl e2e-java test
```

> Paste the commands without trailing `# comments`. zsh (the macOS default shell) doesn't treat
> `#` as a comment in interactive sessions, so it would pass the comment to Maven as arguments.

Then start at **[00: Setup and tour](00-setup.md)**.

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Commands

| Task | Command |
|------|---------|
| Run the app | `./mvnw -pl app spring-boot:run` |
| Your workshop tests | `./mvnw -pl e2e-java test` |
| One lesson's tests | `./mvnw -pl e2e-java test -Dtest='**/workshop/lesson03/*'` |
| Watch the browser | `./mvnw -pl e2e-java test -Dheadless=false` |
| Another environment | `./mvnw -pl e2e-java test -DbaseUrl=http://host:port` |
| Reference suite (parallel) | `./mvnw -pl e2e-java test -Psolutions` |
| Lesson 13 broken suite | `./mvnw -pl e2e-java test -Pbroken` |
| Record a test | `./mvnw -pl e2e-java exec:java -Dexec.args="codegen http://localhost:8080/login"` |
| Open a trace | `./mvnw -pl e2e-java exec:java -Dexec.args="show-trace e2e-java/test-results/<dir>/trace.zip"` |

The e2e tests need the app running in another terminal.

On a fresh checkout, `./mvnw -pl e2e-java test` is green: 2 tests pass (the setup check and lesson 01's
worked example), and every unfinished exercise is reported as *skipped* with a "TODO lesson NN" reason.
The reference suite has 77 tests and finishes in about 45 seconds against a running app. The broken suite
(`-Pbroken`) should fail 9 of its 11 tests on a freshly started app, and 10 on every run after that,
until you've fixed them.
