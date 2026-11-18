# Playwright for Java: hands-on upskill

A self-contained training kit for learning end-to-end test automation with
[Playwright for Java](https://playwright.dev/java/docs/intro) and JUnit 5.

It has two parts:

1. **Ledgerline** (`app/`) is a realistic CRUD web app with a JSON API, used as the system under
   test. It manages customers, their accounts, and deposits, withdrawals and transfers, with real
   business rules, roles, validation, dialogs and asynchronous UI. All data is fictional, in memory,
   and re-seeded on start.
2. **A progressive course** (`lessons/`) of 15 lessons, then a banking track of missions (B0 so far). Each one builds on the last, from a first test
   to a parallel, CI-ready suite with page objects, API tests, network mocking and traces, then a
   "fix the broken suite" debugging exercise and a capstone. Every lesson has **starter files**, which
   start nearly complete and fade to nothing, plus a **reference solution** that's kept out of the
   default build.

Nothing needs to be installed except a JDK. The Maven Wrapper downloads Maven, and Playwright
downloads its own browsers.

## Tracks

Ledgerline has one back end and a front end per track. Each track has its own complete lesson series, and every
front end keeps the same [UI contract](docs/ui-contract.md), so the same skills apply to each.

| Track | Front end | Tests in | Lessons |
|-------|-----------|----------|--------|
| **Java** | Thymeleaf, served by the app on :8080 | Playwright for Java + JUnit 5 (`e2e-tests/`) | [lessons/](lessons/00-setup.md), below |
| **React** | React 19 + Vite on :5173 (`frontends/react/`) | Playwright Test + TypeScript (`e2e-ts/react/`) | [lessons/react/](lessons/react/README.md): 00–14 |
| **Angular** | Angular 22 on :4200 (`frontends/angular/`) | Playwright Test + TypeScript (`e2e-ts/angular/`) | [lessons/angular/](lessons/angular/README.md): 00–14 |

The rest of this README is about the Java track. The React and Angular tracks need Node 22, 24 or 26 as well as
the JDK: start at [lessons/react/00-setup.md](lessons/react/00-setup.md) or
[lessons/angular/00-setup.md](lessons/angular/00-setup.md).

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
./mvnw -pl e2e-tests test-compile exec:java -Dexec.args="install chromium"
./mvnw -pl e2e-tests test
```

> Paste the commands without trailing `# comments`. zsh (the macOS default shell) doesn't treat
> `#` as a comment in interactive sessions, so it would pass the comment to Maven as arguments.

Then start at **[lessons/00-setup.md](lessons/00-setup.md)**.

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## The course

| # | Lesson | You'll learn | Playwright docs |
|---|--------|--------------|-----------------|
| 00 | [Setup and tour](lessons/00-setup.md) | Running the app, installing browsers, exploring the product | [Intro](https://playwright.dev/java/docs/intro) |
| 01 | [Your first test](lessons/01-first-test.md) | `@UsePlaywright`, fixtures, `navigate`, first assertions, headed mode | [Writing tests](https://playwright.dev/java/docs/writing-tests) |
| 02 | [Actions and assertions](lessons/02-actions-and-assertions.md) | `fill`/`click`/`press`, web-first assertions, auto-waiting | [Actions](https://playwright.dev/java/docs/input) |
| 03 | [Locators](lessons/03-locators.md) | `getByRole`/`Label`/`Text`/`TestId`, strictness, chaining, filtering, tables | [Locators](https://playwright.dev/java/docs/locators) |
| 04 | [Options, hooks, isolation](lessons/04-options-hooks-isolation.md) | `OptionsFactory`, base URL, system properties, `@BeforeEach`, contexts | [JUnit](https://playwright.dev/java/docs/junit) |
| 05 | [Codegen](lessons/05-codegen.md) | Recording tests, then making them robust; unique test data | [Codegen](https://playwright.dev/java/docs/codegen) |
| 06 | [CRUD through the UI](lessons/06-crud-ui.md) | Full life cycle, validation, confirmation dialogs, `@Nested` | [Dialogs](https://playwright.dev/java/docs/dialogs) |
| 07 | [Page objects](lessons/07-page-objects.md) | Page Object Model, components, fluent navigation | [POM](https://playwright.dev/java/docs/pom) |
| 08 | [API testing](lessons/08-api-testing.md) | `APIRequestContext`, auth tokens, status codes, a typed API client | [API testing](https://playwright.dev/java/docs/api-testing) |
| 09 | [Hybrid tests](lessons/09-hybrid-tests.md) | Arrange with the API, act and assert in the UI; slow servers without sleeps | [Actionability](https://playwright.dev/java/docs/actionability) |
| 10 | [Auth state and roles](lessons/10-auth-state.md) | `storageState`, signing in once, read-only users, server-side checks | [Auth](https://playwright.dev/java/docs/auth) |
| 11 | [Network](lessons/11-network.md) | `route`/`fulfill`/`abort`/`fetch`, `waitForResponse`, loading states | [Mock APIs](https://playwright.dev/java/docs/mock) |
| 12 | [Debugging and scale](lessons/12-debugging-and-scale.md) | Trace viewer, screenshots, parallel runs, tags, CI | [Trace viewer](https://playwright.dev/java/docs/trace-viewer) |
| 13 | [Fix the broken suite](lessons/13-fix-the-broken-suite.md) | Diagnosing 10 realistic failures from errors and traces; no band-aids | [Debugging](https://playwright.dev/java/docs/debug) |
| 14 | [Capstone](lessons/14-capstone.md) | A full journey test, then independently owning a feature area | — |

**Banking track.** Missions that test what a bank cares about, each ending with the ledger check from B0.

| # | Mission | You'll learn | Docs |
|---|---------|--------------|------|
| B0 | [Ledger invariants](lessons/B0-ledger-invariants.md) | One reusable check that the books balance (`assertAll`), proven against faults planted in the app | [API testing](https://playwright.dev/java/docs/api-testing) |

Allow roughly **3–4 days** to work through the whole course, or about one lesson per session.

## Repository layout

```
app/                         Ledgerline: Spring Boot 4, Thymeleaf, H2 (the system under test)
e2e-tests/src/
  test/java/.../workshop/    YOUR code: starter files per lesson, plus pages/ and support/ (run by default)
  solutions/java/.../solutions/  reference answers, same layout (only built with -Psolutions)
  broken/java/.../broken/    lesson 13's deliberately broken suite (only built with -Pbroken)
lessons/                     the course (Java track)
lessons/answers/             answer notes (spoilers)
lessons/react/               the React track's lessons
lessons/angular/             the Angular track's lessons
frontends/react/             the React front end (with its own unit, integration and mutation tests)
frontends/angular/           the Angular front end (the same contract; its own unit, integration and mutation tests)
e2e-ts/                      Playwright Test (TypeScript) for the React and Angular tracks
  react/                     YOUR code for the React track: config and starter files
  angular/                   YOUR code for the Angular track: config and starter files
  solutions/                 reference answers: per track, and against every front end (the contract check)
  suites.ts                  the shared config behind solutions/ and broken/
  broken/                    lesson 13's deliberately broken suite (TypeScript edition)
docs/
  ledgerline-reference.md    users, seeded data, business rules, UI map, API
  ui-contract.md             what every front end keeps identical, so one set of tests fits all
  research/playwright-java.md  research notes on Playwright Java 1.64, with sources and docs caveats
  research/banking-upskill.md  the banking track's research and mission plan (B0 onwards)
```

## Common commands

| Task | Command |
|------|---------|
| Run the app | `./mvnw -pl app spring-boot:run` |
| App unit and API tests | `./mvnw -pl app test` |
| App mutation tests (slow) | `./mvnw -pl app -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage`, report in `app/target/pit-reports/` |
| Your workshop tests | `./mvnw -pl e2e-tests test` |
| One lesson's tests | `./mvnw -pl e2e-tests test -Dtest='**/workshop/lesson03/*'` |
| Watch the browser | `./mvnw -pl e2e-tests test -Dheadless=false` |
| Another environment | `./mvnw -pl e2e-tests test -DbaseUrl=http://host:port` |
| Reference suite (parallel) | `./mvnw -pl e2e-tests test -Psolutions` |
| Lesson 13 broken suite | `./mvnw -pl e2e-tests test -Pbroken` |
| Record a test | `./mvnw -pl e2e-tests exec:java -Dexec.args="codegen http://localhost:8080/login"` |
| Open a trace | `./mvnw -pl e2e-tests exec:java -Dexec.args="show-trace test-results/<dir>/trace.zip"` |

The e2e tests need the app running in another terminal.

On a fresh checkout, `./mvnw -pl e2e-tests test` is green: 2 tests pass (the setup check and lesson 01's
worked example), and every unfinished exercise is reported as *skipped* with a "TODO lesson NN" reason.
The reference suite has 77 tests and finishes in about 45 seconds against a running app. The broken suite
(`-Pbroken`) should fail 9 of its 11 tests on a freshly started app, and 10 on every run after that,
until you've fixed them.

React and Angular track and front-end commands (from the repo root, after `npm install`):

| Task | Command |
|------|---------|
| Run the React app | `npm run dev:react` (with the back end running) |
| Run the Angular app | `npm run dev:angular` (with the back end running) |
| Your React-track tests | `npm run test:react`, or `npm run test:react -- lesson03 --headed`, or `-- --ui` |
| Your Angular-track tests | `npm run test:angular` (the same options) |
| Reference answers on React | `npm run solutions:react` |
| Reference answers on Angular | `npm run solutions:angular` |
| Contract check: answers on every front end | `npm run contract` |
| Lesson 13 broken suite | `npm run broken:react`, `npm run broken:angular` |
| Front-end unit and integration tests | `npm run test:frontends` (React and Angular) |
| Front-end mutation tests (slow) | `npm run mutation:frontends`, reports in `frontends/react/reports/mutation/` and `frontends/angular/reports/mutation/` |

On a fresh checkout, `npm run test:react` and `npm run test:angular` are green: 2 tests pass in each (the setup
check and lesson 01's worked example) and every unfinished exercise is *skipped* with a "TODO lesson NN" reason.
Playwright starts the back end and that track's front end if they aren't running. The reference answers are 70
tests, plus 2 that sign in first (`npm run solutions:react`, `npm run solutions:angular`); `npm run contract` runs
them against React, Angular and Thymeleaf. Each track's broken suite fails 9 of its 11 tests (the TypeScript runs also list 2 passing sign-in set-up steps) against a freshly
started back end, and 10 on every later run against the same one, until you've fixed them.

## Versions

| Component | Version |
|-----------|---------|
| Playwright for Java | 1.64.0 |
| JUnit Jupiter | 5.14.4 (the line Playwright itself builds against) |
| Spring Boot | 4.1.2 |
| Java | 21+ |
| Maven (via wrapper) | 3.9.16 |
| Playwright Test (React and Angular tracks) | 1.65.1 |
| React / Vite / React Router | 19.3 / 8.3 / 8.5 |
| Angular / Angular CLI | 22.2 / 22.2, with TypeScript 6.0 (Angular 22 doesn't support TypeScript 7 yet) |
| Node | 22, 24 or 26 |

See [`docs/research/playwright-java.md`](docs/research/playwright-java.md) for known differences
between the online docs and 1.64.0. For example, the docs show `setContextOption`, but the method is
actually `setContextOptions`, and the docs describe 1.65 features that aren't released yet.
