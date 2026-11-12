package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lessons 07 and 11: the dashboard at "/". */
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
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    /** The "Recent activity" region. */
    public Locator activityPanel() {
        throw new UnsupportedOperationException("TODO lesson 11");
    }

    /** One element per activity entry (data-testid="activity-item"). */
    public Locator activityItems() {
        throw new UnsupportedOperationException("TODO lesson 11");
    }

    public void refreshActivity() {
        throw new UnsupportedOperationException("TODO lesson 11");
    }
}
