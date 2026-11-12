package dev.upskill.e2e.solutions.b0;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.solutions.pages.AccountPage;
import dev.upskill.e2e.solutions.pages.LoginPage;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.LedgerInvariants;
import dev.upskill.e2e.solutions.support.LedgerInvariants.Invariant;
import dev.upskill.e2e.solutions.support.LedgerInvariants.Violation;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.User;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Mission B0: one reusable check that the books balance, proven against faults planted in the app. */
@UsePlaywright(LedgerlineOptions.class)
class LedgerInvariantsTest {

    LedgerApi teller;
    LedgerApi auditor;
    LedgerInvariants invariants;

    @BeforeEach
    void setUp(Playwright playwright) {
        teller = LedgerApi.loginAs(playwright, User.TELLER);
        // The checker reads with a read-only login, so it can never change the ledger it is checking.
        auditor = LedgerApi.loginAs(playwright, User.AUDITOR);
        invariants = new LedgerInvariants(auditor);
    }

    @AfterEach
    void tearDown() {
        teller.close();
        auditor.close();
    }

    @Test
    void anAccountWithNoHistoryIsBalanced() {
        long account = id(teller.accountWithBalance("0"));

        invariants.assertBalanced(account);
    }

    @Test
    void depositsWithdrawalsAndTransfersStayBalanced() {
        long from = id(teller.accountWithBalance("100.00"));
        long to = id(teller.accountWithBalance("0"));

        teller.withdraw(from, "30.25", "Rent share");
        teller.transfer(from, to, "19.75");
        teller.deposit(to, "0.01");

        invariants.assertBalanced(from, to);
    }

    @Test
    void aTransferMadeInTheUiIsBalanced(Page page) {
        long from = id(teller.accountWithBalance("100.00"));
        long to = id(teller.accountWithBalance("0"));
        new LoginPage(page).open().loginAs(User.TELLER);

        AccountPage account = new AccountPage(page).open(from).transfer("40.00", to);
        assertThat(account.flash()).containsText("Transfer of £40.00");

        // Act in the UI, verify the books through the API.
        invariants.assertBalanced(from, to);
    }

    @ParameterizedTest(name = "{0} breaks {1}")
    @CsvSource({
            "stale-balance-after,   RUNNING_BALANCE",
            "skip-withdrawal-entry, BALANCE_MATCHES_ENTRIES",
            "transfer-in-short,     TRANSFER_PAIRING",
            "unpaired-transfer,     TRANSFER_PAIRING",
            "overdraft,             NO_NEGATIVE_BALANCE",
    })
    void eachPlantedFaultIsCaughtAndNamed(String fault, Invariant broken) {
        assumeTrue(teller.testSupportEnabled(),
                "Planted faults need the app's dev profile: start it with ./mvnw -pl app spring-boot:run");
        long from = id(teller.accountWithBalance("100.00"));
        long to = id(teller.accountWithBalance("0"));

        breakTheLedger(fault, from, to);

        List<Violation> violations = invariants.check(from, to);
        assertEquals(Set.of(broken), violations.stream().map(Violation::invariant).collect(toSet()),
                () -> "Expected only " + broken + " to break, but got " + violations);
        assertThrows(AssertionError.class, () -> invariants.assertBalanced(from, to));
    }

    /** Arms the fault on the account it targets, makes the one movement it corrupts, then disarms it. */
    private void breakTheLedger(String fault, long from, long to) {
        long armedOn = fault.contains("transfer") ? to : from;
        teller.plantFault(fault, armedOn);
        try {
            switch (fault) {
                case "stale-balance-after" -> teller.deposit(from, "10.00");
                case "skip-withdrawal-entry" -> teller.withdraw(from, "10.00", "Lost entry");
                case "overdraft" -> teller.withdraw(from, "150.00", "Too much");
                default -> teller.transfer(from, to, "10.00");
            }
        } finally {
            teller.clearFault(fault, armedOn);
        }
    }

    private static long id(com.google.gson.JsonObject json) {
        return json.get("id").getAsLong();
    }
}
