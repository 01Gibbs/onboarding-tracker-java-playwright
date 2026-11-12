# Course effectiveness review: Playwright for Java upskill

Reviewed 2026-11-11. Scope: `README.md`, `lessons/00–13`, `e2e-tests/.../solutions/` (support, pages, and
the lesson 03, 06, 09, 10, 11 and 12 solutions read in full), `e2e-tests/pom.xml`, `app/` config and tests,
`docs/ledgerline-reference.md`, and `docs/research/playwright-java.md`.

Each claim cites a primary source. Statements marked **[Inference]** are my judgement, not a sourced finding.

---

## Verdict

**The Playwright *content* is strong: accurate, current (1.64), well sequenced, and almost entirely
aligned with Playwright's own guidance. As a *learning design* for a team, it is weaker than its content.**
It is a well-written tutorial with a full answer key in the same source tree. Practice consists of
"reproduce the reference test". There are no starter skeletons, no failing tests to make pass, no
retrieval checks, no spaced review, and almost no diagnose-and-fix practice. It also teaches no Java, even
though the team needs Java too. Engineers who already know Java will get real value from it. Engineers who
are new to Java, or who copy from `solutions/`, will finish with less durable skill than the course's
three-day time budget deserves. **[Inference]**

The test-level balance is defensible for a course *about* a browser tool. Its main weakness here is that
it never makes learners decide which level a test belongs at. In the reference suite, 12 of about 70 tests
are API-only and the rest drive a browser. The app's 4 unit tests are never used in the course.

---

## Strengths, with evidence

### Matches Playwright's recommended practice

