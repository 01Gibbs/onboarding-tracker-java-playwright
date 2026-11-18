package dev.upskill.e2e.broken;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.CustomerDetailPage;
import dev.upskill.e2e.solutions.pages.CustomersPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Test;

/** Run the suite twice before you decide this one is fine. */
@UsePlaywright(BrokenSuiteOptions.class)
class Case10CreateCustomerTest {

    @Test
    void tellerCreatesACustomer(Page page) {
        new LoginPage(page).loginAs(User.TELLER);

        CustomerDetailPage customer = new CustomersPage(page).open()
                .newCustomer()
                .fill("Quinn", "Example", "quinn.example@example.test")
                .save();

        assertThat(customer.flash()).hasText("Customer Quinn Example created");
    }
}
