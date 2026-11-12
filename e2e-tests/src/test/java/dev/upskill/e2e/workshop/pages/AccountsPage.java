package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lesson 07 (optional) / 14: the account list at /accounts. */
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
