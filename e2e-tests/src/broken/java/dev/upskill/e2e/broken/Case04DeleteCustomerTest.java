package dev.upskill.e2e.broken;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.CustomerDetailPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Test;

@UsePlaywright(BrokenSuiteOptions.class)
class Case04DeleteCustomerTest {

    @Test
    void deletedCustomerDisappears(Page page, Playwright playwright) {
        long id;
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            id = api.createCustomer().get("id").getAsLong();
        }
        new LoginPage(page).loginAs(User.TELLER);
        CustomerDetailPage customer = new CustomerDetailPage(page).open(id);

        customer.click("Delete customer");

        assertThat(customer.flash()).containsText("deleted");
    }
}
