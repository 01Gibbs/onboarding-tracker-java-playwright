package dev.upskill.e2e.workshop.lesson04;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lesson 04 starter. Use relative URLs only - the base URL comes from LedgerlineOptions. */
@UsePlaywright(LedgerlineOptions.class)
class OptionsAndHooksTest {

    @BeforeEach
    void signInAsTeller(Page page) {
        // TODO: sign in as teller using page.navigate("/login"), then assert current-user is visible
    }

    @Test
    void dashboardShowsSummaryCards(Page page) {
        abort("TODO lesson 04, exercise 2 (test 1)");
    }

    @Test
    void accountsPageListsSeededAccounts(Page page) {
        abort("TODO lesson 04, exercise 2 (test 2)");
    }
}
