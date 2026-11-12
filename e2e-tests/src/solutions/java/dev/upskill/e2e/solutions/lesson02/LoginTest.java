package dev.upskill.e2e.solutions.lesson02;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Lesson 02: actions (fill, click) and web-first assertions, using the login flow. */
@UsePlaywright
class LoginTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Test
    void tellerCanSignIn(Page page) {
        page.navigate(BASE_URL + "/login");

        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

        assertThat(page).hasURL(BASE_URL + "/");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard"))).isVisible();
        assertThat(page.getByTestId("current-user")).containsText("Terry Teller");
    }

    @Test
    void wrongPasswordShowsAnError(Page page) {
        page.navigate(BASE_URL + "/login");

        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("not-the-password");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).hasText("Invalid username or password");
        assertThat(page).hasURL(Pattern.compile(".*/login$"));
        // The username is kept so the user only has to retype the password
        assertThat(page.getByLabel("Username")).hasValue("teller");
        assertThat(page.getByLabel("Password")).isEmpty();
    }

    @Test
    void lockedUserCannotSignIn(Page page) {
        page.navigate(BASE_URL + "/login");

        page.getByLabel("Username").fill("locked");
        page.getByLabel("Password").fill("locked-pass");
        page.getByLabel("Password").press("Enter"); // submitting with the keyboard works too

        assertThat(page.getByRole(AriaRole.ALERT)).containsText("locked");
    }

    @Test
    void signedInUserCanSignOut(Page page) {
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign out")).click();

        assertThat(page.getByTestId("flash-message")).hasText("You have been signed out");
        page.navigate(BASE_URL + "/customers");
        assertThat(page).hasURL(Pattern.compile(".*/login$"));
    }
}
