package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lesson 07: a single customer at /customers/{id}. */
public class CustomerDetailPage {

    private final Page page;

    public CustomerDetailPage(Page page) {
        this.page = page;
    }

    public CustomerDetailPage open(long customerId) {
        page.navigate("/customers/" + customerId);
        return this;
    }

    public NavBar nav() {
        return new NavBar(page);
    }

    public Locator heading() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public Locator status() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public Locator flash() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    /**
     * The value shown for a term in the details list, e.g. detail("Phone").
     * Hint: each term/value pair is a group named by its term - getByRole(GROUP, ...).getByRole(DEFINITION).
     */
    public Locator detail(String term) {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public CustomerFormPage edit() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }

    public void deleteAndConfirm() {
        throw new UnsupportedOperationException("TODO lesson 07");
    }
}
