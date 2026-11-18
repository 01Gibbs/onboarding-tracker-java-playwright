package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The "New customer" and "Edit customer" form. */
public class CustomerFormPage {

    private final Page page;
    public final Locator firstName;
    public final Locator lastName;
    public final Locator email;
    public final Locator phone;

    public CustomerFormPage(Page page) {
        this.page = page;
        this.firstName = page.getByLabel("First name");
        this.lastName = page.getByLabel("Last name");
        this.email = page.getByLabel("Email");
        this.phone = page.getByLabel("Phone");
    }

    public CustomerFormPage fill(String first, String last, String emailAddress) {
        firstName.fill(first);
        lastName.fill(last);
        email.fill(emailAddress);
        return this;
    }

    public CustomerDetailPage save() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save customer")).click();
        return new CustomerDetailPage(page);
    }

    /** Clicks save when we expect the form to be shown again with errors. */
    public CustomerFormPage saveExpectingErrors() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save customer")).click();
        return this;
    }

    public Locator errorSummary() {
        return page.getByTestId("error-summary");
    }
}
