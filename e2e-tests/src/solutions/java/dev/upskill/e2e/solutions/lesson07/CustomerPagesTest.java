package dev.upskill.e2e.solutions.lesson07;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.CustomerDetailPage;
import dev.upskill.e2e.solutions.pages.CustomerFormPage;
import dev.upskill.e2e.solutions.pages.CustomersPage;
import dev.upskill.e2e.solutions.pages.DashboardPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.TestData;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 07: the lesson 06 scenarios rewritten with page objects.
 * Tests read as user intent; selectors live in one place (the pages package).
 */
@UsePlaywright(LedgerlineOptions.class)
class CustomerPagesTest {

    private DashboardPage dashboard;

    @BeforeEach
    void signIn(Page page) {
        dashboard = new LoginPage(page).loginAs(User.TELLER);
    }

    @Test
    void createSearchAndOpenACustomer() {
        String lastName = TestData.uniqueLastName();

        CustomerDetailPage customer = dashboard.nav.customers()
                .newCustomer()
                .fill("Nina", lastName, TestData.uniqueEmail("nina"))
                .save();
        assertThat(customer.flash()).hasText("Customer Nina " + lastName + " created");

        CustomersPage list = customer.nav().customers().search(lastName);
        assertThat(list.rows()).hasCount(1);

        CustomerDetailPage reopened = list.openCustomer("Nina " + lastName);
        assertThat(reopened.heading()).containsText("Nina " + lastName);
        assertThat(reopened.detail("Phone")).hasText("Not provided");
    }

    @Test
    void deleteACustomer(Page page) {
        String lastName = TestData.uniqueLastName();
        CustomerDetailPage customer = new CustomersPage(page).open()
                .newCustomer()
                .fill("Omar", lastName, TestData.uniqueEmail("omar"))
                .save();

        customer.deleteAndConfirm();

        CustomersPage list = new CustomersPage(page).search(lastName);
        assertThat(list.emptyMessage()).isVisible();
    }

    @Test
    void validationErrorsAreShownNextToFields(Page page) {
        CustomerFormPage form = new CustomersPage(page).open().newCustomer();

        form.fill("", "", "nope").saveExpectingErrors();

        assertThat(form.errorSummary()).isVisible();
        // Each message is linked to its field (aria-describedby), so assert it as the field's description
        assertThat(form.firstName).hasAccessibleDescription("First name is required");
        assertThat(form.email).hasAccessibleDescription("Enter a valid email address");
    }

    @Test
    void searchingForSomethingThatDoesNotExist(Page page) {
        CustomersPage list = new CustomersPage(page).open().search("no-such-customer-" + TestData.unique());

        assertThat(list.rows()).hasCount(0);
        assertThat(list.emptyMessage()).isVisible();
    }
}
