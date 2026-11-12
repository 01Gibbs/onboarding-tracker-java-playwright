# Playwright for Java: research notes

- **Date researched:** 2026-11-11
- **Version found (latest stable on Maven Central):** `com.microsoft.playwright:playwright:1.64.0`
  (`<latest>` and `<release>` both `1.64.0`, `lastUpdated` 20261021172042):
  https://repo1.maven.org/maven2/com/microsoft/playwright/playwright/maven-metadata.xml
- GitHub release `v1.64.0`, published 2026-10-21, not a prerelease:
  https://github.com/microsoft/playwright-java/releases/tag/v1.64.0 (checked via the GitHub API
  `repos/microsoft/playwright-java/releases`). Earlier releases: v1.63.0 (2026-09-14), v1.62.0 (2026-08-03).
- Sources used: only playwright.dev/java (guides and API reference), the microsoft/playwright-java repo
  (tag `v1.64.0`), and Maven Central POMs and metadata.

---

## 0. Caveats and discrepancies (read first)

1. **The docs describe a newer version than Maven Central has.** The release notes already have a
   "Version 1.65" section (https://playwright.dev/java/docs/release-notes), but Maven Central's latest is
   **1.64.0**. Behaviour in the 1.65 notes is **not** in 1.64.0. For example, the "Not Inert"
   actionability check is a 1.65 breaking change. The actionability page already lists it, but on 1.64.0
   actions do not wait for that check. Pin lessons to 1.64.0 and treat 1.65 items as "coming".
2. **The JUnit docs use a method that does not compile on 1.64.0.** https://playwright.dev/java/docs/junit
   shows `new Options().setContextOption(...)`. The actual class has **`setContextOptions(...)`** (plural)
   and no singular alias:
   https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/junit/Options.java
3. **The JUnit integration is labelled "experimental".** The page title is "JUnit (experimental)"
   (https://playwright.dev/java/docs/junit), and the Javadoc says "this API is experimental and is
   subject to changes" (Options.java / UsePlaywright.java, links above). It was introduced in **v1.42**
   (https://playwright.dev/java/docs/release-notes, "Version 1.42 → Experimental JUnit integration").
4. **The Playwright artifact does not bring JUnit with it.** In the parent POM, `junit-jupiter-api` is
   `provided` scope, and `junit-jupiter-engine` and `junit-jupiter-params` are `test` scope, so none of
   them pass through to your project. You must declare JUnit Jupiter yourself:
   https://repo1.maven.org/maven2/com/microsoft/playwright/parent-pom/1.64.0/parent-pom-1.64.0.pom.
   The playwright-java repo itself builds against JUnit **5.14.1**
   (https://github.com/microsoft/playwright-java/blob/v1.64.0/pom.xml, `<junit.version>`).
5. **Several doc snippets are wrong as written** (worth fixing when copying them into lessons):
   - Mock guide: `route.fulfill(RequestOptions.create().setData(data))`
     (https://playwright.dev/java/docs/mock). The Route API reference lists only `Route.FulfillOptions`,
     with `setBody`, `setBodyBytes`, `setContentType`, `setHeaders`, `setPath`, `setResponse` and
     `setStatus`. There is no `RequestOptions` overload and no `setData`/JSON option
     (https://playwright.dev/java/docs/api/class-route#fulfill). For a JSON mock, use
     `setContentType("application/json").setBody(json)`.
   - Mock guide: `Response response = route.fetch();`. The Network guide and API reference show that
     `route.fetch()` returns **`APIResponse`** (https://playwright.dev/java/docs/network,
     https://playwright.dev/java/docs/api/class-route#fetch). The same snippet's Gson calls
     (`parsed.add(new JsonObject().add(...))`) also won't compile.
   - Route.fallback reference: the example passes `new Route.ResumeOptions()`, but the documented
     argument type is `Route.FallbackOptions` (https://playwright.dev/java/docs/api/class-route#fallback).
   - POM guide: `import com.microsoft.playwright;` is not valid Java. Use
     `import com.microsoft.playwright.*;` (https://playwright.dev/java/docs/pom).
   - Auth guide: `System.getenv().put(...)` throws `UnsupportedOperationException`, because the env map
     is immutable (https://playwright.dev/java/docs/auth, "Session storage").
   - Input guide: `page.mouse.wheel(0, 10)` should be `page.mouse().wheel(0, 10)`
     (https://playwright.dev/java/docs/input, "Scrolling").
6. **Some doc examples need newer Java than the library does.** The library targets Java 8, but some
   examples use newer syntax. For instance, `assertThat(page).matchesAriaSnapshot("""...""")` uses a text
   block, which needs Java 15+ (https://playwright.dev/java/docs/api/class-pageassertions), and
   `Map.of(...)` in the 1.64 notes needs Java 9+.
7. **Recent breaking changes worth knowing**
   (https://playwright.dev/java/docs/release-notes):
   - 1.64: elements inside hidden iframes are considered hidden.
   - 1.58: removed `_react`/`_vue` selectors and the `devtools` launch option.
   - 1.57: removed `page.accessibility()`.
   - 1.49: the `chrome`/`msedge` channels switched to the new headless mode.

---

## 1. Version, Java requirement, Maven setup

- **Latest stable:** 1.64.0 (Maven metadata link above).
- **Minimum Java:** "Java 8 or higher" (https://playwright.dev/java/docs/intro, System requirements). This
  matches the repo's `<maven.compiler.source>8</maven.compiler.source>` /
  `<maven.compiler.target>8</maven.compiler.target>`
  (https://github.com/microsoft/playwright-java/blob/v1.64.0/pom.xml).
- **Operating systems:** Windows 11+, Windows Server 2019+ or WSL; macOS 14 (Sonoma)+; Debian 12/13,
  Ubuntu 22.04/24.04/26.04 on x86-64 or arm64 (https://playwright.dev/java/docs/intro).
- **Bundled browsers in 1.64:** Chromium 156.0.8078.4, Firefox 157.0, WebKit 27.2
  (https://playwright.dev/java/docs/release-notes, and the repo README
  https://github.com/microsoft/playwright-java/blob/main/README.md).

Recommended `pom.xml`, from https://playwright.dev/java/docs/intro:

```xml
<dependencies>
  <dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.64.0</version>
  </dependency>
</dependencies>
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.10.1</version>
      <!-- References to interface static methods are allowed only at source level 1.8 or above -->
      <configuration>
        <source>1.8</source>
        <target>1.8</target>
      </configuration>
    </plugin>
  </plugins>
</build>
```

The docs do not give JUnit coordinates. Add JUnit Jupiter yourself (see Caveat 4). Something like
`org.junit.jupiter:junit-jupiter` 5.x in `test` scope plus Surefire is a reasonable choice. This is my
recommendation, not a quote from the docs. The repo uses JUnit 5.14.1.

First run, from https://playwright.dev/java/docs/intro:

```java
import com.microsoft.playwright.*;

public class App {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("https://playwright.dev");
            System.out.println(page.title());
        }
    }
}
```

```bash
mvn compile exec:java -D exec.mainClass="org.example.App"
```

The docs say this run downloads the Playwright package and installs Chromium, Firefox and WebKit
(https://playwright.dev/java/docs/intro).

---

## 2. JUnit 5 integration (`@UsePlaywright`)

Source: https://playwright.dev/java/docs/junit (unless noted otherwise)

**Imports and packages**

```java
import com.microsoft.playwright.junit.UsePlaywright;   // annotation
import com.microsoft.playwright.junit.Options;         // options builder
import com.microsoft.playwright.junit.OptionsFactory;  // interface: Options getOptions()
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
```

The package `com.microsoft.playwright.junit` contains exactly `Options`, `OptionsFactory` and
`UsePlaywright`. The implementation lives in `com.microsoft.playwright.impl.junit`
(https://github.com/microsoft/playwright-java/tree/v1.64.0/playwright/src/main/java/com/microsoft/playwright/junit).

**Basic test**

```java
@UsePlaywright
public class TestExample {
  @Test
  void basicTest(Page page) {
    page.navigate("https://playwright.dev/");
    assertThat(page).hasTitle(Pattern.compile("Playwright"));
  }
}
```

**Fixtures you can inject as test-method parameters** (table on the JUnit page):

| Fixture | Type | Scope |
|---|---|---|
| `page` | `Page` | New for each test ("Isolated page for this test run") |
| `browserContext` | `BrowserContext` | New for each test; `page` belongs to this context |
| `browser` | `Browser` | Shared across tests to save resources |
| `playwright` | `Playwright` | Shared between tests running on the same thread |
| `request` | `APIRequestContext` | New for each test, for API testing |

The `UsePlaywright` Javadoc adds one more injectable type, **`Browser.NewContextOptions`**. You get a
new instance every time, configured from your `OptionsFactory`. Unlike `Page` and `BrowserContext`, it
can also be injected into `@BeforeAll`/`@AfterAll`:
https://github.com/microsoft/playwright-java/blob/v1.64.0/playwright/src/main/java/com/microsoft/playwright/junit/UsePlaywright.java

```java
@BeforeAll
static void beforeAll(Browser browser, Browser.NewContextOptions contextOptions) {
  BrowserContext context = browser.newContext(contextOptions);
  // ...
  context.close();
}
```

**How isolation works.** The Javadoc says `Page` and `BrowserContext` are "created before each test and
closed after the test has finished", while `Browser` and `Playwright` are reused. The `Playwright`
instance is kept in a `ThreadLocal`, and a registry closes every instance at the end
(`impl/junit/PlaywrightExtension.java` at v1.64.0). If you don't set a browser, the default is
**chromium** (`impl/junit/BrowserExtension.java` at v1.64.0).

**Custom options with `OptionsFactory`.** This is the docs example corrected for 1.64.0 (see Caveat 2):

```java
@UsePlaywright(MyTest.CustomOptions.class)
public class MyTest {
  public static class CustomOptions implements OptionsFactory {
    @Override
    public Options getOptions() {
      return new Options()
          .setHeadless(false)
          .setContextOptions(new Browser.NewContextOptions()      // docs show setContextOption (wrong)
              .setBaseURL("https://github.com"))
          .setApiRequestOptions(new APIRequest.NewContextOptions()
              .setBaseURL("https://playwright.dev"));
    }
  }

  @Test
  public void testWithCustomOptions(Page page, APIRequestContext request) {
    page.navigate("/");
    assertThat(page).hasURL(Pattern.compile("github"));
    APIResponse response = request.get("/");
    assertTrue(response.text().contains("Playwright"));
  }
}
```

The full set of `Options` setters in 1.64.0, from `Options.java`:

- `setHeadless(Boolean)`, `setBrowserName(String)` ("chromium"/"firefox"/"webkit"), `setChannel(String)`
- `setDeviceName(String)`, `setBaseUrl(String)`, `setTestIdAttribute(String)`
- `setIgnoreHTTPSErrors(Boolean)`, `setDefaultTimeout(Double)` (applied to each injected context)
- `setLaunchOptions(BrowserType.LaunchOptions)`, `setContextOptions(Browser.NewContextOptions)`,
  `setApiRequestOptions(APIRequest.NewContextOptions)`
- `setPlaywrightCreateOptions(...)`, `setWsEndpoint(String)` with `setConnectOptions(...)` for a remote
  browser
- `setOutputDir(Path)`
- `setTrace(Options.Trace.OFF | ON | RETAIN_ON_FAILURE)` (default `OFF`). Traces are saved to
  `<outputDir>/<TestClass>.<method>-<displayName>/trace.zip`, and `outputDir` defaults to
  `${user.dir}/test-results` (`impl/junit/BrowserContextExtension.java` at v1.64.0). The docs page does
  not mention this option, so it is only documented in source.

**Running tests in parallel.** The docs say that "it is not safe to use same Playwright objects from
multiple threads without extra synchronization", so the recommendation is one Playwright instance per
thread. This config runs classes concurrently and the methods within each class sequentially, using up
to half the CPU cores as threads:

```properties
# src/test/resources/junit-platform.properties
junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = same_thread
junit.jupiter.execution.parallel.mode.classes.default = concurrent
junit.jupiter.execution.parallel.config.strategy=dynamic
junit.jupiter.execution.parallel.config.dynamic.factor=0.5
```

The properties come from https://playwright.dev/java/docs/junit and
https://playwright.dev/java/docs/test-runners. The docs don't name the file;
`junit-platform.properties` is the standard JUnit location. The playwright-java repo passes the same
values to Surefire as `configurationParameters`
(https://github.com/microsoft/playwright-java/blob/v1.64.0/pom.xml).

**Doing it by hand, without `@UsePlaywright`** (https://playwright.dev/java/docs/test-runners and
https://playwright.dev/java/docs/running-tests). Create `Playwright`/`Browser` in `@BeforeAll`, a new
`BrowserContext`/`Page` in `@BeforeEach`, and close the context in `@AfterEach`. For parallel runs,
use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` with Playwright stored in instance fields, so
each class instance has its own Playwright:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestFixtures {
  Playwright playwright;
  Browser browser;
  BrowserContext context;
  Page page;

  @BeforeAll  void launchBrowser()        { playwright = Playwright.create(); browser = playwright.chromium().launch(); }
  @AfterAll   void closeBrowser()         { playwright.close(); }
  @BeforeEach void createContextAndPage() { context = browser.newContext(); page = context.newPage(); }
  @AfterEach  void closeContext()         { context.close(); }
}
```

**Headed mode:** `launch(new BrowserType.LaunchOptions().setHeadless(false))`
(https://playwright.dev/java/docs/running-tests). Tests run headless by default.

**Test isolation** (https://playwright.dev/java/docs/writing-tests): "a BrowserContext … is an in-memory
isolated browser profile. It's recommended to create a new BrowserContext for each test."

---

## 3. Locators

Source: https://playwright.dev/java/docs/locators

**Recommended built-in locators, in priority order:** `getByRole`, `getByText`, `getByLabel`,
`getByPlaceholder`, `getByAltText`, `getByTitle`, `getByTestId`. The page says to prefer "user-facing
attributes and explicit contracts such as Page.getByRole()". `AriaRole` is in
`com.microsoft.playwright.options.AriaRole` (https://playwright.dev/java/docs/writing-tests).

```java
page.getByLabel("User Name").fill("John");
page.getByLabel("Password").fill("secret-password");
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
assertThat(page.getByText("Welcome, John!")).isVisible();

// exact or regex text matching
page.getByText("Welcome, John", new Page.GetByTextOptions().setExact(true));
page.getByText(Pattern.compile("welcome, john$", Pattern.CASE_INSENSITIVE));
page.getByRole(AriaRole.BUTTON,
    new Page.GetByRoleOptions().setName(Pattern.compile("submit", Pattern.CASE_INSENSITIVE)));

page.getByTestId("directions").click();          // matches data-testid by default
```

Text matching always normalises whitespace, even with an exact match.

**Custom test id attribute:**

```java
playwright.selectors().setTestIdAttribute("data-pw");
```

`Selectors.setTestIdAttribute` was added in v1.27. It accepts a comma-separated list such as
`"data-pw,data-ti"` (https://playwright.dev/java/docs/api/class-selectors#selectors-set-test-id-attribute).
With JUnit fixtures, use `Options.setTestIdAttribute(...)` instead (Options.java).

**Locators are lazy.** A locator is re-resolved before every action, so it survives re-renders.

**Locators are strict.** An action throws if more than one element matches. Multi-element operations
such as `count()` are fine.

**Filtering:**

```java
page.getByRole(AriaRole.LISTITEM)
    .filter(new Locator.FilterOptions().setHasText("Product 2"))
    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart"))
    .click();

// has / hasNot: the inner locator is matched relative to the outer one
page.getByRole(AriaRole.LISTITEM)
    .filter(new Locator.FilterOptions()
        .setHas(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Product 2"))));
assertThat(page.getByRole(AriaRole.LISTITEM)
    .filter(new Locator.FilterOptions().setHasNotText("Out of stock"))).hasCount(5);
```

**Chaining and operators:**

```java
Locator product = page.getByRole(AriaRole.LISTITEM)
    .filter(new Locator.FilterOptions().setHasText("Product 2"));
product.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add to cart")).click();

Locator button = page.getByRole(AriaRole.BUTTON).and(page.getByTitle("Subscribe"));       // both
assertThat(newEmail.or(dialog).first()).isVisible();                                      // either
page.locator("button").visible().click();                                                 // visible only
saveButton.within(dialog).click();   // new in 1.64 (Locator.within)
```

**Lists and nth:**

```java
assertThat(page.getByRole(AriaRole.LISTITEM)).hasCount(3);
assertThat(page.getByRole(AriaRole.LISTITEM)).hasText(new String[] {"apple", "banana", "orange"});
Locator banana = page.getByRole(AriaRole.LISTITEM).nth(1);   // first()/last()/nth(): use sparingly
for (Locator row : page.getByRole(AriaRole.LISTITEM).all()) System.out.println(row.textContent());
```

**CSS and XPath** go through `page.locator("css=button")` or `page.locator("//button")`, with the prefix
auto-detected. The docs discourage them because they are brittle.

**iframes:** `page.frameLocator("#my-frame").getByRole(...)`.

**Shadow DOM:** pierced by default, except by XPath and in closed shadow roots.

---

## 4. Assertions

Source: https://playwright.dev/java/docs/test-assertions

**Entry point:** `import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;`

`assertThat` has overloads for `Locator` (returns `LocatorAssertions`), `Page` (returns
`PageAssertions`) and `APIResponse` (returns `APIResponseAssertions`).

These assertions retry automatically. **The default timeout is 5 seconds.**

```java
assertThat(page.getByTestId("status")).hasText("Submitted");
```

**Locator assertions:**

- State: `isAttached`, `isChecked`, `isDisabled`, `isEditable`, `isEmpty`, `isEnabled`, `isFocused`,
  `isHidden`, `isInViewport`, `isVisible`
- Content and attributes: `containsClass` (v1.52), `containsText`, `hasAccessibleDescription`,
  `hasAccessibleName`, `hasAttribute`, `hasClass`, `hasCount`, `hasCSS`, `hasId`, `hasJSProperty`,
  `hasRole`, `hasText`, `hasValue`, `hasValues`, `matchesAriaSnapshot`
- The API reference also lists `hasAccessibleErrorMessage` (v1.50):
  https://playwright.dev/java/docs/api/class-locatorassertions

**Page assertions:** `hasTitle(String|Pattern)`, `hasURL(String|Pattern)` (with an optional
`setIgnoreCase`), and `matchesAriaSnapshot` (v1.60):
https://playwright.dev/java/docs/api/class-pageassertions

**APIResponse assertions:** `isOK()`, which passes for a status in the 200..299 range:
https://playwright.dev/java/docs/api/class-apiresponseassertions

**Negation:** `assertThat(page).not().hasURL("error");` and `assertThat(response).not().isOK();`
(the same API reference pages).

**Changing the timeout:**

```java
// global
PlaywrightAssertions.setDefaultAssertionTimeout(10_000);

// per assertion
assertThat(page.getByText("Name")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10_000));
```

`LocatorAssertions` is in `com.microsoft.playwright.assertions`.

**Action timeout (not assertion timeout):** for injected contexts, use JUnit
`Options.setDefaultTimeout(Double)`, which calls `BrowserContext.setDefaultTimeout`
(Options.java and BrowserContextExtension.java at v1.64.0).

---

## 5. Actions and auto-waiting

Sources: https://playwright.dev/java/docs/input and https://playwright.dev/java/docs/actionability

**Actions:**

```java
page.getByRole(AriaRole.TEXTBOX).fill("Peter");                 // preferred text entry
page.getByLabel("Birth date").fill("2020-02-02");
page.getByLabel("I agree to the terms above").check();          // also setChecked/uncheck
page.getByLabel("Choose a color").selectOption("blue");
page.getByLabel("Choose a color").selectOption(new SelectOption().setLabel("Blue"));
page.getByText("Item").dblclick();
page.getByText("Item").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
page.getByText("Item").click(new Locator.ClickOptions().setModifiers(Arrays.asList(KeyboardModifier.SHIFT)));
page.getByText("Item").hover();
page.locator("#area").pressSequentially("Hello World!");        // only when special key handling needed
page.getByText("Submit").press("Enter");
page.getByRole(AriaRole.TEXTBOX).press("Control+ArrowRight");
page.getByLabel("Upload file").setInputFiles(Paths.get("myfile.pdf"));
page.locator("#item-to-be-dragged").dragTo(page.locator("#item-to-drop-at"));
page.getByRole(AriaRole.BUTTON).click(new Locator.ClickOptions().setForce(true)); // skip some checks
```

**Actionability.** Before an action, Playwright waits until the target passes the checks below,
otherwise it throws `TimeoutError`. For `click()` that means the locator resolves to exactly one
element, and the element is **Visible**, **Stable**, **Receives Events**, **Enabled** and **Not Inert**
(the Not Inert check arrives in 1.65; see Caveat 1).

| Action | Visible | Stable | Receives events | Enabled | Editable |
|---|---|---|---|---|---|
| check, click, dblclick, setChecked, tap, uncheck | Yes | Yes | Yes | Yes | - |
| hover, dragTo | Yes | Yes | Yes | - | - |
| screenshot | Yes | Yes | - | - | - |
| fill, clear | Yes | - | - | Yes | Yes |
| selectOption | Yes | - | - | Yes | - |
| press, pressSequentially, focus, blur, dispatchEvent, setInputFiles | - | - | - | - | - |

What each check means:

- **Visible:** the element has a non-empty bounding box and is not `visibility:hidden`. `opacity:0`
  still counts as visible; `display:none` and zero-size elements do not.
- **Stable:** the bounding box is unchanged for two consecutive animation frames.
- **Receives Events:** the element is the hit target at the action point, so it is not covered by an
  overlay.
- **`force` option:** "disables non-essential actionability checks".

---

## 6. API testing with `APIRequestContext`

Source: https://playwright.dev/java/docs/api-testing

**Creating a context:**

```java
Map<String, String> headers = new HashMap<>();
headers.put("Accept", "application/vnd.github.v3+json");
headers.put("Authorization", "token " + API_TOKEN);

APIRequestContext request = playwright.request().newContext(new APIRequest.NewContextOptions()
    .setBaseURL("https://api.github.com")
    .setExtraHTTPHeaders(headers));
```

**Sending requests:**

```java
APIResponse newIssue = request.post("/repos/" + USER + "/" + REPO + "/issues",
    RequestOptions.create().setData(data));          // com.microsoft.playwright.options.RequestOptions
assertTrue(newIssue.ok());
APIResponse issues = request.get("/repos/" + USER + "/" + REPO + "/issues");
String body = issues.text();
request.delete("/repos/" + USER + "/" + REPO);
```

**Cleanup:** call `request.dispose()` and then `playwright.close()` in `@AfterAll`.

**With JUnit fixtures:** inject `APIRequestContext request` and configure it with
`Options.setApiRequestOptions(...)` (https://playwright.dev/java/docs/junit). Each test gets a new
context.

**Mixing API and UI**, as shown on the API testing page:

- **Arrange via the API, then assert in the UI.** Create the data with `request.post(...)`, then open
  the page and use `assertThat(locator).hasText(...)`.
- **Act in the UI, then verify via the API.** Run the UI flow, then call
  `assertThat(request.get(...)).isOK()`.
- **Share login state.** Storage state works in both directions between `APIRequestContext` and
  `BrowserContext`:

```java
APIRequestContext requestContext = playwright.request().newContext(
    new APIRequest.NewContextOptions().setHttpCredentials("user", "passwd"));
requestContext.get("https://api.example.com/login");
String state = requestContext.storageState();
BrowserContext context = browser.newContext(new Browser.NewContextOptions().setStorageState(state));
```

**`page.request()`:** a page also has its own request context that shares the page's cookies
(https://playwright.dev/java/docs/api/class-apiresponseassertions example:
`page.request().get(...)`).

**New in 1.64:** `APIRequestContext.addCookies()`, `cookies()` and `clearCookies()`
(https://playwright.dev/java/docs/release-notes).

---

## 7. Codegen and browser installation (CLI through Maven)

All Playwright CLI commands run the class `com.microsoft.playwright.CLI` through exec-maven-plugin.

**Codegen** (https://playwright.dev/java/docs/codegen):

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="codegen demo.playwright.dev/todomvc"
```

Useful flags (same page):

- `--viewport-size='800,600'`
- `--device="iPhone 13"`
- `--color-scheme=dark`
- `--timezone`, `--geolocation`, `--lang`
- `--save-storage=auth.json` and `--load-storage=auth.json`
- `--user-data-dir=...`
- `--http-credentials="user:pass"`

The Inspector can record assertions ("assert visibility", "assert text" and "assert value") and has a
"Pick Locator" tool.

To record from inside your own code, call `page.pause()` in a headed browser.

**Installing browsers** (https://playwright.dev/java/docs/browsers):

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install webkit"
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps"
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps --only-shell"   # CI, headless only
```

**Where browsers are stored:**

- macOS: `~/Library/Caches/ms-playwright`
- Linux: `~/.cache/ms-playwright`
- Windows: `%USERPROFILE%\AppData\Local\ms-playwright`

**Skipping the browser download:** `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 mvn test`.

**Branded browsers:** set the channel with
`new BrowserType.LaunchOptions().setChannel("chrome"|"msedge"|...)`.

**Gradle equivalent:** `./gradlew playwright --args="help"` with a `JavaExec` task whose `mainClass` is
`com.microsoft.playwright.CLI` (https://playwright.dev/java/docs/test-runners).

**Inference (not stated in the docs):** `exec:java` uses the runtime classpath by default. If you
declare Playwright in `test` scope, the CLI class may not be found. Keep the default `compile` scope as
the intro POM does, or add `-D exec.classpathScope=test`.

---

## 8. Trace viewer

Source: https://playwright.dev/java/docs/trace-viewer

```java
BrowserContext context = browser.newContext();
context.tracing().start(new Tracing.StartOptions()
    .setScreenshots(true)
    .setSnapshots(true)
    .setSources(true));
Page page = context.newPage();
page.navigate("https://playwright.dev");
context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("trace.zip")));
```

Start tracing before you create or navigate the page.

**Opening a trace:**

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="show-trace trace.zip"
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="show-trace https://example.com/trace.zip"
```

You can also drag and drop the file into https://trace.playwright.dev, which processes it locally in
the browser.

**Viewer tabs:** Actions (with before, action and after snapshots), Source, Call, Log, Errors, Console,
Network and Metadata.

**With JUnit fixtures:** `Options.setTrace(Options.Trace.RETAIN_ON_FAILURE)` writes
`test-results/.../trace.zip` automatically. This is only in source; see section 2.

---

## 9. Network mocking and waiting

Sources: https://playwright.dev/java/docs/mock, https://playwright.dev/java/docs/network and
https://playwright.dev/java/docs/api/class-route

**Fulfilling (mocking):**

```java
page.route("**/api/fetch_data", route -> route.fulfill(new Route.FulfillOptions()
    .setStatus(200)
    .setBody(testData)));

// JSON mock (fixed version of the mock-guide example; see Caveat 5)
page.route("*/**/api/v1/fruits", route -> route.fulfill(new Route.FulfillOptions()
    .setContentType("application/json")
    .setBody("[{\"name\":\"Strawberry\",\"id\":21}]")));
page.navigate("https://demo.playwright.dev/api-mocking");
assertThat(page.getByText("Strawberry")).isVisible();

// serve a file
page.route("**/xhr_endpoint", route -> route.fulfill(
    new Route.FulfillOptions().setPath(Paths.get("mock_data.json"))));
```

**Modifying the real response:**

```java
page.route("**/title.html", route -> {
  APIResponse response = route.fetch();
  String body = response.text().replace("<title>", "<title>My prefix:");
  route.fulfill(new Route.FulfillOptions().setResponse(response).setBody(body));
});
```

**Aborting and resuming:**

```java
page.route("**/*.{png,jpg,jpeg}", route -> route.abort());
page.route("**/*", route -> {
  if ("image".equals(route.request().resourceType())) route.abort(); else route.resume();
});
page.route("**/*", route -> route.resume(new Route.ResumeOptions().setMethod("POST")));
```

`abort` takes an optional error code (`"failed"` by default, also `"timedout"`, `"connectionrefused"`,
and others).

**Fallback (v1.23).** `fallback()` passes the request to the next matching handler, whereas `resume()`
sends it straight to the network. Handlers run in **reverse registration order**:

```java
page.route("**/*", route -> {
  if (!route.request().method().equals("GET")) { route.fallback(); return; }
  // handle GET only
});
```

**Scope.** `BrowserContext.route()` applies to the whole context, including popups.

**Glob patterns:** `*` matches anything except `/`, `**` matches anything including `/`, and `{a,b}`
matches alternatives. The pattern must match the entire URL. You can also pass a `Pattern` or a
predicate.

**Waiting for responses:**

```java
Response response = page.waitForResponse("**/api/fetch_data", () -> {
  page.getByText("Update").click();
});
Response r2 = page.waitForResponse(Pattern.compile("\\.jpeg$"), () -> page.getByText("Update").click());
Response r3 = page.waitForResponse(r -> r.url().contains(token), () -> page.getByText("Update").click());
```

**Listening to traffic:** `page.onRequest(req -> ...)` and `page.onResponse(res -> ...)`.

**Other features:**

- HAR replay: `page.routeFromHAR(Path.of("./hars/fruit.har"), new RouteFromHAROptions().setUrl(...).setUpdate(true|false))`.
- WebSocket mocking: `page.routeWebSocket(...)`.
- Service workers can hide traffic from `route()`. Block them with `setServiceWorkers` if needed.

---

## 10. Authentication and `storageState`

Source: https://playwright.dev/java/docs/auth

Keep state files in `playwright/.auth` and add that directory to `.gitignore`. The files contain
sensitive cookies and tokens.

```java
// log in once through the UI
page.navigate("https://github.com/login");
page.getByLabel("Username or email address").fill("username");
page.getByLabel("Password").fill("password");
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

// save
context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get("state.json")));

// reuse
BrowserContext context = browser.newContext(
    new Browser.NewContextOptions().setStorageStatePath(Paths.get("state.json")));
```

**What storage state includes:** cookies, local storage and virtual WebAuthn credentials (passkeys).
IndexedDB is not saved by default; turn on `setIndexedDB` when saving. Session storage is not persisted,
but the docs give a workaround using `addInitScript` (see Caveat 5 for its bug).

**With JUnit fixtures:** pass `.setStorageStatePath(...)` inside `Options.setContextOptions(...)`.
This is an inference that combines the two APIs.

**Through codegen:** `--save-storage=auth.json` and `--load-storage=auth.json` (section 7).

---

## 11. Page Object Model

Source: https://playwright.dev/java/docs/pom

A page object wraps a `Page`, holds `Locator` fields that are created in the constructor (they are lazy,
so that is safe), and exposes methods named after user intent. Example, with the import corrected
(see Caveat 5):

```java
package models;

import com.microsoft.playwright.*;

public class SearchPage {
  private final Page page;
  private final Locator searchTermInput;

  public SearchPage(Page page) {
    this.page = page;
    this.searchTermInput = page.locator("[aria-label='Enter your search term']");
  }
  public void navigate() { page.navigate("https://bing.com"); }
  public void search(String text) {
    searchTermInput.fill(text);
    searchTermInput.press("Enter");
  }
}

// In the test
SearchPage searchPage = new SearchPage(page);
searchPage.navigate();
searchPage.search("search query");
```

The page is short. It does not say whether assertions belong in page objects or in tests.

---

## 12. Dialogs and screenshots

**Dialogs** (https://playwright.dev/java/docs/dialogs):

- Dialogs (`alert`, `confirm`, `prompt`) are **auto-dismissed** when no `onDialog` listener is
  registered.
- If you register a listener, it **must** call `accept()` or `dismiss()`, otherwise the action hangs.
- Register the handler **before** the action that triggers the dialog.

```java
page.onDialog(dialog -> dialog.accept());
page.getByRole(AriaRole.BUTTON).click();

// WRONG: hangs, because the listener never accepts or dismisses
page.onDialog(dialog -> System.out.println(dialog.message()));
```

For `beforeunload`, use `page.close(new Page.CloseOptions().setRunBeforeUnload(true))` with a handler
that checks `dialog.type()` is `"beforeunload"`.

**Screenshots** (https://playwright.dev/java/docs/screenshots):

```java
page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot.png")));
page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot.png")).setFullPage(true));
byte[] buffer = page.screenshot();
page.locator(".header").screenshot(new Locator.ScreenshotOptions().setPath(Paths.get("screenshot.png")));
```

The Java docs have no built-in visual comparison assertion (`toHaveScreenshot`). None of the assertion
pages for Java list one (https://playwright.dev/java/docs/test-assertions).

**Video (new in 1.64):** `page.screencast().start(new Screencast.StartOptions().setPath(...).setFps(60))`
and `setRecordVideoFps` (https://playwright.dev/java/docs/release-notes).
