# Playwright upskill: hands-on end-to-end testing

A self-contained course in end-to-end test automation with [Playwright](https://playwright.dev), for full-stack
engineers on Java, React or Angular teams. Everyone tests the same app, **Ledgerline**: a realistic banking CRUD
app with real business rules, roles, validation, dialogs and asynchronous UI. All data is fictional, in memory,
and re-seeded on start.

## Pick your track

Each track is a complete course of 15 lessons, from a first test to a parallel, CI-ready suite with page objects,
API tests, network mocking and traces, then a "fix the broken suite" debugging exercise and a capstone. You only
need your own track's folders.

| Track | You test | You write tests in | Start here |
|-------|----------|--------------------|------------|
| **Java** | the Thymeleaf front end, served by the app on :8080 | `e2e-java/` (Playwright for Java + JUnit 5) | [lessons/java/](lessons/java/README.md) |
| **React** | the React front end on :5173 (`frontends/react/`) | `e2e-react/` (Playwright Test + TypeScript) | [lessons/react/](lessons/react/README.md) |
| **Angular** | the Angular front end on :4200 (`frontends/angular/`) | `e2e-angular/` (Playwright Test + TypeScript) | [lessons/angular/](lessons/angular/README.md) |

Every track needs **JDK 21+** (the back end is a Spring Boot app; the Maven Wrapper downloads Maven). React and
Angular also need **Node 22, 24 or 26**. Playwright downloads its own browsers. Allow roughly **3–4 days** for a
track, or about one lesson per session.

## Repository layout

```
app/                Ledgerline's back end (Spring Boot, JSON API), and the Thymeleaf front end
frontends/react/    the React front end, over the same JSON API
frontends/angular/  the Angular front end, over the same JSON API

lessons/java/       the Java track's lessons            e2e-java/     its tests (Maven module)
lessons/react/      the React track's lessons           e2e-react/    its tests (Playwright Test project)
lessons/angular/    the Angular track's lessons         e2e-angular/  its tests (Playwright Test project)

docs/               Ledgerline reference, the UI contract, and research notes
```

Inside each `e2e-*` folder: **your** starter files (one folder per lesson, plus `pages/` and `support/`),
`solutions/` with the reference answers, and `broken/` with lesson 13's deliberately broken suite.

All three front ends keep one [UI contract](docs/ui-contract.md) (roles, accessible names, test ids and URLs), so
the same skills, and the same reference answers, work against each. See
[docs/ledgerline-reference.md](docs/ledgerline-reference.md) for users, seeded data, business rules and the API.

## Maintainer commands

Learner commands are in each track's lessons. These check the course itself:

| Task | Command |
|------|---------|
| Back end unit and API tests | `./mvnw -pl app test` |
| Back end mutation tests (slow) | `./mvnw -pl app -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage`, report in `app/target/pit-reports/` |
| Java reference answers (77 tests) and broken suite | `./mvnw -pl e2e-java test -Psolutions`, `./mvnw -pl e2e-java test -Pbroken` |
| React / Angular reference answers | `npm run solutions:react`, `npm run solutions:angular` |
| Contract check: both TypeScript tracks' answers on every front end | `npm run contract` |
| Type-check both TypeScript tracks | `npm run typecheck` |
| Front-end unit and integration tests | `npm run test:frontends` |
| Front-end mutation tests (slow) | `npm run mutation:frontends`, reports in `frontends/*/reports/mutation/` |

Run the npm commands from the repo root after `npm install`. Each track's broken suite fails 9 of its 11 tests
against a freshly started back end, and 10 on every later run against the same one (the TypeScript runs also
list 2 passing sign-in set-up steps).

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
