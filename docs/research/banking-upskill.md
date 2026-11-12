# Banking-specific upskill: what this course is missing, and a better shape for it

- **Date researched:** 2026-11-11
- **Question:** is there a better way to upskill engineers in Playwright for Java for a banking-type project
  than this repo's course, and what should change?
- **Builds on:** [`playwright-java.md`](playwright-java.md) (API facts and doc errata for 1.64) and
  [`course-effectiveness.md`](course-effectiveness.md) (learning design, test levels, flakiness, sensitive-project
  hygiene). This note doesn't repeat them. It covers the gap they leave: **banking-domain depth**.
- **How the API claims were checked.** Every Playwright method named below was checked against the
  `com.microsoft.playwright:playwright:1.64.0` jar this repo builds with (`javap` on the resolved test
  classpath), as well as against playwright.dev/java. "Verified in 1.64" means the method signature is in that jar.
- **How the app claims were checked.** Ledgerline was started locally (port 8091) and probed with `curl`. Results
  are marked **[Probed]**. Statements marked **[Inference]** are my own judgement, not a sourced finding.

---

## 1. TL;DR verdict

**Yes, there is a better way, but it's an addition, not a replacement.** Lessons 00–13 teach the Playwright
mechanics well, and the earlier review explains why. What they don't teach is the set of failure modes
that make banking software different: money that's wrong by a penny, a payment applied twice, a transfer
that races another, a cut-off that moves a payment to the next business day, a session that must end after
5 minutes idle, an approval the maker can grant themselves, an SCA code that still works after the amount
changes, and test evidence that leaks customer data. A learner can finish this course without writing a
single test of any of these. **[Inference]**

**The better shape:** keep a shortened core (lessons 00–12), then replace the generic capstone with a
**banking track of 8 domain "missions"**. Each mission adds one small, flag-guarded feature to Ledgerline
(maker-checker, OTP with dynamic linking, cut-off times, idempotent payments, a session-timeout warning,
and so on). The learner then tests it at the right levels. These missions use Playwright Java APIs the
course never touches, all present in 1.64: `Clock`, `Credentials` (virtual WebAuthn), multi-context
multi-actor tests, screenshot `setMask`, `Tracing.group`, and axe-core via Deque's Java package. Every
assertion of a money movement ends with an **API-level ledger-invariant check**.

Four findings from probing the current app give the track real bugs and gaps to work on:

1. `POST /api/accounts/3/deposits {"amount":1E+2}` is **accepted** and echoed back as `"amount":1E+2`.
   The 2-decimal rule checks `scale() > 2`, and `1E+2` has a scale of −2. **[Probed]**
2. Two identical deposit POSTs, even with an `Idempotency-Key` header, create **two** ledger entries. There is no
   idempotency support. **[Probed]**
3. A form `POST /accounts/3/movements` with the session cookie and `Origin: https://evil.example` **succeeds**.
   There is no CSRF token or Origin check, and the `JSESSIONID` cookie has no `SameSite` attribute. **[Probed]**
4. Concurrency is **correct**. 10 parallel £200 withdrawals from £1,250 gave exactly 6×201 and 4×422, with a
   final balance of £50. 40 parallel £1 deposits gave a balance of £40 from 40 entries. 20 pairs of crossing
   transfers left both balances unchanged. That makes concurrency a good "prove it" exercise, and a good
   candidate for a deliberately broken toggle. **[Probed]**

---

## 2. Gap analysis: current course vs banking needs

| Banking need | Current course (lesson) | Gap | Severity |
|---|---|---|---|
| Money correctness: decimals, rounding, scale | L06/L09 test "max 2 decimals" via UI; L08 says use `BigDecimal` not `double` | No boundary table; no scale/exponent cases (the `1E+2` bug above); no `BigDecimal.equals` vs `compareTo` drill | High |
| Ledger invariants (balance = Σ entries; transfer legs net to zero) | L14 checks one balance and an entry count via API | Not a reusable check; not run after every money test | High |
| Idempotency and double submit | None | App has no idempotency; UI relies on JS disabling the button | High |
| Concurrency and race conditions | L12 makes a test order-dependent (test-suite race, not app race) | No test fires concurrent payments at the app | Medium |
| Time: cut-offs, business days, statement dates | L14 "where next" links Clock | No exercise; the app has no time-dependent behaviour, and uses `Instant.now()` server-side | High |
| Session timeout (SCA RTS 5 min; WCAG 2.2.1 warning) | None | No timeout feature to test | High |
| Step-up auth / SCA / OTP, dynamic linking | None | No feature; no Playwright `Credentials` (passkeys) coverage | High |
| Maker-checker / four-eyes | L10 teller vs auditor (read-only) | No two-actor approval flow; no multi-context test | High |
| Authorisation bypass at API level | L10: auditor POST → 403 | Only one bypass; no object-level (IDOR) cases; no self-approval | Medium |
| CSRF / cookie attributes | None | App has no CSRF defence (probed) | Medium |
| storageState hygiene | L10 "treat as secrets", git-ignored | Not covered: per-role files, expiry, and that `setCredentials`/`setIndexedDB` widen what's stored | Low |
| Accessibility (EAA, WCAG 2.2 AA) | L06 `aria-describedby`; L14 links ARIA snapshots | No axe scan; no WCAG 3.3.4 confirm step; no timeout-warning a11y | High (EU) |
| Locale/timezone | None | App hard-codes `Locale.UK`, `en-GB` and `ZoneId.systemDefault()`, so emulation does nothing | Medium |
| Evidence and PII in artefacts | L12 traces/screenshots; course-effectiveness Gap 8 (retention, env-var creds) | No masking (`setMask`), no `Tracing.group` evidence structure, no HAR content policy | Medium |
| Visual regression | L14 note: Java has no screenshot assertion | Correct; just needs a stated alternative | Low |
| Parallelism with shared ledger state | L12 class-level parallelism; L14 asks "how do you assert totals in parallel?" | No `@ResourceLock`/`@Isolated` answer | Medium |
| Pedagogy | Linear tool-centric lessons; broken suite (L13) | No domain-driven, feature-plus-test missions | — |

