package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.support.User;

public class LoginPage {

    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator signIn;
    private final Locator error;

    public LoginPage(Page page) {
        this.page = page;
        this.username = page.getByLabel("Username");
        this.password = page.getByLabel("Password");
        this.signIn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
        this.error = page.getByRole(AriaRole.ALERT);
    }

    public LoginPage open() {
        page.navigate("/login");
        return this;
    }

    public void submit(String user, String pass) {
        username.fill(user);
        password.fill(pass);
        signIn.click();
    }

    /** Opens the login page, signs in and returns the dashboard. */
    public DashboardPage loginAs(User user) {
        open();
        submit(user.username(), user.password());
        return new DashboardPage(page);
    }

    public Locator error() {
        return error;
    }
}
