package dev.upskill.e2e.solutions.lesson10;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 10 (part 2): a faster way to get the same storage state - post the login form with an
 * APIRequestContext (no browser needed), then hand its cookies to browser contexts.
 */
@UsePlaywright(LedgerlineOptions.class)
class TellerSessionViaApiTest {

    private static final Path TELLER_STATE = Path.of("target", ".auth", "teller.json");

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void signInWithoutABrowser(Playwright playwright) {
        APIRequestContext request = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL(LedgerlineOptions.BASE_URL));
        try {
            var response = request.post("/login", RequestOptions.create().setForm(FormData.create()
                    .set("username", User.TELLER.username())
                    .set("password", User.TELLER.password())));
            assertThat(response).isOK(); // the redirect to the dashboard was followed
            request.storageState(new APIRequestContext.StorageStateOptions().setPath(TELLER_STATE));
        } finally {
            request.dispose();
        }
    }

    @BeforeEach
    void startSignedIn(Browser browser, Browser.NewContextOptions options) {
        context = browser.newContext(options.setStorageStatePath(TELLER_STATE));
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void tellerIsAlreadySignedIn() {
        page.navigate("/customers");

        assertThat(page.getByTestId("current-user")).containsText("Terry Teller (Teller)");
    }
}
