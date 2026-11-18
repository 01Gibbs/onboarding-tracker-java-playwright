package dev.upskill.e2e.solutions.lesson09;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.AccountPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 09: arrange with the API, act in the UI, assert in the UI (and double-check with the API).
 * Every test gets its own freshly created account, so tests never interfere with each other.
 */
@UsePlaywright(LedgerlineOptions.class)
class MoveMoneyTest {

    private LedgerApi api;
    private AccountPage account;

    @BeforeEach
    void setUp(Playwright playwright, Page page) {
        api = LedgerApi.loginAs(playwright, User.TELLER);
        new LoginPage(page).loginAs(User.TELLER);
        account = new AccountPage(page);
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    @Test
    void depositIncreasesTheBalance() {
        long id = api.accountWithBalance("100.00").get("id").getAsLong();
        account.open(id);
        assertThat(account.balance).hasText("£100.00");

        account.deposit("25.50");

        assertThat(account.flash()).hasText("Deposit of £25.50 completed");
        assertThat(account.balance).hasText("£125.50");
        assertThat(account.transactionRows().first()).containsText("Deposit");
        assertThat(account.transactionRows().first()).containsText("+£25.50");
    }

    @Test
    void actionsWaitForTheServerBeforeContinuing() {
        long id = api.accountWithBalance("100.00").get("id").getAsLong();
        account.open(id);

        // The server deliberately takes ~750ms. click() waits for the resulting navigation to
        // commit, and hasText() keeps retrying - so no sleeps are needed anywhere.
        account.withdraw("10");

        assertThat(account.balance).hasText("£90.00");
    }

    @Test
    void withdrawingTooMuchShowsAnErrorAndChangesNothing() {
        long id = api.accountWithBalance("50.00").get("id").getAsLong();
        account.open(id);

        account.withdraw("50.01");

        assertThat(account.amount).hasAccessibleDescription("Insufficient funds. Available balance is £50.00");
        assertThat(account.amount).hasAttribute("aria-invalid", "true");
        assertThat(account.amount).hasValue("50.01");
        assertThat(account.balance).hasText("£50.00");
        assertEquals(new BigDecimal("50.00"), api.balanceOf(id));
    }

    @Test
    void amountsAreValidated() {
        long id = api.accountWithBalance("0").get("id").getAsLong();
        account.open(id);

        account.deposit("12.345");

        assertThat(account.amount).hasAccessibleDescription("Amount can have at most 2 decimal places");
    }

    @Test
    void transferFieldOnlyAppearsForTransfers() {
        long id = api.accountWithBalance("0").get("id").getAsLong();
        account.open(id);

        assertThat(account.toAccount).isHidden();
        account.chooseType("Transfer");
        assertThat(account.toAccount).isVisible();
        account.chooseType("Deposit");
        assertThat(account.toAccount).isHidden();
    }

    @Test
    void transferUpdatesBothAccounts(Page page) {
        long from = api.accountWithBalance("300.00").get("id").getAsLong();
        JsonObject target = api.accountWithBalance("0");
        long to = target.get("id").getAsLong();
        account.open(from);

        account.transfer("120.00", to);

        assertThat(account.flash()).containsText("Transfer of £120.00 to");
        assertThat(account.flash()).containsText(target.get("accountNumber").getAsString());
        assertThat(account.balance).hasText("£180.00");
        assertThat(account.transactionRows().first()).containsText("to " + target.get("accountNumber").getAsString());

        // Check the receiving side in the UI and through the API
        new AccountPage(page).open(to);
        assertThat(account.balance).hasText("£120.00");
        assertEquals(new BigDecimal("120.00"), api.balanceOf(to));
    }

    @Test
    void transferRequiresADestination() {
        long id = api.accountWithBalance("10.00").get("id").getAsLong();
        account.open(id);

        account.chooseType("Transfer");
        account.amount.fill("5");
        account.submit.click();

        assertThat(account.toAccount).isVisible(); // still visible after the round trip
        assertThat(account.toAccount).hasAttribute("aria-invalid", "true");
        assertThat(account.toAccount).hasAccessibleDescription("Choose an account to transfer to");
    }

    @Test
    void frozenAccountsCannotMoveMoney() {
        long id = api.accountWithBalance("10.00").get("id").getAsLong();
        api.setAccountStatus(id, "FROZEN");

        account.open(id);

        assertThat(account.status()).hasText("Frozen");
        assertThat(account.moveMoneySection()).isHidden();
    }

    @Test
    void freezeAndCloseThroughTheUi() {
        long id = api.accountWithBalance("0").get("id").getAsLong();
        account.open(id);

        account.click("Freeze account");
        assertThat(account.status()).hasText("Frozen");
        account.click("Unfreeze account");
        assertThat(account.status()).hasText("Active");

        account.clickAndConfirm("Close account");
        assertThat(account.status()).hasText("Closed");
        assertThat(account.flash()).containsText("is now closed");
    }

    @Test
    void filterTheTransactionHistory() {
        long id = api.accountWithBalance("100.00").get("id").getAsLong();
        api.withdraw(id, "30.00", "Cash");
        account.open(id);
        assertThat(account.transactionRows()).hasCount(2);

        account.filterTransactions("Withdrawal");

        assertThat(account.transactionRows()).hasCount(1);
        assertThat(account.transactionRows()).containsText("Cash");
    }
}
