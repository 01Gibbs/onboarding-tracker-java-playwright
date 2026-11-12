package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lesson 07: the header on every signed-in page. Scope your locators to the "Main" navigation. */
public class NavBar {

    private final Page page;

    public NavBar(Page page) {
        this.page = page;
    }

    public CustomersPage customers() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public AccountsPage accounts() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public Locator currentUser() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public void signOut() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }
}
