package dev.upskill.e2e.workshop.lesson08;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 08 starter, part 2: use your LedgerApi. Create it in @BeforeEach (inject Playwright), close it in @AfterEach. */
@UsePlaywright(LedgerlineOptions.class)
class AccountApiTest {

    @Test
    void newAccountsStartActiveWithZeroBalance() {
        abort("TODO lesson 08, exercise 7");
    }

    @Test
    void depositsAndWithdrawalsUpdateTheBalance() {
        abort("TODO lesson 08, exercise 8");
    }

    @Test
    void cannotWithdrawMoreThanTheBalance() {
        abort("TODO lesson 08, exercise 9");
    }

    @Test
    void transfersMoveMoneyBetweenAccounts() {
        abort("TODO lesson 08, exercise 10");
    }

    @Test
    void frozenAccountsRejectDeposits() {
        abort("TODO lesson 08, exercise 11");
    }

    @Test
    void accountsWithMoneyCannotBeClosed() {
        abort("TODO lesson 08, exercise 12");
    }
}