---

## 3. Recommendations (prioritised)

Ranked by (risk the skill prevents on a banking project) ÷ (effort). Effort: **S** < ½ day, **M** 1–3 days,
**L** > 3 days for one author.

### R1. A money-correctness module built on ledger invariants (High, M)

**What to teach**

1. **An `InvariantCheck` support class.** Learners call it at the end of every money test, through the API.
   It checks that, for each account touched:
   - `balance == Σ signedAmount(entries)`
   - each entry's `balanceAfter` chains from the previous one
   - every `transferId` has exactly two legs, `TRANSFER_OUT` and `TRANSFER_IN`, with equal amounts.

   This works on Ledgerline today. The seed data is created through `MoneyService.deposit/withdraw/transfer`,
   so even seeded accounts satisfy it (`DataSeeder.java` lines 76–85). It is the "act in the UI, verify via
   the API" pattern from the [API testing guide](https://playwright.dev/java/docs/api-testing), raised from one
   assertion to an invariant. **[Inference]**
2. **Boundary tables** with JUnit `@ParameterizedTest` and `@CsvSource`, run at the **API level**. Use the
   UI for one representative case only (course-effectiveness Gap 4 already argues for pushing tests down).
   Cases: `0`, `-0`, `0.01`, `0.005`, `12.345` (as a string), `1E+2`, `1000000.00`, `1000000.01`, `"abc"`.
   The probe results were: `0.005` → 422, `-0` → 422, `1000000.01` → 422, `"abc"` → 400, but **`1E+2` → 201 with
   `"amount":1E+2` in the response**. **[Probed]** This makes an excellent "find the bug" exercise. It also
   re-teaches lesson 08's warning, because `new BigDecimal("1E+2").equals(new BigDecimal("100.00"))` is false.
