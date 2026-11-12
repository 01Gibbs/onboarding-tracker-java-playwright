package dev.upskill.e2e.solutions.lesson05;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.TestData;
import org.junit.jupiter.api.Test;

/**
 * Lesson 05: a test that started life as a codegen recording, then was tidied up.
 *
 * <p>The raw recording looked roughly like this:
 * <pre>
 * page.navigate("http://localhost:8080/login");
 * page.getByLabel("Username").click();
 * page.getByLabel("Username").fill("teller");
 * page.getByLabel("Username").press("Tab");
 * page.getByLabel("Password").fill("teller-pass");
 * page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
 * page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Customers").setExact(true)).click();
 * page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New customer")).click();
 * page.getByLabel("First name").click();
 * page.getByLabel("First name").fill("Grace");
 * ...
 * page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save customer")).click();
 * assertThat(page.getByTestId("flash-message")).containsText("Customer Grace Hopper created");
 * </pre>
 * Codegen added setExact(true) because the dashboard's "Customers 5" summary card is also a link whose
 * name contains "Customers". We prefer scoping to the main navigation, which says what we mean.
 * What we changed: relative URLs, removed redundant click()/press("Tab"), unique test data
 * (so the test can run more than once), and assertions on the outcome rather than just the flash.
 */
@UsePlaywright(LedgerlineOptions.class)
class CreateCustomerRecordedTest {

    @Test
    void tellerCreatesACustomer(Page page) {
        String lastName = TestData.uniqueLastName();
        String email = TestData.uniqueEmail("grace");

        page.navigate("/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

        page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Main"))
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customers")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New customer")).click();
        page.getByLabel("First name").fill("Grace");
        page.getByLabel("Last name").fill(lastName);
        page.getByLabel("Email").fill(email);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save customer")).click();

        assertThat(page.getByTestId("flash-message")).hasText("Customer Grace " + lastName + " created");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
                .containsText("Grace " + lastName);
        assertThat(page.getByText(email)).isVisible();
    }
}
