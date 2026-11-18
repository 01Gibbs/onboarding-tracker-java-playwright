package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The header shown on every signed-in page. */
public class NavBar {

    private final Page page;
    private final Locator nav;

    public NavBar(Page page) {
        this.page = page;
        this.nav = page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Main"));
    }

    public CustomersPage customers() {
        nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Customers")).click();
        return new CustomersPage(page);
    }

    public AccountsPage accounts() {
        nav.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Accounts")).click();
        return new AccountsPage(page);
    }

    public Locator currentUser() {
        return page.getByTestId("current-user");
    }

    public void signOut() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign out")).click();
    }
}
