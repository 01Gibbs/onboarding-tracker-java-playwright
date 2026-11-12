package dev.upskill.e2e.solutions.lesson12;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.CustomersPage;
import dev.upskill.e2e.solutions.pages.DashboardPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Lesson 12: a tiny, fast "smoke" suite tagged so it can be run on its own:
 * <pre>./mvnw -pl e2e-tests test -Psolutions -Dgroups=smoke</pre>
 */
@Tag("smoke")
@UsePlaywright(LedgerlineOptions.class)
class SmokeTest {

    @Test
    void tellerCanSignInAndSeeTheDashboard(Page page) {
        DashboardPage dashboard = new LoginPage(page).loginAs(User.TELLER);

        assertThat(dashboard.heading()).isVisible();
    }

    @Test
    void customerListLoads(Page page) {
        new LoginPage(page).loginAs(User.TELLER);

        CustomersPage customers = new CustomersPage(page).open();

        assertThat(customers.rows().first()).isVisible();
    }
}
