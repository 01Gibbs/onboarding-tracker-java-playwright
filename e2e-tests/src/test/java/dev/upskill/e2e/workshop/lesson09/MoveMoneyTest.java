package dev.upskill.e2e.workshop.lesson09;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 09 starter: arrange with LedgerApi, act and assert through AccountPage. Set both up in @BeforeEach. */
@UsePlaywright(LedgerlineOptions.class)
class MoveMoneyTest {

    @Test
    void depositIncreasesTheBalance() {
        abort("TODO lesson 09, exercise 1");
    }

    @Test
    void actionsWaitForTheServerBeforeContinuing() {
        abort("TODO lesson 09, exercise 2");
    }

    @Test
    void withdrawingTooMuchShowsAnErrorAndChangesNothing() {
        abort("TODO lesson 09, exercise 3");
    }

    @Test
    void amountsAreValidated() {
        abort("TODO lesson 09, exercise 4");
    }

    @Test
    void transferFieldOnlyAppearsForTransfers() {
        abort("TODO lesson 09, exercise 5");
    }

    @Test
    void transferUpdatesBothAccounts(Page page) {
        abort("TODO lesson 09, exercise 6");
    }

    @Test
    void transferRequiresADestination() {
        abort("TODO lesson 09, exercise 7");
    }

    @Test
    void frozenAccountsCannotMoveMoney() {
        abort("TODO lesson 09, exercise 8");
    }

    @Test
    void freezeAndCloseThroughTheUi() {
        abort("TODO lesson 09, exercise 9");
    }

    @Test
    void filterTheTransactionHistory() {
        abort("TODO lesson 09, exercise 10");
    }
}
