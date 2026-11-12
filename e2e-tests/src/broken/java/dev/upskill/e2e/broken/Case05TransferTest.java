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
class Case05TransferTest {

    @Test
    void transferMovesMoneyToAnotherAccount(Page page, Playwright playwright) {
        long from;
        long to;
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            from = api.accountWithBalance("100.00").get("id").getAsLong();
            to = api.accountWithBalance("0").get("id").getAsLong();
        }
        new LoginPage(page).loginAs(User.TELLER);
        AccountPage account = new AccountPage(page).open(from);

        account.toAccount.selectOption(String.valueOf(to));
        account.chooseType("Transfer");
        account.amount.fill("40.00");
        account.submit.click();

        assertThat(account.balance).hasText("£60.00");
    }
}
