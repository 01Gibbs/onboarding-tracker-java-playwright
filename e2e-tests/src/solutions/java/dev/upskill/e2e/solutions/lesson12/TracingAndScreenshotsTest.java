package dev.upskill.e2e.solutions.lesson12;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.AccountPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Lesson 12: collecting evidence. LedgerlineOptions already keeps a trace for every FAILED test
 * (Options.Trace.RETAIN_ON_FAILURE -> test-results/). This test shows the manual equivalents.
 * Open a trace with:
 * <pre>./mvnw -pl e2e-tests exec:java -Dexec.args="show-trace target/traces/account-page.zip"</pre>
 */
@UsePlaywright(LedgerlineOptions.class)
class TracingAndScreenshotsTest {

    private static final Path OUT = Path.of("target");

    @Test
    void recordATraceAndScreenshotsManually(Browser browser, Browser.NewContextOptions options) {
        try (BrowserContext context = browser.newContext(options)) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));
            Page page = context.newPage();

            new LoginPage(page).loginAs(User.TELLER);
            AccountPage account = new AccountPage(page).open(1);
            assertThat(account.balance).isVisible();

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(OUT.resolve("screenshots/account-full.png")).setFullPage(true));
            account.balance.screenshot(new Locator.ScreenshotOptions()
                    .setPath(OUT.resolve("screenshots/account-balance.png")));

            context.tracing().stop(new Tracing.StopOptions().setPath(OUT.resolve("traces/account-page.zip")));
        }

        assertTrue(Files.exists(OUT.resolve("traces/account-page.zip")));
        assertTrue(Files.exists(OUT.resolve("screenshots/account-balance.png")));
    }
}
