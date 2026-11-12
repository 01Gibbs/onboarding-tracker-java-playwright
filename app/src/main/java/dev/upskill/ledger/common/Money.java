package dev.upskill.ledger.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.springframework.stereotype.Component;
import dev.upskill.ledger.LedgerlineProperties;

/** Formats amounts consistently, e.g. {@code £1,250.00} and {@code -£20.00}. Exposed to templates as {@code @money}. */
@Component("money")
public class Money {

    private final String symbol;

    public Money(LedgerlineProperties properties) {
        this.symbol = properties.currencySymbol();
    }

    public String getSymbol() {
        return symbol;
    }

    public String format(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.UK));
        String digits = format.format(amount.abs().setScale(2, RoundingMode.HALF_UP));
        return (amount.signum() < 0 ? "-" : "") + symbol + digits;
    }
}
