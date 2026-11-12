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
class Case06FreezeAccountTest {

    @Test
    void tellerFreezesAnAccount(Page page, Playwright playwright) {
        long id;
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            id = api.accountWithBalance("10.00").get("id").getAsLong();
        }
        new LoginPage(page).loginAs(User.AUDITOR);
        AccountPage account = new AccountPage(page).open(id);

        account.click("Freeze account");

        assertThat(account.status()).hasText("Frozen");
    }
}
