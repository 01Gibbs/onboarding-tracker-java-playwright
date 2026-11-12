package dev.upskill.e2e.workshop.lesson03;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lesson 03 starter. Sign-in is provided; each test's goal is in lessons/03-locators.md. */
@UsePlaywright
class LocatorsTest {

    // -DbaseUrl=... points the tests at another environment (lesson 04 tidies this up).
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    @BeforeEach
    void signIn(Page page) {
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
    }

    @Test
    void navigateUsingTheMainNavigation(Page page) {
        abort("TODO lesson 03, exercise 1");
    }

    @Test
    void searchForACustomerByPlaceholder(Page page) {
        abort("TODO lesson 03, exercise 2");
    }

    @Test
    void filterTableRowsAndChainLocators(Page page) {
        abort("TODO lesson 03, exercise 3");
    }

    @Test
    void findByTextAndExactMatching(Page page) {
        abort("TODO lesson 03, exercise 4");
    }

    @Test
    void everyFrozenAccountShowsAFrozenBadge(Page page) {
        abort("TODO lesson 03, exercise 5");
    }

    @Test
    void openAnAccountFromTheCustomersPage(Page page) {
        abort("TODO lesson 03, exercise 6");
    }
}
