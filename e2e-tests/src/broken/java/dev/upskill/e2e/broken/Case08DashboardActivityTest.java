package dev.upskill.e2e.broken;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.DashboardPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Test;

@UsePlaywright(BrokenSuiteOptions.class)
class Case08DashboardActivityTest {

    @Test
    void refreshingShowsRecentActivity(Page page) {
        DashboardPage dashboard = new LoginPage(page).loginAs(User.TELLER);

        dashboard.refreshActivity();
        int items = dashboard.activityItems().count();

        assertTrue(items > 0, "expected some recent activity but found " + items);
    }
}