| Course practice | Where | Playwright guidance |
|---|---|---|
| Locators ranked role → label → placeholder → text → test id → CSS "last resort"; explains strictness, chaining and filtering | `lessons/03-locators.md` | Locators page: role first, test ids are "not user facing", "CSS and XPath are not recommended as the DOM can often change" ([locators](https://playwright.dev/java/docs/locators)). "Use chaining and filtering" ([best practices](https://playwright.dev/docs/best-practices)) |
| "Use this / Not this" table: `assertThat(locator).isVisible()` rather than `assertTrue(locator.isVisible())`; checkpoint "no `Thread.sleep`" | `lessons/02` | "Use web-first assertions… Don't use manual assertions" ([best practices](https://playwright.dev/docs/best-practices)) |
| A new context per test, owning your own data, never touching seeded data, parallel-safe by construction | `lessons/04`, `06`, `09`, `12`; `TestData.java` | "Make tests as isolated as possible… each test… its own local storage, session storage, data, cookies" ([best practices](https://playwright.dev/docs/best-practices)). Contexts give isolation, so tests can run in any order or in parallel ([browser contexts](https://playwright.dev/java/docs/browser-contexts)) |
| Sign in once with `storageState`, treat the files as secrets, git-ignore them | `lessons/10`; `.gitignore` has `target/` and `playwright/.auth/` | "The browser state file may contain sensitive cookies… We strongly discourage checking them into private or public repositories" ([auth](https://playwright.dev/java/docs/auth)) |
| Codegen presented as a first draft, then refactored | `lessons/05` | "Generate locators" with codegen ([best practices](https://playwright.dev/docs/best-practices)) |
| Network mocking for states you can't produce for real | `lessons/11` | "Avoid testing third-party dependencies… use the Network API" ([best practices](https://playwright.dev/docs/best-practices)) |
| CI installs only Chromium (`--with-deps --only-shell`) and uploads traces on failure | `lessons/12` | "Optimize browser downloads on CI"; "Debugging on CI: use the trace viewer" ([best practices](https://playwright.dev/docs/best-practices)); `install --with-deps` ([Java CI](https://playwright.dev/java/docs/ci)) |
| Parallelism at class level, `same_thread` within a class | `lessons/12`, `pom.xml` `solutions` profile | Matches the configuration and the thread-safety note on the [JUnit page](https://playwright.dev/java/docs/junit) |

`docs/research/playwright-java.md` §0 lists real Java documentation errors (`setContextOption`, `RequestOptions` in
`route.fulfill`, `import com.microsoft.playwright;`) and pins to 1.64. The lessons repeat these as "Docs gotcha"
call-outs. This saves learners hours. Few official courses do this. **[Inference]**

### Sound testing-strategy instincts in places

- Lesson 08 says "Use the UI tests for things only the UI can show". Lesson 09 moves the *arrange* step to the
  API. Lesson 13 warns that journey tests should be "few". This matches Fowler and Vocke: "The more
  high-level you get the fewer tests you should have", and "Push your tests as far down the test pyramid as you
  can" ([Practical Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)).
- Lesson 10 checks the auditor's 403 on the server, not just the hidden button. That is a test at the
  right level for an authorisation rule.

### Learning-design elements already present

- **Worked examples before practice.** Every lesson has a "Concepts" section with complete code before the
  exercise. Studying worked examples beats unguided problem solving for novices
  ([Sweller & Cooper 1985](https://doi.org/10.1207/s1532690xci0201_3);
  [Sweller, van Merriënboer & Paas 1998](https://doi.org/10.1023/A:1022193728205)). Minimal guidance fails
  for novices ([Kirschner, Sweller & Clark 2006](https://doi.org/10.1207/s15326985ep4102_1)).
- **Self-explanation prompts.** The "Discussion" questions (for example, lesson 02: "why `hasText` and not
  `containsText`?") are self-explanation prompts. These improved transfer when combined with faded examples
  ([Atkinson, Renkl & Merrill 2003](https://doi.org/10.1037/0022-0663.95.4.774)).
- **Some productive failure.** Lesson 01 has you break the title and watch the 5-second retry. Lesson 05
  makes you run a recorded test twice so it fails. Lesson 12 has you open a trace of a broken test and make a
  test order-dependent. These are good, but there are too few of them (see Gap 2).
- **Increasing independence at the end.** Lesson 13 part 2 ("own a feature", write a test plan, peer review)
  is real, unscaffolded practice.

### Fit for a sensitive project

- Data is fictional by construction. Emails use the reserved `.test` TLD
  ([RFC 2606](https://www.rfc-editor.org/rfc/rfc2606)). Phone numbers are in the `07700 900xxx` range that
  Ofcom reserves for drama ([Ofcom](https://www.ofcom.org.uk/phones-and-broadband/phone-numbers/numbers-for-drama))
  (`DataSeeder.java`). The H2 database is in memory and re-seeded on start (`application.yml`).
- Runs fully on localhost. After the first Maven and browser download, no network access is needed (`lessons/00`).
- `TestingApi` is off unless enabled, and `application.yml` labels it "never enable in a real system".
- Lesson 12 correctly notes that trace.playwright.dev processes traces locally: "does not transmit any data
  externally" ([trace viewer](https://playwright.dev/java/docs/trace-viewer)).

---

## Gaps

Effort: **S** = under half a day; **M** = 1–3 days; **L** = more than 3 days for one author.

### Gap 1: Solutions are in the same compile tree, and exercises are "reproduce the answer"

**What happens.** `solutions/` sits beside `workshop/` in `src/test/java`, so it compiles with the learner's
code. IDE auto-import and "Go to class" will offer `dev.upskill.e2e.solutions.pages.LoginPage` while learners
write their own. **[Inference]** The README already warns about the `-Dtest=FirstTest` name clash. Every
exercise lists exact assertions, and the reference file contains exactly those assertions. Learners start
from an empty file every time: there are no starter skeletons and no failing or `@Disabled` tests to make pass.

**Why it matters.**
- Fading means moving from complete worked examples, to partially worked examples, to independent problems.
  Removing worked steps one at a time beats jumping from a full example to a blank page
  ([Renkl & Atkinson 2003](https://doi.org/10.1207/S15326985EP3801_3);
  [Atkinson, Renkl & Merrill 2003](https://doi.org/10.1037/0022-0663.95.4.774)). This course jumps: a full
  snippet in Concepts, then an empty file.
- The expertise-reversal effect: guidance that helps novices becomes redundant, or harmful, for more
  experienced learners ([Kalyuga, Ayres, Chandler & Sweller 2003](https://doi.org/10.1207/S15326985EP3801_4)).
  An answer key one click away gives everyone, at every level, the same maximum guidance. **[Inference]**

**Fix.**
1. Move `solutions/` out of `src/test/java` into `e2e-tests/src/solutions/java`. Add it as a test source
   **only** in the `solutions` profile, with `build-helper-maven-plugin` `add-test-source`. Alternatively,
   publish solutions on a `solutions` git branch or tag. Either way, they stop showing up in completion and
   in `-Dtest` matches.
2. Add `e2e-tests/src/test/java/dev/upskill/e2e/workshop/lessonNN/` starter files that fade:
   - Lessons 01–03: complete tests with one or two `// TODO` lines blanked (an assertion or a locator).
   - Lessons 04–07: test methods with names, arrange steps and `fail("TODO")` bodies. Learners write act and assert.
   - Lessons 08–11: `@Disabled("lesson 08 – remove when you start")` tests with only a name and a one-line
     Javadoc of the behaviour.
   - Lessons 12–13: nothing. This is already the case.
3. Change each lesson's "Checkpoint" from "N passing tests" to "the provided tests pass and you added one of
   your own that the brief didn't specify" (a near-transfer task).

**Effort:** M.

### Gap 2: Little practice at diagnosing and fixing broken or flaky tests

**What happens.** Debugging is one half of lesson 12. Learners get one "break it, then read the trace" task
and one "make it order-dependent" task. They never get a broken test they didn't write.

**Why it matters.**
- Flakiness is the most expensive E2E failure mode. At Google, about 16% of tests show some flakiness, and
  84% of pass-to-fail transitions involved a flaky test
  ([Micco 2016](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html)). 14% of large
  tests were flaky, against 0.5% of small ones. Java WebDriver tests were 10.45% flaky
  ([Listfield 2017](https://testing.googleblog.com/2017/04/where-do-our-flaky-tests-come-from.html)).
- Deliberate practice means tasks designed to target specific weaknesses, with immediate feedback and
  repetition ([Ericsson, Krampe & Tesch-Römer 1993](https://doi.org/10.1037/0033-295X.100.3.363)). Writing
  green tests to a spec doesn't target the hardest skill: diagnosing red ones. **[Inference]**

**Fix.** Add `lessons/12b-fix-the-suite.md` and `e2e-tests/src/test/java/dev/upskill/e2e/workshop/broken/`.
It should contain 8–10 short tests, each with **one** planted defect drawn from what this course teaches
against:
- `assertTrue(locator.isVisible())` straight after a click
- `Thread.sleep(500)` that passes locally and fails at `processing-delay-ms=2000` (run with that override)
- a CSS `nth-child` locator broken by a row created by another test
- a test that edits Alice Archer and breaks `LocatorsTest` when run in parallel
- `page.route` registered *after* `navigate` (a race)
- a missing `onceDialog`, so the delete silently does nothing
- `Locator.all()` called before the list has loaded. Playwright warns this "will produce unpredictable and
  flaky results" ([Locator.all](https://playwright.dev/java/docs/api/class-locator#locator-all))
- a shared `storageState` used by a test that signs out
- a `hasCount(1)` on a list other tests write to

Learners must reproduce each failure (for example with `-Dsurefire.rerunFailingTestsCount=0`, parallel on,
looped five times), diagnose it **from the trace alone**, and fix it. Provide a one-line hint per test inside
a `<details>` block.

**Effort:** M.

### Gap 3: No retrieval practice and no spacing

**What happens.** Lessons end with checklists ("You can explain…") and discussion questions, but nothing
requires recall without notes. No lesson revisits earlier material, except through the cumulative refactors
in lessons 04 and 10.

**Why it matters.**
- Testing yourself produced "substantially greater retention than studying" on tests 2 days and 1 week later,
  "even though repeated studying increased students' confidence"
  ([Roediger & Karpicke 2006](https://doi.org/10.1111/j.1467-9280.2006.01693.x)).
- Distributed practice beats massed practice across a large meta-analysis
  ([Cepeda et al. 2006](https://doi.org/10.1037/0033-2909.132.3.354)). Both practice testing and distributed
  practice are rated "high utility" ([Dunlosky et al. 2013](https://doi.org/10.1177/1529100612453266)).
- Microsoft Learn's own Playwright module ends with a graded "Module assessment"
  ([MS Learn: Build your first E2E test with Playwright](https://learn.microsoft.com/en-us/training/modules/build-with-playwright/)).

**Fix.**
- Open each lesson with **"Recall (5 min, no notes)"**: 3–5 questions, with at least two from lessons more than
  one session back. Put the answers in a collapsed `<details>` block. For example, lesson 09 might ask:
  "What does `onceDialog` default to if you don't register it?" (lesson 06) and "Why is `setBaseUrl` not
  enough for `APIRequestContext`?" (lesson 04).
- Add a 10-question end-of-day check after lessons 04, 09 and 13. Include "predict the output" items such as
  "this test fails with a strict mode violation: why?"
- The README says "about 3 days". Recommend a spread schedule instead, such as half-day sessions over 2 weeks.

**Effort:** S.

### Gap 4: The course never makes learners choose the test level, and E2E is the default

**What happens.** Counting `@Test` in the reference solutions: about 70 tests. 12 are API-only (lesson 08) and
the rest drive a browser. Validation and business rules are tested in the UI in lesson 06 (9 tests) and
lesson 09, and again via the API in lesson 08. `app/src/test/.../MoneyServiceTest.java` (4 Spring tests) is
never mentioned. Lesson 09 asks "Which of these tests would you also want at the API level only?", but nothing
acts on the answer.

**Why it matters.**
- "If a higher-level test spots an error and there's no lower-level test failing, you need to write a
  lower-level test." "Push your tests as far down the test pyramid as you can." E2E tests are "notoriously
  flaky", so "reduce the number of end-to-end tests to a bare minimum"
  ([Fowler/Vocke](https://martinfowler.com/articles/practical-test-pyramid.html)).
- Google suggests 70/20/10 unit/integration/E2E as "a good first guess". They warn against the "ice cream cone"
  (mostly E2E) and the "hourglass" (E2E where integration tests should be). An ideal feedback loop is fast,
  reliable and isolates failures, and E2E tests are weak on all three
  ([Wacker 2015](https://testing.googleblog.com/2015/04/just-say-no-to-more-end-to-end-tests.html)).
- For a team rolling onto a real project, the most expensive mistake is likely an ice-cream-cone suite, not
  bad locators. **[Inference]**

**Fix.** Add `lessons/09b-choosing-the-level.md`, placed after the hybrid lesson:
1. Give a table of 15 behaviours from `docs/ledgerline-reference.md` → "Business rules". Learners classify each
   as unit (`@SpringBootTest` service, or a plain JUnit test of `Money`), web-layer (`@WebMvcTest` +
   `MockMvc`), API (Playwright `APIRequestContext`), or UI. They justify each choice, then compare with a key.
2. Push one rule down: write the "max 2 decimals" rule as a unit or `MockMvc` test in `app/src/test/java`,
   then delete the duplicate UI test from their lesson 09 `MoveMoneyTest`. Time both.
3. Add a lesson 13 "Definition of done" item: "each test is at the lowest level that can catch the bug".

This doubles as Java practice (see Gap 5).

**Effort:** M.

### Gap 5: No Java-language upskilling

**What happens.** The course assumes Java fluency. Learners meet records (`User`), lambdas and method
references (`onceDialog`, `held::add`), `AutoCloseable` and try-with-resources (`LedgerApi`,
`BrowserContext`), `BigDecimal` scale semantics, `CopyOnWriteArrayList` and thread-safety, static imports,
`@Nested` inner classes, `var` and text blocks (ARIA snapshots need Java 15+, per research notes §0.6).
None of them is explained beyond a one-line comment, and there are no Java exercises.

**Why it matters.**
- Cognitive load theory: working memory is limited. Unfamiliar language syntax adds load that competes with
  the target skill ([Sweller, van Merriënboer & Paas 1998](https://doi.org/10.1023/A:1022193728205)).
  Someone fighting lambdas and `BigDecimal.equals` has little capacity left for locators. **[Inference
  applied to this case]**
- The brief says the team needs Java as well. A course that leaves this out misses half its goal.

**Fix.**
- Add a **"Java you'll need"** box to each lesson. Link each item to the official tutorial on
  [dev.java/learn](https://dev.java/learn/), with one 5-minute kata in a new `java-katas/` module (plain JUnit,
  no browser, runs offline):
  - 01: static imports, annotations
  - 02: varargs, `Pattern`
  - 04: interfaces (`OptionsFactory`), `System.getProperty`
  - 06: lambdas and functional interfaces (`Consumer<Dialog>`), inner and `@Nested` classes
  - 07: records, encapsulation, fluent return types
  - 08: `BigDecimal` (`equals` vs `compareTo`), `Map.of`, `AutoCloseable` and try-with-resources, Gson
  - 11: method references, concurrency basics (why `CopyOnWriteArrayList`, why route handlers must not
    block)
  - 12: `java.nio.file`
- Add a 15-minute diagnostic to lesson 00, so people who are fluent in Java can skip the katas. This responds
  to the expertise-reversal effect ([Kalyuga et al. 2003](https://doi.org/10.1207/S15326985EP3801_4)).
- Gap 4's `MockMvc` exercise also gives learners real Java and Spring practice on the kind of code they will
  meet on the project.

**Effort:** M for the boxes and the diagnostic; L if `java-katas/` covers every row.

### Gap 6: Missing Playwright capabilities that official material covers

**What happens.**
- **Broken link.** Lesson 13's "Where to go next" links
  [`/java/docs/test-snapshots`](https://playwright.dev/java/docs/test-snapshots), which returns **404**. Java
  has no `toHaveScreenshot`. The repo's own research notes say so (`docs/research/playwright-java.md` line
  ~810). Visual comparison exists only in the Node test runner
  ([test-snapshots](https://playwright.dev/docs/test-snapshots)).
- **ARIA snapshots** (`assertThat(locator).matchesAriaSnapshot(...)`) are supported in Java
  ([aria-snapshots](https://playwright.dev/java/docs/aria-snapshots)) and only appear under "where to go next".
  Ledgerline's labelled regions and tables suit them well.
- **Clock** ([clock](https://playwright.dev/java/docs/clock)) and **emulation**
  ([emulation](https://playwright.dev/java/docs/emulation)) aren't exercised. Ledgerline shows dates and
  "Recent activity", so `page.clock().setFixedTime(...)` has a natural target. Locale and time-zone emulation
  matter for `£` formatting. **[Inference]**
- **Cross-browser** is only a capstone idea. Playwright recommends "Test across all browsers"
  ([best practices](https://playwright.dev/docs/best-practices)).
- **Retries and flake reporting** aren't covered. Java has no Playwright-runner retries (the
  [JUnit page](https://playwright.dev/java/docs/junit) doesn't mention them). Surefire provides
  `rerunFailingTestsCount` and reports a test that passes on rerun as flaky
  ([Surefire](https://maven.apache.org/surefire/maven-surefire-plugin/examples/rerun-failing-tests.html)).
  Google's experience is that automatic retries "encourage developers to ignore flakiness"
  ([Micco 2016](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html)). That trade-off
  is worth teaching explicitly.
- **Soft assertions.** Playwright lists them as a productivity tip
  ([best practices](https://playwright.dev/docs/best-practices)). The JUnit equivalent is `assertAll`. Not covered.
- **Comparison with official material.** The Microsoft Learn module is introductory: one 8-unit beginner module
  in TypeScript and VS Code, ending in an assessment
  ([MS Learn](https://learn.microsoft.com/en-us/training/modules/build-with-playwright/)). This course goes
  much further on content. The official module has an assessment and VS Code tooling, and this course has
  neither. Playwright's demo apps such as [demo.playwright.dev/todomvc](https://demo.playwright.dev/todomvc)
  are simpler than Ledgerline. Ledgerline is the better practice target, because it has roles, dialogs, async
  states and an API. **[Inference]**

**Fix.**
- Fix the lesson 13 link. Point to the Node page and say "Node runner only". Suggest `matchesAriaSnapshot` as
  the Java alternative for structural regressions. **(S)**
- Add `lessons/11b-snapshots-clock-emulation.md`, with:
  1. an ARIA snapshot of the "Accounts" table on Alice Archer's page
  2. `page.clock()` to fix "now" and assert a date column or activity timestamp
  3. `Browser.NewContextOptions().setLocale("en-US").setTimezoneId(...)`, and a discussion of what changes
  4. one `assertAll` example (**M**)
- Add a `-Dbrowser=` system property to `LedgerlineOptions` (`Options.setBrowserName`), plus a lesson 12
  exercise to run the smoke suite on all three engines. **(S)**
- Add a lesson 12 subsection on `-Dsurefire.rerunFailingTestsCount=1`: how to read the "Flakes:" summary, and
  why you should quarantine and fix rather than retry forever. **(S)**

### Gap 7: Locator hygiene in the reference page objects is weaker than the lesson teaches

**What happens.** Lesson 03 calls CSS a "last resort", yet the reference page objects use
`page.locator("#amount-error")`, `"#" + fieldId + "-error"`, `".stat-value"`, `".details div"` and
`"#movement-form button[type=submit]"`. Lesson 06 also asserts `page.locator("#firstName-error")`.

**Why it matters.** "CSS and XPath are not recommended as the DOM can often change"
([locators](https://playwright.dev/java/docs/locators)). Learners copy the reference code, not the prose.
**[Inference]** The app already links errors with `aria-describedby` (`customers/form.html`,
`accounts/detail.html`). Java has `assertThat(field).hasAccessibleDescription(...)` (since v1.44)
([LocatorAssertions](https://playwright.dev/java/docs/api/class-locatorassertions)), which checks the
error *and* its accessibility wiring in one step.

**Fix.** In `solutions/pages/AccountPage.java`, `CustomerFormPage.java` and `lesson06/CustomerCrudTest.java`,
replace the error-id CSS with `assertThat(account.amount).hasAccessibleDescription("…")`. Keep one CSS
example, the `submit` button whose name changes to "Processing…", with a comment explaining why it is
justified. Add a lesson 03 discussion question: "Find the CSS locators in `pages/`. Which are justified?"

**Effort:** S.

### Gap 8: Sensitive-project hygiene is good for the toy app, but not taught as transferable practice

**What happens.**
- Lesson 10 warns that storage state is secret, which is good. But traces and screenshots are not described as
  sensitive, and lesson 12's CI snippet uploads all of `test-results/` with no retention limit.
- Credentials are literals in `User.java`. That's fine for fictional users, but the course never shows the
  pattern for real ones.
- Lesson 00 assumes direct access to Maven Central and the Playwright CDN.
- The solutions write auth state to `target/.auth`, while Playwright recommends `playwright/.auth`. Both are
  ignored here, so this is a consistency nit, not a leak.

**Why it matters.**
- Traces hold full network request and response bodies, DOM snapshots and console logs
  ([trace viewer](https://playwright.dev/java/docs/trace-viewer)). On a real system those can contain personal
  data and session tokens. **[Inference applied to their project]**
- Corporate networks often block the CDN. Playwright documents `HTTPS_PROXY`, `PLAYWRIGHT_DOWNLOAD_HOST` for an
  internal mirror, `PLAYWRIGHT_BROWSERS_PATH` and `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD`
  ([browsers](https://playwright.dev/java/docs/browsers)). The Java CI page recommends the pinned
  `mcr.microsoft.com/playwright/java:v1.64.0-noble` image ([CI](https://playwright.dev/java/docs/ci)).

**Fix.** Add a "Working on a sensitive system" section to `lessons/12-debugging-and-scale.md` (or a new
`docs/sensitive-projects.md`) covering:
1. Traces, screenshots and videos are sensitive artefacts. Use `RETAIN_ON_FAILURE` (already done), set
   `retention-days` on `upload-artifact`, and never upload traces from production-like data to public CI.
2. Read credentials from environment variables or a secret store in `LedgerlineOptions` and `User`. Show
   `System.getenv("LEDGERLINE_TELLER_PASSWORD")` with a fallback for the training app.
3. Installing behind a proxy or internal mirror (the variables above), and running from the pinned Docker image.
4. Never point the suite at an environment containing real customer data. Test data must be synthetic, using
   `.test` domains and Ofcom drama numbers, as in `DataSeeder`.

**Effort:** S.

### Smaller issues

- ~~**Assertions inside dialog handlers.**~~ **Checked 2026-11-11, not an issue.** A probe test with a
  deliberately wrong `assertEquals` inside `onceDialog` failed with `AssertionFailedError` raised from
  the `click()` call. The Java client runs event handlers on the calling test thread, so the lesson 06
  code stays as it is.
- **Test count mismatch.** The README says the reference suite has 69 tests. A `@Test` grep over
  `solutions/` gives 70. Minor; recount and fix.
- **Lesson 00 time.** Lesson 00 allows about 30 minutes, including first-time Maven and browser downloads. On a
  locked-down corporate laptop, that is optimistic. **[Inference]**

---

## Prioritised top 5 (impact ÷ effort)

| # | Recommendation | Gap | Effort | Why this rank |
|---|---|---|---|---|
| 1 | **Add a "fix the broken suite" lesson** (`lessons/12b`, `workshop/broken/`, 8–10 planted defects, diagnosed from traces) | 2 | M | Flakiness and diagnosis are where E2E suites really cost teams time ([Micco 2016](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html)). This is deliberate practice on the hardest skill ([Ericsson et al. 1993](https://doi.org/10.1037/0033-295X.100.3.363)) |
| 2 | **Move solutions out of the compile path, and add fading starter skeletons** (full → partial → `@Disabled` stubs) | 1 | M | Turns copy-the-answer into real problem solving, with evidence-based fading ([Renkl & Atkinson 2003](https://doi.org/10.1207/S15326985EP3801_3)) |
| 3 | **Add recall questions to every lesson, plus three cumulative checks** | 3 | S | Cheapest change with the strongest evidence ([Roediger & Karpicke 2006](https://doi.org/10.1111/j.1467-9280.2006.01693.x); [Dunlosky et al. 2013](https://doi.org/10.1177/1529100612453266)) |
| 4 | **Add a "choose the level" lesson** (classify rules; push one down into `app/` with `MockMvc` or unit tests; delete the duplicate UI test) | 4 | M | Prevents an ice-cream-cone suite on the real project ([Wacker 2015](https://testing.googleblog.com/2015/04/just-say-no-to-more-end-to-end-tests.html); [Fowler/Vocke](https://martinfowler.com/articles/practical-test-pyramid.html)). Also gives Java practice |
| 5 | **Add "Java you'll need" boxes and a diagnostic** in each lesson, with katas for records, lambdas, try-with-resources, `BigDecimal` and concurrency | 5 | M (L with full katas) | Meets the stated Java goal and lowers extraneous load ([Sweller et al. 1998](https://doi.org/10.1023/A:1022193728205)) |

**Quick wins to do alongside these (all S):**
- Fix the 404 visual-comparisons link in lesson 13 (Gap 6).
- Add `-Dbrowser=` cross-browser support and a Surefire rerun and flake-reporting note (Gap 6).
- Replace CSS error locators with `hasAccessibleDescription` (Gap 7).
- Add the sensitive-systems section: trace hygiene, credentials from env vars, proxy and mirror installs (Gap 8).
