package dev.upskill.e2e.broken;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.DashboardPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Test;

@UsePlaywright(BrokenSuiteOptions.class)
class Case07ActivityEmptyStateTest {

    @Test
    void dashboardShowsAnEmptyStateWhenThereIsNoActivity(Page page) {
        new LoginPage(page).loginAs(User.TELLER);
        DashboardPage dashboard = new DashboardPage(page).open();

        page.route("**/api/activity*", route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("application/json").setBody("[]")));

        assertThat(dashboard.activityPanel()).containsText("No activity yet.");
    }
}
