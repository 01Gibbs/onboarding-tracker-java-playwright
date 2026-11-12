package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The account list at /accounts. */
public class AccountsPage {

    private final Page page;

    public AccountsPage(Page page) {
        this.page = page;
    }

    public AccountsPage open() {
        page.navigate("/accounts");
        return this;
    }

    public AccountsPage filterByStatus(String statusLabel) {
        page.getByLabel("Status").selectOption(statusLabel);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters")).click();
        return this;
    }

    public Locator rows() {
        return page.getByTestId("account-row");
    }

    public Locator row(String containing) {
        return rows().filter(new Locator.FilterOptions().setHasText(containing));
    }
}
