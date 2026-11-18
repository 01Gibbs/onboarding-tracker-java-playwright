package dev.upskill.e2e.solutions.lesson04;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import java.util.regex.Pattern;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Lesson 04: every test gets a brand-new BrowserContext (an isolated, incognito-like profile).
 * The order is fixed only to prove the point: the second test does NOT see the first test's login.
 */
@UsePlaywright(LedgerlineOptions.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IsolationTest {

    @Test
    @Order(1)
    void firstTestSignsIn(Page page, BrowserContext context) {
        page.navigate("/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

        assertThat(page.getByTestId("current-user")).isVisible();
        assertTrue(context.cookies().stream().anyMatch(c -> c.name.equals("JSESSIONID")));
    }

    @Test
    @Order(2)
    void secondTestStartsSignedOut(Page page, BrowserContext context) {
        assertTrue(context.cookies().isEmpty(), "a new context has no cookies");

        page.navigate("/customers");

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
    }
}
