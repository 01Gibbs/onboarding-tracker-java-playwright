package dev.upskill.e2e.solutions.lesson14;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.AccountPage;
import dev.upskill.e2e.solutions.pages.CustomerDetailPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.TestData;
import dev.upskill.e2e.solutions.support.User;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Lesson 14 capstone: one realistic journey through the whole product, driven entirely through
 * the UI and verified at the end through the API.
 */
@UsePlaywright(LedgerlineOptions.class)
class CustomerJourneyTest {

    @Test
    void onboardFundAndTidyUpACustomer(Page page, Playwright playwright) {
        String lastName = TestData.uniqueLastName();

        // 1. Sign in and create the customer
        CustomerDetailPage customer = new LoginPage(page).loginAs(User.TELLER)
                .nav.customers()
                .newCustomer()
                .fill("Priya", lastName, TestData.uniqueEmail("priya"))
                .save();
        assertThat(customer.status()).hasText("Active");
        assertThat(customer.accountRows()).hasCount(0);
        long customerId = idFromUrl(page, "customers");

        // 2. Open a current account and a savings account
        AccountPage spending = customer.openAccount().open(null, "Current", "Spending");
        assertThat(spending.flash()).containsText("opened");
        long spendingId = idFromUrl(page, "accounts");

        AccountPage goals = customer.open(customerId).openAccount().open(null, "Savings", "Goals");
        long goalsId = idFromUrl(page, "accounts");

        // 3. Fund the current account and move some into savings
        spending.open(spendingId).deposit("500.00");
        assertThat(spending.balance).hasText("£500.00");
        spending.transfer("200.00", goalsId);
        assertThat(spending.balance).hasText("£300.00");

        // 4. The customer page summarises both accounts
        customer.open(customerId);
        assertThat(customer.accountRows()).hasCount(2);
        assertThat(customer.accountRows().filter(new Locator.FilterOptions().setHasText("Spending")))
                .containsText("£300.00");
        assertThat(customer.accountRows().filter(new Locator.FilterOptions().setHasText("Goals")))
                .containsText("£200.00");

        // 5. Savings cannot be closed while it holds money...
        goals.open(goalsId).clickAndConfirm("Close account");
        assertThat(goals.flash()).hasText("Account balance must be zero before it can be closed");

        // ...so move the money back, then close it
        goals.transfer("200.00", spendingId);
        assertThat(goals.balance).hasText("£0.00");
        goals.clickAndConfirm("Close account");
        assertThat(goals.status()).hasText("Closed");

        // 6. Verify the end state through the API
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            assertEquals(new BigDecimal("500.00"), api.balanceOf(spendingId));
            assertEquals("CLOSED", api.getAccount(goalsId).get("status").getAsString());
            assertEquals(3, api.transactions(spendingId).size()); // deposit, transfer out, transfer in
        }
    }

    /** Reads the numeric id from URLs like /customers/42 or /accounts/7. */
    private static long idFromUrl(Page page, String collection) {
        Matcher matcher = Pattern.compile("/" + collection + "/(\\d+)").matcher(page.url());
        if (!matcher.find()) {
            throw new AssertionError("Expected a /" + collection + "/{id} URL but was " + page.url());
        }
        return Long.parseLong(matcher.group(1));
    }
}
