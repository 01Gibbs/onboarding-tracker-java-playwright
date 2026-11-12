package dev.upskill.e2e.workshop.lesson02;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Lesson 02 starter. The actions for the first test are done for you; you write the assertions. */
@UsePlaywright
class LoginTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Test
    void tellerCanSignIn(Page page) {
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();

        // TODO exercise 1: assert
        //  - the URL is BASE_URL + "/"
        //  - the "Dashboard" heading is visible
        //  - getByTestId("current-user") contains "Terry Teller"
        abort("TODO lesson 02, exercise 1");
    }

    @Test
    void wrongPasswordShowsAnError(Page page) {
        // TODO exercise 2: sign in with a wrong password, then assert
        //  - the alert (getByRole(AriaRole.ALERT)) has exactly the text "Invalid username or password"
        //  - you are still on /login
        //  - Username still has the value "teller" and Password is empty
        abort("TODO lesson 02, exercise 2");
    }

    @Test
    void lockedUserCannotSignIn(Page page) {
        // TODO exercise 3: sign in as locked / locked-pass by pressing Enter in the password field.
        //  Assert the alert mentions "locked".
        abort("TODO lesson 02, exercise 3");
    }

    @Test
    void signedInUserCanSignOut(Page page) {
        // TODO exercise 4: sign in, click "Sign out", assert the flash message, then prove /customers
        //  sends you back to the login page.
        abort("TODO lesson 02, exercise 4");
    }
}
