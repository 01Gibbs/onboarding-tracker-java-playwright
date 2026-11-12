package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.workshop.support.User;

/**
 * Lesson 07 worked example: a complete page object. Use it as the pattern for the others in this package.
 * Locators are created once in the constructor (they're lazy, so this is free); methods express user intent;
 * methods that navigate return the page you land on.
 */
public class LoginPage {

    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator signIn;

    public LoginPage(Page page) {
        this.page = page;
        this.username = page.getByLabel("Username");
        this.password = page.getByLabel("Password");
        this.signIn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
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

    public DashboardPage loginAs(User user) {
        open();
        submit(user.username(), user.password());
        return new DashboardPage(page);
    }

    public Locator error() {
        return page.getByRole(AriaRole.ALERT);
    }
}
