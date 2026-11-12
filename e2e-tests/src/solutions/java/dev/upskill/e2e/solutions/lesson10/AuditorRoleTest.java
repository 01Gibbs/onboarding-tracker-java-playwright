package dev.upskill.e2e.solutions.lesson10;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 10: sign in through the UI ONCE, save the browser's storage state (cookies) to a file,
 * then start every test already signed in by loading that file into a new context.
 */
@UsePlaywright(LedgerlineOptions.class)
class AuditorRoleTest {

    private static final Path AUDITOR_STATE = Path.of("target", ".auth", "auditor.json");

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void signInOnce(Browser browser, Browser.NewContextOptions options) {
        try (BrowserContext context = browser.newContext(options)) {
            Page page = context.newPage();
            new LoginPage(page).loginAs(User.AUDITOR);
            assertThat(page.getByTestId("current-user")).containsText(User.AUDITOR.displayName());
            context.storageState(new BrowserContext.StorageStateOptions().setPath(AUDITOR_STATE));
        }
    }

    @BeforeEach
    void startSignedIn(Browser browser, Browser.NewContextOptions options) {
        context = browser.newContext(options.setStorageStatePath(AUDITOR_STATE));
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void auditorLandsOnTheDashboardWithoutLoggingIn() {
        page.navigate("/");

        assertThat(page.getByTestId("current-user")).containsText("Alex Auditor (Auditor)");
    }

    @Test
    void auditorDoesNotSeeEditingControls() {
        page.navigate("/customers");
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New customer"))).hasCount(0);

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Alice Archer")).click();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete customer")))
                .hasCount(0);

        page.navigate("/accounts/1");
        assertThat(page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName("Move money"))).hasCount(0);
        assertThat(page.getByTestId("transaction-row").first()).isVisible(); // but can still read history
    }

    @Test
    void auditorIsBlockedFromChangesEvenWithoutTheButtons() {
        // Don't just trust the UI: send the request the button would have sent.
        // page.request() shares the page's cookies, so this is sent as the signed-in auditor.
        APIResponse response = page.request().post("/accounts/1/status",
                RequestOptions.create().setForm(FormData.create().set("status", "FROZEN")));

        assertEquals(403, response.status());
    }
}
