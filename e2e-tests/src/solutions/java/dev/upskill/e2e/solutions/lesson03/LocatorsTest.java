package dev.upskill.e2e.solutions.lesson03;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lesson 03: finding elements the way a user would - by role, label, text and test id. */
@UsePlaywright
class LocatorsTest {

    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void signIn(Page page) {
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
    }

    @Test
    void navigateUsingTheMainNavigation(Page page) {
        Locator mainNav = page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Main"));

        mainNav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customers")).click();

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Customers"))).isVisible();
        // The current page's link is marked with aria-current="page"
        assertThat(mainNav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customers")))
                .hasAttribute("aria-current", "page");
    }

    @Test
    void searchForACustomerByLabelAndPlaceholder(Page page) {
        page.navigate(BASE_URL + "/customers");

        page.getByPlaceholder("Name or email").fill("archer");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search")).click();

        Locator rows = page.getByTestId("customer-row");
        assertThat(rows).hasCount(1);
        assertThat(rows).containsText("Alice Archer");
        assertThat(page.getByTestId("result-count")).hasText("1 customer");
    }

    @Test
    void filterTableRowsAndChainLocators(Page page) {
        page.navigate(BASE_URL + "/customers");

        Locator table = page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName("Customers"));
        Locator dansRow = table.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText("Dan Evans"));

        assertThat(dansRow.getByTestId("status-badge")).hasText("Suspended");
        assertThat(dansRow.getByRole(AriaRole.CELL).first()).hasText("CUS-0004");
    }

    @Test
    void findByTextAndExactMatching(Page page) {
        page.navigate(BASE_URL + "/customers");

        // Substring, case-insensitive by default...
        assertThat(page.getByText("alice.archer@")).isVisible();
        // ...or exact when you need it
        assertThat(page.getByText("Erin Foster", new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    @Test
    void everyFrozenAccountShowsAFrozenBadge(Page page) {
        page.navigate(BASE_URL + "/accounts");

        page.getByLabel("Status").selectOption("Frozen");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters")).click();

        Locator rows = page.getByTestId("account-row");
        assertThat(rows.filter(new Locator.FilterOptions().setHasText("Holiday Fund"))).isVisible();
        for (Locator badge : rows.getByTestId("status-badge").all()) {
            assertThat(badge).hasText("Frozen");
        }
    }

    @Test
    void openAnAccountFromTheCustomersPage(Page page) {
        page.navigate(BASE_URL + "/customers");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Alice Archer")).click();

        Locator accounts = page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName("Accounts"));
        Locator savingsRow = accounts.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHas(page.getByText("Savings", new Page.GetByTextOptions().setExact(true))));
        savingsRow.getByRole(AriaRole.LINK).click();

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).containsText("Rainy Day");
        assertThat(page.getByTestId("account-number")).hasText("10000002");
    }
}
