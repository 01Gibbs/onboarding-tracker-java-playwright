package dev.upskill.e2e.solutions.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;

/** A single account at /accounts/{id}, including the "Move money" form and transaction history. */
public class AccountPage {

    private final Page page;
    public final Locator balance;
    public final Locator amount;
    public final Locator toAccount;
    public final Locator description;
    public final Locator submit;

    public AccountPage(Page page) {
        this.page = page;
        this.balance = page.getByTestId("account-balance");
        this.amount = page.getByLabel("Amount");
        this.toAccount = page.getByLabel("To account");
        this.description = page.getByLabel("Description");
        this.submit = page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName("Move money"))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Submit"));
    }

    public AccountPage open(long accountId) {
        page.navigate("/accounts/" + accountId);
        return this;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
    }

    public Locator status() {
        return heading().getByTestId("status-badge");
    }

    public Locator accountNumber() {
        return page.getByTestId("account-number");
    }

    public Locator flash() {
        return page.getByTestId("flash-message");
    }

    public Locator moveMoneySection() {
        return page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName("Move money"));
    }

    public AccountPage chooseType(String label) {
        page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName(label)).check();
        return this;
    }

    public AccountPage deposit(String value) {
        return move("Deposit", value);
    }

    public AccountPage withdraw(String value) {
        return move("Withdrawal", value);
    }

    public AccountPage transfer(String value, long toAccountId) {
        chooseType("Transfer");
        toAccount.selectOption(String.valueOf(toAccountId));
        amount.fill(value);
        submit.click();
        return this;
    }

    private AccountPage move(String type, String value) {
        chooseType(type);
        amount.fill(value);
        submit.click();
        return this;
    }

    public Locator transactionRows() {
        return page.getByTestId("transaction-row");
    }

    public void filterTransactions(String typeLabel) {
        page.getByLabel("Show").selectOption(new SelectOption().setLabel(typeLabel));
    }

    /** Clicks a confirm-protected action button (Close account, Delete account) and accepts the dialog. */
    public void clickAndConfirm(String buttonName) {
        page.onceDialog(dialog -> dialog.accept());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonName)).click();
    }

    public void click(String buttonName) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonName)).click();
    }
}
