package dev.upskill.e2e.solutions.lesson01;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Lesson 01: the smallest useful Playwright tests. */
@UsePlaywright
class FirstTest {

    // The app's address. -DbaseUrl=... points the tests at another environment.
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    @Test
    void loginPageHasTheExpectedTitle(Page page) {
        page.navigate(BASE_URL + "/login");

        assertThat(page).hasTitle("Sign in · Ledgerline");
    }

    @Test
    void loginPageShowsItsHeading(Page page) {
        page.navigate(BASE_URL + "/login");

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Sign in to Ledgerline")))
                .isVisible();
    }

    @Test
    void anonymousVisitorsAreSentToTheLoginPage(Page page) {
        page.navigate(BASE_URL + "/customers");

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
    }
}
