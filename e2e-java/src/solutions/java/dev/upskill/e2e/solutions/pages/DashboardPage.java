package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class DashboardPage {

    private final Page page;
    public final NavBar nav;

    public DashboardPage(Page page) {
        this.page = page;
        this.nav = new NavBar(page);
    }

    public DashboardPage open() {
        page.navigate("/");
        return this;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard"));
    }

    /** A summary card, e.g. stat("stat-customers"). Assert on it with containsText. */
    public Locator stat(String testId) {
        return page.getByTestId(testId);
    }

    public Locator activityItems() {
        return page.getByTestId("activity-item");
    }

    public Locator activityPanel() {
        return page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName("Recent activity"));
    }

    public void refreshActivity() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh")).click();
    }
}
