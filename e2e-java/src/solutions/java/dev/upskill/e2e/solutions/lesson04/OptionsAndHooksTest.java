package dev.upskill.e2e.solutions.lesson04;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 04: shared options (base URL, headless) via an OptionsFactory, and a @BeforeEach hook.
 * Note the relative URLs: page.navigate("/accounts") resolves against the configured base URL.
 */
@UsePlaywright(LedgerlineOptions.class)
class OptionsAndHooksTest {

    @BeforeEach
    void signInAsTeller(Page page) {
        page.navigate("/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
        assertThat(page.getByTestId("current-user")).isVisible();
    }

    @Test
    void dashboardShowsSummaryCards(Page page) {
        assertThat(page.getByTestId("stat-customers")).containsText("Customers");
        assertThat(page.getByTestId("stat-active-accounts")).containsText("Active accounts");
        assertThat(page.getByTestId("stat-total-balance")).containsText("£");
    }

    @Test
    void accountsPageListsSeededAccounts(Page page) {
        page.navigate("/accounts");

        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("10000001"))).isVisible();
    }
}
