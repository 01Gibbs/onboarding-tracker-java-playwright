package dev.upskill.e2e.broken;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Test;

@UsePlaywright(BrokenSuiteOptions.class)
class Case03NavigationTest {

    @Test
    void tellerOpensCustomersFromTheDashboard(Page page) {
        new LoginPage(page).loginAs(User.TELLER);

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Customers")).click();

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Customers"))).isVisible();
    }
}
