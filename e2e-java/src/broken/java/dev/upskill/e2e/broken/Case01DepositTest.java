package dev.upskill.e2e.broken;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.AccountPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.Test;

@UsePlaywright(BrokenSuiteOptions.class)
class Case01DepositTest {

    @Test
    void depositShowsTheNewBalance(Page page, Playwright playwright) {
        long id;
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            id = api.accountWithBalance("1000.00").get("id").getAsLong();
        }
        new LoginPage(page).loginAs(User.TELLER);
        AccountPage account = new AccountPage(page).open(id);

        account.deposit("250.00");

        assertThat(account.balance).hasText("£1250.00");
    }
}
