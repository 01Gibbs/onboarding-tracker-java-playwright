package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lesson 07: the customer list at /customers. */
public class CustomersPage {

    private final Page page;

    public CustomersPage(Page page) {
        this.page = page;
    }

    public CustomersPage open() {
        page.navigate("/customers");
        return this;
    }

    public CustomersPage search(String text) {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public CustomerFormPage newCustomer() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public Locator rows() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    /** Rows that contain the given text. */
    public Locator row(String containing) {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public CustomerDetailPage openCustomer(String fullName) {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public Locator emptyMessage() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }
}
