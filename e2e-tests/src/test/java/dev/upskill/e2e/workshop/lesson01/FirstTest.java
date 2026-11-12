package dev.upskill.e2e.workshop.lesson01;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Lesson 01 starter. The first test is a complete worked example: read it, run it, then do the TODOs.
 * A test that calls abort(...) is reported as SKIPPED - replace the abort line with your code.
 */
@UsePlaywright
class FirstTest {

    @Test
    void loginPageHasTheExpectedTitle(Page page) {
        page.navigate("http://localhost:8080/login");

        assertThat(page).hasTitle("Sign in · Ledgerline");
    }

    @Test
    void loginPageShowsItsHeading(Page page) {
        page.navigate("http://localhost:8080/login");

        // TODO exercise 2: assert that the heading "Sign in to Ledgerline" is visible.
        //  Hint: page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("..."))
        abort("TODO lesson 01, exercise 2");
    }

    @Test
    void anonymousVisitorsAreSentToTheLoginPage(Page page) {
        // TODO exercise 3: while signed out, navigate to http://localhost:8080/customers
        //  and assert the URL ends with /login.  Hint: assertThat(page).hasURL(Pattern.compile(...))
        abort("TODO lesson 01, exercise 3");
    }
}
