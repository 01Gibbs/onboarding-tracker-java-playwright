package dev.upskill.e2e.solutions.lesson11;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.pages.DashboardPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 11: the dashboard loads "Recent activity" from GET /api/activity with fetch().
 * We intercept that request to test states that are hard to reproduce with real data.
 */
@UsePlaywright(LedgerlineOptions.class)
class ActivityFeedTest {

    private static final String ACTIVITY = "**/api/activity*";

    private static final String TWO_ENTRIES = """
            [
              {"id": 1, "accountId": 1, "accountNumber": "12345678", "accountNickname": "Mocked Current",
               "type": "DEPOSIT", "amount": 1234.5, "description": "Mocked salary"},
              {"id": 2, "accountId": 1, "accountNumber": "12345678", "accountNickname": "Mocked Current",
               "type": "WITHDRAWAL", "amount": 20, "description": "Mocked coffee"}
            ]
            """;

    private Page page;
    private DashboardPage dashboard;

    @BeforeEach
    void signIn(Page page) {
        this.page = page;
        new LoginPage(page).loginAs(User.TELLER);
        dashboard = new DashboardPage(page);
    }

    @Test
    void realActivityIsShownByDefault() {
        // No mocking: wait for the real response while reloading
        Response response = page.waitForResponse(ACTIVITY, () -> page.reload());

        assertEquals(200, response.status());
        assertThat(dashboard.activityItems().first()).isVisible();
    }

    @Test
    void showsMockedEntries() {
        page.route(ACTIVITY, route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody(TWO_ENTRIES)));

        dashboard.open();

        assertThat(dashboard.activityItems()).hasCount(2);
        assertThat(dashboard.activityItems().first()).containsText("Deposit · Mocked salary");
        assertThat(dashboard.activityItems().first()).containsText("+£1,234.50");
        assertThat(dashboard.activityItems().nth(1)).containsText("-£20.00");
    }

    @Test
    void showsAnEmptyState() {
        page.route(ACTIVITY, route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("application/json").setBody("[]")));

        dashboard.open();

        assertThat(dashboard.activityPanel()).containsText("No activity yet.");
    }

    @Test
    void recoversFromAServerErrorAfterRefresh() {
        page.route(ACTIVITY, route -> route.fulfill(new Route.FulfillOptions().setStatus(500)));
        dashboard.open();
        assertThat(dashboard.activityPanel().getByRole(AriaRole.ALERT))
                .containsText("Could not load recent activity");

        page.unroute(ACTIVITY);
        dashboard.refreshActivity();

        assertThat(dashboard.activityItems().first()).isVisible();
    }

    @Test
    void handlesANetworkFailure() {
        page.route(ACTIVITY, route -> route.abort("connectionrefused"));

        dashboard.open();

        assertThat(dashboard.activityPanel().getByRole(AriaRole.ALERT)).isVisible();
    }

    @Test
    void modifiesTheRealResponse() {
        page.route(ACTIVITY, route -> {
            APIResponse real = route.fetch();
            String patched = real.text().replaceAll("\"description\":\"[^\"]*\"", "\"description\":\"PATCHED\"");
            route.fulfill(new Route.FulfillOptions().setResponse(real).setBody(patched));
        });

        dashboard.open();

        assertThat(dashboard.activityItems().first()).containsText("PATCHED");
    }

    @Test
    void refreshCallsTheApiAgain() {
        assertThat(dashboard.activityItems().first()).isVisible(); // initial load has finished
        AtomicInteger calls = new AtomicInteger();
        page.onRequest(request -> {
            if (request.url().contains("/api/activity")) {
                calls.incrementAndGet();
            }
        });

        Response response = page.waitForResponse(ACTIVITY, dashboard::refreshActivity);

        assertTrue(response.ok());
        assertEquals(1, calls.get());
    }
}
