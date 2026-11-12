package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The customer list at /customers. */
public class CustomersPage {

    private final Page page;
    private final Locator table;

    public CustomersPage(Page page) {
        this.page = page;
        this.table = page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName("Customers"));
    }

    public CustomersPage open() {
        page.navigate("/customers");
        return this;
    }

    public CustomersPage search(String text) {
        page.getByRole(AriaRole.SEARCHBOX, new Page.GetByRoleOptions().setName("Search customers")).fill(text);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search")).click();
        return this;
    }

    public CustomerFormPage newCustomer() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New customer")).click();
        return new CustomerFormPage(page);
    }

    /** Table rows, excluding the header row. */
    public Locator rows() {
        return page.getByTestId("customer-row");
    }

    public Locator row(String containing) {
        return rows().filter(new Locator.FilterOptions().setHasText(containing));
    }

    public CustomerDetailPage openCustomer(String fullName) {
        table.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(fullName)).click();
        return new CustomerDetailPage(page);
    }

    public Locator emptyMessage() {
        return page.getByText("No customers match your search.");
    }
}
