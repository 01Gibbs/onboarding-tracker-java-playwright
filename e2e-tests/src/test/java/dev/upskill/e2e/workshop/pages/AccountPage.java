package dev.upskill.e2e.workshop.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Lesson 09: a single account at /accounts/{id}, with the "Move money" form and the transaction history. */
public class AccountPage {

    private final Page page;
    public final Locator balance;
    public final Locator amount;
    public final Locator toAccount;
    public final Locator description;
    public final Locator submit;

    public AccountPage(Page page) {
        this.page = page;
        // TODO lesson 09: balance by test id; the form fields by label; submit by role inside the "Move money" region
        this.balance = null;
        this.amount = null;
        this.toAccount = null;
        this.description = null;
        this.submit = null;
    }

    public AccountPage open(long accountId) {
        page.navigate("/accounts/" + accountId);
        return this;
    }

    public Locator status() {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public Locator flash() {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public Locator moveMoneySection() {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public AccountPage chooseType(String label) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public AccountPage deposit(String value) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public AccountPage withdraw(String value) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public AccountPage transfer(String value, long toAccountId) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public Locator transactionRows() {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public void filterTransactions(String typeLabel) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    public void click(String buttonName) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }

    /** Clicks a confirm-protected button and accepts the dialog. */
    public void clickAndConfirm(String buttonName) {
        throw new UnsupportedOperationException("TODO lesson 09");
    }
}
