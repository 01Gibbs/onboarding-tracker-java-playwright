package dev.upskill.ledger.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.upskill.ledger.common.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The one place that decides what a valid amount of money is. No Spring needed. */
class AmountTest {

    @Test
    void exponentNotationIsNormalisedToPence() {
        assertThat(Amount.of(new BigDecimal("1E+2")).value().toPlainString()).isEqualTo("100.00");
    }

    @Test
    void moreThanTwoDecimalPlacesIsRejectedAgainstTheAmountField() {
        assertThatThrownBy(() -> Amount.of(new BigDecimal("12.345")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Amount can have at most 2 decimal places")
                .extracting("field").isEqualTo("amount");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "null,        Amount must be greater than zero",
            "0,           Amount must be greater than zero",
            "-5.00,       Amount must be greater than zero",
            "1000000.01,  'Amount must not exceed 1,000,000.00'",
    })
    void outOfRangeAmountsAreRejected(BigDecimal raw, String message) {
        assertThatThrownBy(() -> Amount.of(raw))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(message);
    }

    @Test
    void theLimitItselfIsAllowed() {
        assertThat(Amount.of(new BigDecimal("1000000")).value()).isEqualByComparingTo("1000000.00");
    }
}
