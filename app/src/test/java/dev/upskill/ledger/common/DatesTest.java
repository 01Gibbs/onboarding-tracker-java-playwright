package dev.upskill.ledger.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Dates are shown in the server's zone, UK style. No Spring needed. */
class DatesTest {

    private static final Instant NEW_YEAR_IN_UTC = Instant.parse("2030-12-31T23:30:00Z");

    @Test
    void showsTheDayAndTimeInTheServersZone() {
        Dates london = new Dates(Clock.system(ZoneId.of("Europe/London")));
        Dates tokyo = new Dates(Clock.system(ZoneId.of("Asia/Tokyo")));

        assertThat(london.date(NEW_YEAR_IN_UTC)).isEqualTo("31 Dec 2030");
        assertThat(london.dateTime(NEW_YEAR_IN_UTC)).isEqualTo("31 Dec 2030 23:30");
        assertThat(tokyo.date(NEW_YEAR_IN_UTC)).isEqualTo("1 Jan 2031");
        assertThat(tokyo.dateTime(NEW_YEAR_IN_UTC)).isEqualTo("1 Jan 2031 08:30");
    }

    @Test
    void nothingToShowIsBlank() {
        Dates dates = new Dates(Clock.systemUTC());

        assertThat(dates.date(null)).isEmpty();
        assertThat(dates.dateTime(null)).isEmpty();
    }
}
