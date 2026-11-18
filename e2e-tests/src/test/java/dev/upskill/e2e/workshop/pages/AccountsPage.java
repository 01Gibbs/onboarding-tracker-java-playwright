package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** The account list at /accounts. Optional: complete it in lesson 07 if you want, or when a test of yours needs it. */
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
        throw new UnsupportedOperationException("TODO");
    }

    public Locator rows() {
        throw new UnsupportedOperationException("TODO");
    }
}
