package dev.upskill.ledger.common;

import static org.assertj.core.api.Assertions.assertThat;

import dev.upskill.ledger.LedgerlineProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The text every front end shows for money. No Spring needed. */
class MoneyTest {

    private final Money money = new Money(new LedgerlineProperties(0, false, "£", List.of()));

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0          | £0.00",
            "5          | £5.00",
            "1250.5     | £1,250.50",
            "1000000.00 | £1,000,000.00",
            "-20        | -£20.00",
            "0.005      | £0.01",
    })
    void formatsWithSymbolGroupingAndPence(BigDecimal amount, String text) {
        assertThat(money.format(amount)).isEqualTo(text);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "25.5 | +£25.50",
            "-5   | -£5.00",
            "0    | £0.00",
    })
    void changesAreSignedSoMoneyInStandsOut(BigDecimal amount, String text) {
        assertThat(money.formatChange(amount)).isEqualTo(text);
    }

    @Test
    void nothingToFormatIsBlank() {
        assertThat(money.format(null)).isEmpty();
    }

    @Test
    void theSymbolComesFromConfiguration() {
        Money euros = new Money(new LedgerlineProperties(0, false, "€", List.of()));

        assertThat(euros.format(new BigDecimal("-3"))).isEqualTo("-€3.00");
        assertThat(euros.getSymbol()).isEqualTo("€");
    }
}
