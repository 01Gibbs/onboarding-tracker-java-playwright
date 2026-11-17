package dev.upskill.ledger.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** A ledger line knows which way the money went. No Spring needed. */
class LedgerEntryTest {

    @ParameterizedTest
    @CsvSource({
            "DEPOSIT,      12.50",
            "TRANSFER_IN,  12.50",
            "WITHDRAWAL,   -12.50",
            "TRANSFER_OUT, -12.50",
    })
    void moneyInIsPositiveAndMoneyOutIsNegative(EntryType type, BigDecimal signed) {
        Account account = new Account(null, AccountType.CURRENT, "Unit", Instant.EPOCH);
        LedgerEntry entry = new LedgerEntry(account, type, new BigDecimal("12.50"), "Unit", null, null, Instant.EPOCH);

        assertThat(entry.getSignedAmount()).isEqualByComparingTo(signed);
    }
}
