package dev.upskill.ledger.ledger;

import dev.upskill.ledger.common.BusinessRuleException;
import java.math.BigDecimal;

/** An amount of money that is safe to move: always held to exactly 2 decimal places. */
public record Amount(BigDecimal value) {

    public static final BigDecimal MAX = new BigDecimal("1000000.00");

    /** Validates {@code raw} and normalises it, or throws a rule broken against the {@code amount} field. */
    public static Amount of(BigDecimal raw) {
        if (raw == null || raw.signum() <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero", "amount");
        }
        if (raw.compareTo(MAX) > 0) {
            throw new BusinessRuleException("Amount must not exceed 1,000,000.00", "amount");
        }
        // stripTrailingZeros so 10.500 and 1E+2 are judged by their value, not how they were written
        if (raw.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Amount can have at most 2 decimal places", "amount");
        }
        return new Amount(raw.setScale(2));
    }
}
