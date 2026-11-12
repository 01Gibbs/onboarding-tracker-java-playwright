package dev.upskill.e2e.broken;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.AccountPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@UsePlaywright(BrokenSuiteOptions.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class Case09AccountLifecycleTest {

    private static Long accountId;

    @BeforeEach
    void signIn(Page page) {
        new LoginPage(page).loginAs(User.TELLER);
    }

    @Test
    void openAccount(Playwright playwright) {
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            accountId = api.accountWithBalance("0").get("id").getAsLong();
        }
    }

    @Test
    void depositIntoTheAccount(Page page) {
        AccountPage account = new AccountPage(page).open(accountId);

        account.deposit("75.00");

        assertThat(account.balance).hasText("£75.00");
    }
}
