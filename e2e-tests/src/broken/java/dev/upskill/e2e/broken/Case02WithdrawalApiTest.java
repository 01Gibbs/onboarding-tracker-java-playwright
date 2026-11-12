package dev.upskill.e2e.broken;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@UsePlaywright(BrokenSuiteOptions.class)
class Case02WithdrawalApiTest {

    @Test
    void withdrawalReducesTheBalance(Playwright playwright) {
        try (LedgerApi api = LedgerApi.loginAs(playwright, User.TELLER)) {
            long id = api.accountWithBalance("100.00").get("id").getAsLong();

            api.withdraw(id, "10.00", "Cash");

            assertEquals(new BigDecimal("90"), api.balanceOf(id));
        }
    }
}