3. **Concurrency.** Fire N payments at once and assert the invariant, plus "accepted count × amount ≤
   opening balance".
   - **Caution:** Playwright objects are not thread-safe ("not safe to use same Playwright objects from
     multiple threads without extra synchronization", [JUnit page](https://playwright.dev/java/docs/junit)).
     The burst must use one `Playwright`/`APIRequestContext` per thread, or plain `java.net.http.HttpClient`.
   - Today the app passes these checks **[Probed]**. To make the lesson bite, add a training toggle
     `ledgerline.faults.lost-update=true` that reads the balance, sleeps, then writes it back. Learners must
     catch the lost update.
4. **Double submit.**
   - **UI:** `dblclick()` on "Submit", or press Enter twice, then assert exactly one new `transaction-row`
     and the invariant. The form uses POST-redirect-GET (`302` to `/accounts/{id}`, **[Probed]**), so also test
     that `page.reload()` after submitting doesn't post again.
   - **API:** send the same payment twice with the same `Idempotency-Key` and expect one entry. This needs the
     app change below.

**Standards that motivate it**

- OWASP ASVS 5.0 **2.3.3** (L2): transactions at the business-logic level, so an operation succeeds or rolls
  back as a whole.
- ASVS 5.0 **2.3.4** (L2): locking so limited resources "cannot be double-booked by manipulating the
  application's logic".
- ASVS 5.0 **2.4.2** (L3): "realistic human timing, preventing excessively rapid transaction submissions"
  ([ASVS V2](https://github.com/OWASP/ASVS/blob/master/5.0/en/0x11-V2-Validation-and-Business-Logic.md)).
- WSTG **4.10.4** "Test for Process Timing" and **4.10.5** "Test Number of Times a Function Can Be Used Limits"
  ([WSTG business logic](https://wstg.owasp.org/v4.2/4-Web_Application_Security_Testing/10-Business_Logic_Testing/)).

**Playwright Java API (verified in 1.64):** `APIRequestContext.get/post`, `RequestOptions.setData` and
`setFailOnStatusCode` (v1.18), `Locator.dblclick`, `Page.reload`, `assertThat(locator).hasCount`.

- **Don't put `RequestOptions.setMaxRetries` on payment POSTs.** It retries "network errors" (v1.46,
  [RequestOptions](https://playwright.dev/java/docs/api/class-requestoptions)). Without server idempotency, a
  retry after a reset that the server had already processed would double-pay. **[Inference]**

**App changes**

| Change | Why |
|---|---|
| Reject non-canonical amounts: `stripTrailingZeros().scale() > 2` **and** `scale() < 0` normalised, or serialise with `setScale(2)` | Fixes the `1E+2` acceptance and echo. Keep the current bug behind `ledgerline.faults.exponent-amounts` for the exercise |
| `Idempotency-Key` header on `/api/accounts/{id}/deposits`, `/withdrawals`, `/api/transfers` (store key → response) | Enables the idempotency exercise |
| `ledgerline.faults.lost-update` toggle (non-atomic balance update) | Gives the concurrency test something to catch |

### R2. Time control: browser clock plus a server clock (High, M)

**Key fact.** Playwright's Clock controls time **inside the page**. It overrides `Date`, `setTimeout`,
`setInterval`, `requestAnimationFrame`, `performance` and similar
([Clock guide](https://playwright.dev/java/docs/clock)). Ledgerline computes and renders almost every time
**on the server**: `LedgerEntry.createdAt = Instant.now()`, and `Dates` formats with `ZoneId.systemDefault()`. So
`page.clock()` alone changes nothing a learner can see. That makes it the most important thing to teach about
time in a server-rendered banking app: **you need both clocks**. **[Inference from the source]**

**Playwright Java API (verified in 1.64).** `Page.clock()` and `BrowserContext.clock()` return `Clock`, with
`install(Clock.InstallOptions)`, `setFixedTime(long|String|Date)`, `setSystemTime(...)`, `pauseAt(...)`,
`runFor(long|String)`, `fastForward(long|String)` and `resume()`. All were added in v1.45
([Clock API](https://playwright.dev/java/docs/api/class-clock)). The guide says "If you call `install` at any
point in your test, the call _MUST_ occur before any other clock related calls". It recommends
`setFixedTime` for the simple case. `Browser.NewContextOptions.setLocale` and `setTimezoneId` are present
([emulation](https://playwright.dev/java/docs/emulation)).

**Banking use cases and how to test each**

| Use case | Where time lives | Test technique |
|---|---|---|
| Payment cut-off (e.g. 15:30 London → next business day) | Server | Set the server clock via a test endpoint; assert the "Value date" in the UI **and** via API |
| Weekends and bank holidays roll forward | Server | `@ParameterizedTest` over a date table; API level |
| Scheduled / standing payment executes on its date | Server job | Advance the server clock, trigger the job (test endpoint), assert entries and invariant |
| Idle-session warning dialog, then auto sign-out | Browser JS timer **and** server session | `page.clock().install()` before `navigate`, then `fastForward("04:40")` and assert the warning; `runFor` past the deadline and assert the redirect to `/login`. The server side is tested separately (R3) |
| "Statement for October" boundaries in a non-UK time zone | Server zone, browser zone | Server: a fixed `Clock` in `Europe/London`. Browser: `setTimezoneId` only matters if the page formats dates in JS |

**Why a server clock.** `java.time.Clock` exists so that "alternate clocks [can] be plugged in… This can
simplify testing". `Clock.fixed` and `Clock.offset` are provided for that purpose, the latter to "simulate
running in the future or in the past" ([Java SE 21 `Clock`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html)).

**Locale caveat to teach.** `Money` formats with `Locale.UK`, and the dashboard JS uses
`toLocaleString('en-GB', …)`. `setLocale("de-DE")` therefore changes nothing, which is correct for a
GBP ledger. The lesson is to **assert that it doesn't change**: a server-rendered amount must not depend on
`Accept-Language`. If the bank serves EU customers, add a JS-formatted view and assert `1.250,00 €` vs
`£1,250.00`. **[Inference]**

**App changes**

| Change | Why |
|---|---|
| Inject a `java.time.Clock` bean everywhere `Instant.now()` is used (`LedgerEntry`, `Account.openedAt`) | Makes server time controllable |
| `POST /api/testing/clock {"instant": "...", "zone": "Europe/London"}` and `DELETE` to reset (TestingApi, test-support flag only) | Lets tests set server time |
| Cut-off rule and a `valueDate` on entries, plus a static bank-holiday list | Gives a time-dependent business rule |
| Scheduled payments (`/api/scheduled-payments`) plus a `POST /api/testing/run-scheduler` trigger | Scheduled-payment mission |
| A JS idle-timer that shows a `role="alertdialog"` warning with "Stay signed in" | Clock mission, and WCAG 2.2.1 (R4) |

### R3. Authentication and authorisation for money movement (High, L)

**3a. Maker-checker (four-eyes).** OWASP ASVS 5.0 **2.3.5** (L3): "Verify that high-value business logic flows
require multi-user approval to prevent unauthorized or accidental actions"
([ASVS V2](https://github.com/OWASP/ASVS/blob/master/5.0/en/0x11-V2-Validation-and-Business-Logic.md)).

- **Test pattern:** two `BrowserContext`s in one test, each with its own role's storage state:
  `browser.newContext(new Browser.NewContextOptions().setStorageStatePath(...))`.
  - Maker submits £15,000.
  - Checker sees it in "Pending approvals" and approves.
  - Assert the balance changes only after approval, then run the invariant.
- **Negative cases at the API level:** the maker approves their own payment (403); an auditor approves
  (403); approval after the amount was edited (409); approval of an already-rejected item (409).
- **Playwright Java API (verified in 1.64):** `Browser.newContext`, `NewContextOptions.setStorageStatePath`,
  and the injected `Browser` fixture ([JUnit](https://playwright.dev/java/docs/junit)). The auth guide doesn't
  show a multi-role example ([auth](https://playwright.dev/java/docs/auth)), so the course must provide one.

**3b. Step-up / SCA with dynamic linking.** The EU SCA RTS, Delegated Regulation (EU) 2018/389:

- **Article 5(1)(a)–(d).** The payer "is made aware of the amount… and of the payee". The code "is specific to
  the amount… and the payee". The accepted code "corresponds to the original specific amount". And "any
  change to the amount or the payee results in the invalidation of the authentication code"
  ([Art. 5](https://www.legislation.gov.uk/eur/2018/389/article/5)).
- **Article 4(3)(b).** Failed attempts "shall not exceed five within a given period of time"
  ([Art. 4](https://www.legislation.gov.uk/eur/2018/389/article/4)).
- **Article 13.** SCA is required "where a payer creates or amends a list of trusted beneficiaries"
  ([Art. 13](https://www.legislation.gov.uk/eur/2018/389/article/13)).
- **Article 16.** The low-value exemption is ≤ EUR 30 per payment, and cumulatively ≤ EUR 100 or five
  payments since the last SCA ([Art. 16](https://www.legislation.gov.uk/eur/2018/389/article/16)).

Each of these turns into a concrete test:

| RTS rule | Test |
|---|---|
| Art. 5(1)(a) | The OTP screen shows amount and payee. Assert this with `hasText`, or with an ARIA snapshot of the confirmation region |
| Art. 5(1)(d) | Get an OTP for £100 to A, change the amount to £101 via the API or by tampering with the form, submit the old OTP, and expect rejection |
| Art. 4(3)(b) | Five wrong OTPs, then a lock. Assert that the sixth attempt is refused even with the right code |
| Art. 16 | A table of cumulative amounts or counts decides whether SCA is triggered (API-level parameterised test) |
| Art. 13 | Adding a payee triggers SCA; paying a trusted payee doesn't |

- **How Playwright gets the OTP:** never by scraping a real SMS. Read it from a test-only "OTP outbox"
  endpoint. For passkeys, use the virtual authenticator: `BrowserContext.credentials()` returns
  `Credentials` with `install()`, `create(rpId[, CreateOptions])`, `get()` and `delete(id)`. It is "a virtual
  WebAuthn authenticator scoped to a BrowserContext", and the methods were added in v1.61
  ([Credentials](https://playwright.dev/java/docs/api/class-credentials)). `install()` must be called "before the
  page first touches `navigator.credentials`".
- **storageState and passkeys:** `BrowserContext.StorageStateOptions.setCredentials(boolean)` exists in 1.64,
  alongside `setIndexedDB` and `setOpfs`. The auth guide notes that storage state can hold "virtual WebAuthn
  credentials". Each of these makes the state file **more** sensitive than lesson 10 implies
  ([auth](https://playwright.dev/java/docs/auth)).
- **UK note:** the FCA made its own UK SCA-RTS after EU exit ([FCA PS19/26](https://www.fca.org.uk/publication/policy/ps19-26.pdf)).
  legislation.gov.uk marks the retained EU text as revoked. Check UK wording against the FCA Handbook (see §6).

**3c. Session expiry.**

- **SCA RTS Art. 4(3)(d):** "the maximum time without activity by the payer after being authenticated for
  accessing its payment account online… shall not exceed 5 minutes"
  ([Art. 4](https://www.legislation.gov.uk/eur/2018/389/article/4)). The EBA reads this as applying only where
  SCA is applied ([EBA Q&A 2018_4068](https://www.eba.europa.eu/single-rule-book-qa/qna/view/publicId/2018_4068)).
- **ASVS 5.0 7.3.1/7.3.2 (L2):** an inactivity timeout and an absolute lifetime, both forcing re-authentication.
- **ASVS 7.4.1 (L1):** after logout or expiry, "the application disallows any further use of the session"
  ([ASVS V7](https://github.com/OWASP/ASVS/blob/master/5.0/en/0x16-V7-Session-Management.md)).

Ledgerline has no idle-timeout feature. Tests should cover:
- the browser warning (Clock, R2)
- server expiry, by setting the server clock or shortening the session timeout in a test profile, then
  `page.reload()` and `assertThat(page).hasURL(".../login")`
- reuse of a captured cookie after logout: save `storageState`, log out, start a new context from the saved
  state, and expect a redirect
- API tokens. `TokenStore` tokens "live until the app restarts" (the class Javadoc), which fails ASVS
  7.3.x. Add expiry and a test.

Login already rotates the session id (`LoginController` invalidates the old session; **[Probed]**: the
pre-login and post-login `JSESSIONID` differ). That meets ASVS 7.2.4. Keep it as a regression test.

**3d. Authorisation bypass at the API level.**

- **ASVS 8.2.1 (function level), 8.2.2 (data level, IDOR/BOLA), 8.3.1 (enforced at a trusted service layer)**
  ([ASVS V8](https://github.com/OWASP/ASVS/blob/master/5.0/en/0x17-V8-Authorization.md)). Lesson 10 covers 8.2.1
  for auditors.
- IDOR needs data ownership, but every Ledgerline teller sees every customer. Add **branch scoping** (teller A:
  branch 1 only) or a small **customer self-service** login, so `GET /api/accounts/{otherCustomersId}` → 404/403
  becomes testable.

**3e. CSRF and cookie attributes.**

- **ASVS 5.0 3.5.1 (L1):** cross-origin requests to sensitive functions are validated as originating from the app.
- **ASVS 3.3.2 (L2):** `SameSite` set "according to the purpose of the cookie"
  ([ASVS V3](https://github.com/OWASP/ASVS/blob/master/5.0/en/0x12-V3-Web-Frontend-Security.md)).
- **[Probed]:** `Set-Cookie: JSESSIONID=…; Path=/; HttpOnly` has no `SameSite`, and a `POST /accounts/3/movements`
  with a foreign `Origin` was applied.
- **Test:**
  1. Read the session cookie with `context.cookies()`.
  2. Assert `cookie.sameSite` and `cookie.httpOnly`. The `Cookie` fields `sameSite`, `httpOnly` and `secure`
     are verified in 1.64.
  3. Send a forged POST with `page.request().post(url, RequestOptions.create().setHeader("Origin", "https://evil.example").setForm(...))`
     and expect 403 once the app is fixed.
- **Playwright limitation:** Playwright can't easily reproduce a real cross-site navigation from another origin
  without a second server. Teach the request-level test, and say so. **[Inference]**

**App changes (all of R3)**

| Change | Mission |
|---|---|
| Users `maker`/`checker` (or `teller2`), `PENDING_APPROVAL` status, approval threshold, approve/reject UI + API | Maker-checker |
| OTP challenge for transfers > threshold and for new payees; OTP bound to (amount, payee) hash; 5-attempt lock; `GET /api/testing/otp-outbox` | SCA |
| Optional WebAuthn passkey sign-in (only if the team's real project uses passkeys) | SCA (advanced) |
| Idle timeout (configurable; default 5 min) + absolute lifetime; API token expiry | Session |
| Origin/CSRF-token check on state-changing form posts; `SameSite=Lax` (or `Strict`) on the session cookie; a fault toggle to switch it off | CSRF |
| Branch ownership on customers, or a customer self-service role | IDOR |

### R4. Accessibility as a regulated requirement (High in the EU, S–M)

**Regulatory driver.** The European Accessibility Act, Directive (EU) 2019/882, makes accessibility a legal
requirement for consumer banking in the EU:

- **Art. 2(2)(d):** the Directive applies to "consumer banking services" provided to consumers after 28 June 2025
  ([Art. 2](https://www.legislation.gov.uk/eudr/2019/882/article/2)).
- **Art. 31(2):** Member States "shall apply those measures from 28 June 2025"
  ([Art. 31](https://www.legislation.gov.uk/eudr/2019/882/article/31)).
- **Annex I, Section IV(e):** identification methods, electronic signatures, security and payment services must
  be "perceivable, operable, understandable and robust". Information must not exceed CEFR level B2
  ([Annex I](https://www.legislation.gov.uk/eudr/2019/882/annex/I)).

**WCAG 2.2 criteria that map directly onto banking flows** ([WCAG 2.2](https://www.w3.org/TR/WCAG22/),
a W3C Recommendation of 12 December 2024):

- **3.3.4 Error Prevention (Legal, Financial, Data) (AA).** Financial transactions must be reversible, checked,
  or have "a mechanism… for reviewing, confirming, and correcting information before finalizing the
  submission". Ledgerline's Move-money form submits immediately, so **add a review step** for transfers. It also
  serves the SCA Art. 5(1)(a) display.
- **2.2.1 Timing Adjustable (A).** Warn the user "and [give them] at least 20 seconds to extend the time limit
  with a simple action". The Understanding doc says security-driven time limits count as set by the content,
  and that time-limited 2FA tokens "can be considered _essential_"
  ([Understanding 2.2.1](https://www.w3.org/WAI/WCAG22/Understanding/timing-adjustable.html)). The idle warning
  from R2 is the test target: `page.clock()` plus `assertThat(dialog).isVisible()`, then check that "Stay
  signed in" is focusable.
- **3.3.8 Accessible Authentication (Minimum) (AA).** No cognitive-function test unless there is an
  alternative or a mechanism. For OTP entry, test that paste works. `Locator.fill` sets the value, so to
  exercise a paste-blocking handler, dispatch a paste event or use the keyboard. **[Inference]**

**Tooling.** The Java accessibility guide recommends Deque's **`com.deque.html.axe-core:playwright`**, with
`new AxeBuilder(page).withTags(...).include(...).exclude(...).disableRules(...).analyze()`
([accessibility testing](https://playwright.dev/java/docs/accessibility-testing)). The latest version on Maven
Central is **4.14.0**, last updated 2026-10-15
([metadata](https://repo1.maven.org/maven2/com/deque/html/axe-core/playwright/maven-metadata.xml)). Its licence
is MPL-2.0 ([repo](https://github.com/dequelabs/axe-core-maven-html)). It's a third-party dependency, so it's not
in this repo's classpath today, and banks often need licence review. The guide is explicit that "many
accessibility problems can only be discovered through manual testing".

Teach both:
- an axe scan per key page, tagged `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, plus `wcag22aa` if axe 4.14
  supports it (see §6)
- the course's existing semantic assertions (`hasAccessibleDescription`, `matchesAriaSnapshot`)

**App changes:** a transfer review/confirm page (3.3.4); the idle-warning `alertdialog` (2.2.1); an OTP field
with `autocomplete="one-time-code"` that allows paste (3.3.8). Seed one deliberate axe violation behind a fault
toggle, so the first scan has something to find.

### R5. Evidence, audit and PII hygiene for test artefacts (Medium, S)

Course-effectiveness Gap 8 already covers retention, env-var credentials and proxies. This section adds the
banking-specific mechanics.

- **Masking screenshots.** `Page.ScreenshotOptions.setMask(List<Locator>)`, `setMaskColor(String)` (v1.35) and
  `setStyle(String)` (v1.41) are all verified in 1.64
  ([release notes](https://playwright.dev/java/docs/release-notes)). Mask account numbers, names and balances.
  For example, mask `getByTestId("account-number")` and the "Customers" table cells.
- **Traces can't be masked.** `Tracing.StartOptions` in 1.64 has only `setScreenshots`, `setSnapshots`,
  `setSources`, `setTitle`, `setName`, `setLive`, `setAriaSnapshots` and `setScreenSnapshots`. There is no mask
  or redaction option (verified in 1.64). Snapshots "capture DOM snapshot and record network activity on every
  action" ([Tracing](https://playwright.dev/java/docs/api/class-tracing)). So the rule is: **synthetic data
  only**, plus `RETAIN_ON_FAILURE` and short retention. Masking is not an alternative to synthetic data.
  **[Inference]**
- **Structured evidence.**
  - `Tracing.group(String)` and `groupEnd()` (v1.49) label phases ("Maker submits", "Checker approves").
  - `Tracing.StartOptions.setTitle` names the trace after the test or requirement ID (e.g. "ASVS 2.3.5").
  - `Tracing.startHar(path, StartHarOptions.setContent(HarContentPolicy.OMIT))` (v1.60) records network
    evidence without bodies.
  - `Page.screencast()` (v1.59; `showChapter`, `showOverlay`) gives narrated walkthrough videos for reviewers
    or auditors.

  All verified in 1.64.
- **Reporting.** Playwright Java has no built-in HTML reporter. Its JUnit page lists no reporter
  ([JUnit](https://playwright.dev/java/docs/junit)), and its CI page has no Java reporting guidance
  ([CI](https://playwright.dev/java/docs/ci)). Allure's JUnit 5 integration (`io.qameta.allure:allure-jupiter`,
  with an AspectJ agent for `@Step`/`@Attachment`) is documented by Allure, but its page **does not mention
  Playwright Java** ([Allure JUnit 5](https://allurereport.org/docs/junit5/)). If you use it, attach
  `trace.zip` and masked screenshots yourself with `Allure.attachment(...)`. Teach the Surefire XML reports
  first. **[Inference]**

**App changes:** none. Add `data-testid`s on PII cells if masking by role or label proves awkward.

### R6. Test architecture at bank scale (Medium, S–M)

- **Shared ledger state in parallel.** Tests that assert global figures ("Total balance held", "Active
  accounts") can't share a database with tests that move money. Use JUnit's `@ResourceLock("ledger-totals",
  mode = READ_WRITE)` on money-moving classes, `READ` on the totals tests, and `@Isolated` on anything that
  calls `/api/testing/reset`. Two READ tests "may run in parallel with each other… but not while any other test
  that requires READ_WRITE access to the same shared resource is running". `@Isolated` classes run "without
  any other tests running at the same time"
  ([JUnit 5.14 guide §2.22.2](https://docs.junit.org/5.14.0/user-guide/#writing-tests-parallel-execution-synchronization)).
  This answers the question lesson 14 asks but never resolves.
- **Data builders through the API.** Extend `LedgerApi` into a fluent builder, e.g.
  `aCustomer().withCurrentAccount("500.00").withPendingApproval("15000.00")`, so every mission arranges its
  scenario in one line (lesson 09's pattern, scaled). **[Inference]**
- **Quarantine, not retry.** Use `@Tag("quarantine")` and exclude that group in the main CI job, with a nightly
  job that runs it. Course-effectiveness Gap 6 already covers Surefire reruns. For payments, never auto-retry a
  test whose arrange step moves money unless that step is idempotent (R1). **[Inference]**
- **CI.**
  - Run in the pinned `mcr.microsoft.com/playwright/java:v1.64.0-noble` image
    ([CI](https://playwright.dev/java/docs/ci)) with `--init` and `--ipc=host`. The Docker page says
    "Using --ipc=host is recommended when using Chromium. Without it, Chromium can run out of memory and crash"
    ([Docker](https://playwright.dev/java/docs/docker)).
  - The image runs browsers as root with the Chromium sandbox disabled. That is acceptable for your own app;
    for anything untrusted, use `pwuser` and the seccomp profile (same page).
- **Contract tests between services** (Pact, Spring Cloud Contract) are the right layer for inter-service
  payment APIs, but are out of scope for a Playwright course. See §6.

**App changes:** none required.

### R7. Visual regression: say "no" clearly and offer the Java alternatives (Low, S)

1.64's `PageAssertions` and `LocatorAssertions` contain `matchesAriaSnapshot` but **no screenshot
assertion**. This was verified with `javap`, and agrees with `playwright-java.md` §12 and lesson 14's note.
For banking UIs, the structure of a statement or receipt matters more than pixels. Use
`assertThat(locator).matchesAriaSnapshot(...)` (v1.49; page-level in v1.60) for the transfer-confirmation
region. If pixels really matter (brand-controlled statements), take masked screenshots with a fixed
`setStyle` and compare them with a separate image-diff library, or run only the visual suite in Playwright's
Node runner ([test-snapshots](https://playwright.dev/docs/test-snapshots)). **[Inference]**

---

## 4. Proposed module list

**Core (compressed from today's 00–12, about 2 days).** Keep the current lessons, applying
course-effectiveness's fixes (recall, fading, choosing the test level). Fold lesson 05 (codegen) into 03, and
lesson 11's mocking into a shorter unit.

**Banking track (about 3–4 days; one "mission" per half-day).** Each mission has the same shape:
1. Read the rule (with its regulatory or ASVS source).
2. Switch on the feature flag.
3. Write a test plan that says which level each test belongs at.
4. Implement the tests.
5. Run the **InvariantCheck**.
6. Switch on the matching **fault toggle** and prove your tests catch it.

| # | Mission | Rule source | Playwright Java focus (1.64) | App change |
|---|---|---|---|---|
| B0 | Ledger invariants and money boundaries | ASVS 2.3.3 | `APIRequestContext`, `@ParameterizedTest`, `BigDecimal.compareTo` | Canonical-amount fix + `exponent-amounts` fault |
| B1 | Double submit and idempotency | ASVS 2.3.4, 2.4.2 | `dblclick`, `reload`, repeated `post` | `Idempotency-Key`; PRG already present |
| B2 | Concurrent payments | ASVS 2.3.4; WSTG 4.10.4 | One `Playwright` per thread / `HttpClient`; invariant | `lost-update` fault |
| B3 | Cut-off times and scheduled payments | — (business rule) | Server-clock endpoint; `page.clock()` for JS parts; `setTimezoneId` | `Clock` bean, value dates, scheduler + trigger |
| B4 | Session timeout and warning | SCA RTS Art. 4(3)(d); ASVS 7.3; WCAG 2.2.1 | `clock().install/fastForward/runFor`; `storageState` reuse after logout | Idle timeout, `alertdialog`, token expiry |
| B5 | Maker-checker | ASVS 2.3.5, 8.2.1 | Two `BrowserContext`s with per-role `setStorageStatePath`; API bypass tests | Pending-approval workflow, maker/checker users |
| B6 | SCA / OTP with dynamic linking (+ passkeys option) | SCA RTS Art. 4, 5, 13, 16 | OTP outbox via API; `context.credentials()`; ARIA snapshot of the confirmation | OTP challenge, attempt lock, payee list |
| B7 | Accessible payments | EAA Art. 2(2)(d), Annex I §IV(e); WCAG 3.3.4, 3.3.8 | `AxeBuilder` (Deque), `hasAccessibleDescription`, `matchesAriaSnapshot` | Review/confirm step; OTP field; a11y fault |
| B8 | CSRF, cookies and IDOR | ASVS 3.3.2, 3.5.1, 8.2.2 | `context.cookies()` attributes; `page.request().post` with forged `Origin` | Origin/CSRF check, `SameSite`, branch scoping |
| B9 | Evidence pack (capstone) | Internal audit / change evidence | `Tracing.group`, `setTitle`, `startHar` with `OMIT`, `screenshot.setMask`, `screencast().showChapter` | None |

**Why missions beat extending the linear lessons.** Each mission is a whole task with a real rule, a
feature, and a planted fault to detect. That is the deliberate-practice and "own a feature" format that
course-effectiveness already credits lessons 13 and 14 for. Missions also give the team a catalogue it can
reuse on the real project ("which mission does this story resemble?"). **[Inference]**

**Minimum viable version (if only about 3 days of authoring are available):** B0, B4 and B5. These need only
the `Clock` bean, the idle timeout and the approval workflow. They cover money, time and multi-actor auth,
the three areas the current course has the least of.

---

## 5. Sources

**Playwright for Java (playwright.dev/java and the 1.64.0 jar)**

- Clock guide: https://playwright.dev/java/docs/clock
- Clock API: https://playwright.dev/java/docs/api/class-clock
- Credentials (virtual WebAuthn): https://playwright.dev/java/docs/api/class-credentials
- Tracing API: https://playwright.dev/java/docs/api/class-tracing
- RequestOptions: https://playwright.dev/java/docs/api/class-requestoptions
- Accessibility testing: https://playwright.dev/java/docs/accessibility-testing
- Emulation: https://playwright.dev/java/docs/emulation
- Authentication: https://playwright.dev/java/docs/auth
- API testing: https://playwright.dev/java/docs/api-testing
- JUnit: https://playwright.dev/java/docs/junit
- CI: https://playwright.dev/java/docs/ci
- Docker: https://playwright.dev/java/docs/docker
- Videos: https://playwright.dev/java/docs/videos
- Release notes: https://playwright.dev/java/docs/release-notes
- Node-only visual comparisons: https://playwright.dev/docs/test-snapshots
- API presence was confirmed with `javap` against `playwright-1.64.0.jar`: `Clock`, `Credentials`,
  `Page$ScreenshotOptions`, `Tracing`, `Tracing$StartOptions`, `Tracing$StartHarOptions`, `Screencast`,
  `Browser$NewContextOptions`, `BrowserContext$StorageStateOptions`, `options.Cookie`,
  `options.RequestOptions`, `assertions.PageAssertions` and `assertions.LocatorAssertions`

**Testing frameworks and libraries**

- JUnit 5.14 User Guide, §2.22.2 Synchronization: https://docs.junit.org/5.14.0/user-guide/
- Deque axe-core Maven metadata: https://repo1.maven.org/maven2/com/deque/html/axe-core/playwright/maven-metadata.xml
- Deque axe-core repository: https://github.com/dequelabs/axe-core-maven-html
- Allure JUnit 5: https://allurereport.org/docs/junit5/
- Java SE 21 `java.time.Clock`: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html

**Security**

- OWASP ASVS 5.0, V2 Validation and Business Logic: https://github.com/OWASP/ASVS/blob/master/5.0/en/0x11-V2-Validation-and-Business-Logic.md
- OWASP ASVS 5.0, V3 Web Frontend Security: https://github.com/OWASP/ASVS/blob/master/5.0/en/0x12-V3-Web-Frontend-Security.md
- OWASP ASVS 5.0, V7 Session Management: https://github.com/OWASP/ASVS/blob/master/5.0/en/0x16-V7-Session-Management.md
- OWASP ASVS 5.0, V8 Authorization: https://github.com/OWASP/ASVS/blob/master/5.0/en/0x17-V8-Authorization.md
- OWASP ASVS releases: https://github.com/OWASP/ASVS/releases
- OWASP WSTG 4.2, Business Logic Testing: https://wstg.owasp.org/v4.2/4-Web_Application_Security_Testing/10-Business_Logic_Testing/

**Regulation**

- SCA RTS, Delegated Regulation (EU) 2018/389, Art. 4: https://www.legislation.gov.uk/eur/2018/389/article/4
- SCA RTS Art. 5: https://www.legislation.gov.uk/eur/2018/389/article/5
- SCA RTS Art. 13: https://www.legislation.gov.uk/eur/2018/389/article/13
- SCA RTS Art. 16: https://www.legislation.gov.uk/eur/2018/389/article/16
- EBA Q&A 2018_4068 (scope of the 5-minute rule): https://www.eba.europa.eu/single-rule-book-qa/qna/view/publicId/2018_4068
- FCA PS19/26 (UK SCA-RTS): https://www.fca.org.uk/publication/policy/ps19-26.pdf
- European Accessibility Act, Directive (EU) 2019/882, Art. 2: https://www.legislation.gov.uk/eudr/2019/882/article/2
- EAA Art. 31: https://www.legislation.gov.uk/eudr/2019/882/article/31
- EAA Annex I: https://www.legislation.gov.uk/eudr/2019/882/annex/I
- European Commission, EAA overview: https://commission.europa.eu/strategy-and-policy/policies/justice-and-fundamental-rights/disability/union-equality-strategy-rights-persons-disabilities-2021-2030/european-accessibility-act_en

**Accessibility standards**

- W3C WCAG 2.2: https://www.w3.org/TR/WCAG22/
- W3C Understanding SC 2.2.1: https://www.w3.org/WAI/WCAG22/Understanding/timing-adjustable.html

**This repo**

- `app/src/main/java/dev/upskill/ledger/ledger/MoneyService.java`
- `.../common/Money.java`, `.../common/Dates.java`
- `.../ledger/LedgerEntry.java`, `.../account/Account.java`
- `.../auth/TokenStore.java`, `.../auth/PageAuthInterceptor.java`
- `.../web/LoginController.java`, `.../seed/DataSeeder.java`
- `app/src/main/resources/static/js/app.js`, `application.yml`

---

## 6. Unverified items and caveats

1. **EUR-Lex text not read directly.** EUR-Lex returned empty pages to the fetch tool, and an automated
   download was refused, so the SCA RTS and EAA wording was read from the **legislation.gov.uk** copies of the
   same EU instruments. legislation.gov.uk marks the retained SCA RTS as revoked in the UK by FSMA 2023
   Sch. 1 Pt. 3, and shows its last version. The current EU text was amended by Delegated Regulation
   2022/2360, which changed Article 10 (the account-information exemption), not Articles 4, 5, 13 or 16. That
   amendment detail came from search snippets and **was not verified**. Before quoting these in training,
   check them against EUR-Lex.
2. **UK position.** The UK SCA-RTS now sits in the FCA Handbook (FCA PS19/26). I did **not** read the current
   UK Article 4 wording, so I can't confirm the UK keeps "5 minutes" verbatim. The EAA is an EU directive. Its
   applicability to a UK-only bank is out of scope, and **not verified**. Equivalent UK drivers (Equality Act
   2010, FCA Consumer Duty) weren't researched.
3. **EN 301 549 and WCAG version.** Which WCAG version the harmonised standard under the EAA references
   (2.1 AA vs 2.2) comes only from secondary sources and is **not verified**. Whether axe-core 4.14 ships a
   `wcag22aa` tag was also **not verified**. The Playwright page's examples stop at `wcag21aa`.
4. **ASVS 5.0 release year.** The GitHub release page shows "30 May" with no year. **May 2025** is from my own
   knowledge.
5. **Spring session configuration.** `server.servlet.session.timeout` (default `30m`) and
   `server.servlet.session.cookie.same-site` are from my knowledge of Spring Boot. The Boot 4.1 property
   reference was truncated in fetch and is **not verified**. Ledgerline doesn't use Spring Security, so its
   CSRF support doesn't apply unless it's added.
6. **Why concurrency is correct today.** The probes show serialised, correct results. I didn't establish
   **why**. It could be H2 row locking on the `accounts` update, or `@Version` optimistic locking surfacing as
   something other than a 500 (no lock errors were logged). Don't present this as a guarantee on a real
   database.
7. **Probe scope.** All [Probed] results come from one local run on 2026-11-11 against a freshly started app
   on port 8091, using `curl`. They were not written as tests.
8. **Release-notes discrepancy.** The fetched release notes put `APIRequestContext.addCookies/cookies/
   clearCookies` under **1.65**. `playwright-java.md` §6 says 1.64, and the 1.64.0 jar **does** contain them.
   Treat the jar as authoritative.
9. **`setMaxRetries` and payments.** The docs say it retries "network errors". The release-notes summary
   said ECONNRESET specifically. The double-payment risk I describe is my inference.
10. **Allure.** It's documented for JUnit 5 by its own vendor, not by Playwright. Its effect on JUnit parallel
    execution with `@UsePlaywright` was **not tested**.
11. **Contract testing** (Pact, Spring Cloud Contract) isn't researched against primary sources here.
12. **Effort estimates and the mission format** are judgement. No pilot has been run.
