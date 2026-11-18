package dev.upskill.e2e.solutions.b0;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
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
import org.junit.jupiter.params.provider.EnumSource;

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

    /** The app's planted faults: where each one is armed, the movement it corrupts, and the rule it breaks. */
    enum PlantedFault {
        STALE_BALANCE_AFTER("stale-balance-after", Target.SOURCE, Invariant.RUNNING_BALANCE,
                (teller, from, to) -> teller.deposit(from, "10.00")),
        SKIP_WITHDRAWAL_ENTRY("skip-withdrawal-entry", Target.SOURCE, Invariant.BALANCE_MATCHES_ENTRIES,
                (teller, from, to) -> teller.withdraw(from, "10.00", "Lost entry")),
        TRANSFER_IN_SHORT("transfer-in-short", Target.DESTINATION, Invariant.TRANSFER_PAIRING,
                (teller, from, to) -> teller.transfer(from, to, "10.00")),
        UNPAIRED_TRANSFER("unpaired-transfer", Target.DESTINATION, Invariant.TRANSFER_PAIRING,
                (teller, from, to) -> teller.transfer(from, to, "10.00")),
        OVERDRAFT("overdraft", Target.SOURCE, Invariant.NO_NEGATIVE_BALANCE,
                (teller, from, to) -> teller.withdraw(from, "150.00", "Too much"));

        enum Target { SOURCE, DESTINATION }

        interface Movement {
            void make(LedgerApi teller, long from, long to);
        }

        final String name;
        final Target target;
        final Invariant breaks;
        final Movement movement;

        PlantedFault(String name, Target target, Invariant breaks, Movement movement) {
            this.name = name;
            this.target = target;
            this.breaks = breaks;
            this.movement = movement;
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(PlantedFault.class)
    void eachPlantedFaultIsCaughtAndNamed(PlantedFault fault) {
        assumeTrue(teller.testSupportEnabled(),
                "Planted faults need the app's dev profile: start it with ./mvnw -pl app spring-boot:run");
        long from = id(teller.accountWithBalance("100.00"));
        long to = id(teller.accountWithBalance("0"));

        long armedOn = fault.target == PlantedFault.Target.SOURCE ? from : to;
        teller.plantFault(fault.name, armedOn);
        try {
            fault.movement.make(teller, from, to);
        } finally {
            teller.clearFault(fault.name, armedOn);
        }

        List<Violation> violations = invariants.check(from, to);
        assertEquals(Set.of(fault.breaks), violations.stream().map(Violation::invariant).collect(toSet()),
                () -> "Expected only " + fault.breaks + " to break, but got " + violations);
        assertThrows(AssertionError.class, () -> invariants.assertBalanced(from, to));
    }

    private static long id(JsonObject json) {
        return json.get("id").getAsLong();
    }
}
