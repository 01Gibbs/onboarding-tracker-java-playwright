package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lesson 07: the "New customer" / "Edit customer" form. Fields are public so tests can assert on them. */
public class CustomerFormPage {

    private final Page page;
    public final Locator firstName;
    public final Locator lastName;
    public final Locator email;
    public final Locator phone;

    public CustomerFormPage(Page page) {
        this.page = page;
        // TODO lesson 07: locate each field by its label
        this.firstName = null;
        this.lastName = null;
        this.email = null;
        this.phone = null;
    }

    public CustomerFormPage fill(String first, String last, String emailAddress) {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    /** Saves and lands on the new customer's page. */
    public CustomerDetailPage save() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    /** Saves when you expect validation errors, so you stay on this form. */
    public CustomerFormPage saveExpectingErrors() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public Locator errorSummary() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }
}
