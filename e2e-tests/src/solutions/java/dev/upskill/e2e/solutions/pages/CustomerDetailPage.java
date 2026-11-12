package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

/** A single customer at /customers/{id}. */
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
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
    }

    public Locator status() {
        return heading().getByTestId("status-badge");
    }

    public Locator flash() {
        return page.getByTestId("flash-message");
    }

    /** The value shown for a term in the details list, e.g. detail("Email"). Each pair is a group named by its term. */
    public Locator detail(String term) {
        return page.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName(term).setExact(true))
                .getByRole(AriaRole.DEFINITION);
    }

    public CustomerFormPage edit() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Edit")).click();
        return new CustomerFormPage(page);
    }

    public void toggleSuspension() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(
                Pattern.compile("^(Suspend|Reactivate) customer$"))).click();
    }

    /** Clicks "Delete customer" and accepts the browser confirmation dialog. */
    public void deleteAndConfirm() {
        page.onceDialog(dialog -> dialog.accept());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete customer")).click();
    }

    public void click(String buttonName) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonName)).click();
    }

    public OpenAccountPage openAccount() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Open account")).click();
        return new OpenAccountPage(page);
    }

    public Locator accountRows() {
        return page.getByTestId("account-row");
    }
}
