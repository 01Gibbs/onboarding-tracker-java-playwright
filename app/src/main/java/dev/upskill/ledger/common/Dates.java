package dev.upskill.ledger.common;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Formats timestamps for display in the server's time zone. Exposed to templates as {@code @dates}. */
@Component("dates")
public class Dates {

    private final Clock clock;

    public Dates(Clock clock) {
        this.clock = clock;
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.UK);

    public String date(Instant instant) {
        return instant == null ? "" : DATE.format(instant.atZone(clock.getZone()));
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(clock.getZone()));
    }
}
