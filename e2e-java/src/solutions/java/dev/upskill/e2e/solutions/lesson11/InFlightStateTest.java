package dev.upskill.e2e.solutions.lesson11;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.pages.DashboardPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

/**
 * Lesson 11 (part 2): observing the page WHILE a request is in flight.
 *
 * <p>We HOLD the request: the route handler stores the Route without answering it, so the browser
 * keeps waiting while we assert on the loading state. Then we let it go with route.resume().
 *
 * <p>Keep route handlers short and never call Playwright page/locator methods inside them - the handler
 * runs on Playwright's dispatch thread and such calls can deadlock.
 *
 * <p>This only works for fetch()/XHR requests. During a full page navigation (like submitting the
 * "Move money" form) Playwright's locators deliberately wait for the navigation to finish, so a
 * transient state such as the "Processing…" button can't be asserted - and usually shouldn't be.
 */
@UsePlaywright(LedgerlineOptions.class)
class InFlightStateTest {

    @Test
    void showsALoadingStateUntilActivityArrives(Page page) {
        DashboardPage dashboard = new LoginPage(page).loginAs(User.TELLER);
        assertThat(dashboard.activityItems().first()).isVisible(); // let the initial load finish first
        List<Route> held = new CopyOnWriteArrayList<>();
        page.route("**/api/activity*", held::add);

        dashboard.refreshActivity();

        assertThat(dashboard.activityPanel().getByRole(AriaRole.STATUS)).hasText("Loading activity…");
        assertThat(dashboard.activityItems()).hasCount(0);

        held.forEach(Route::resume);

        assertThat(dashboard.activityPanel().getByRole(AriaRole.STATUS)).isHidden();
        assertThat(dashboard.activityItems().first()).isVisible();
    }
}
