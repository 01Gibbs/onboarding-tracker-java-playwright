package dev.upskill.ledger.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** "Now" for the whole app: the real time unless a test pins it. No Spring needed. */
class LedgerClockTest {

    private static final Instant PINNED = Instant.parse("2030-01-31T16:59:00Z");

    private final LedgerClock clock = new LedgerClock();

    @Test
    void followsTheSystemClockUntilPinned() {
        assertThat(Duration.between(clock.instant(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void staysAtThePinnedInstant() {
        clock.pin(PINNED);

        assertThat(clock.instant()).isEqualTo(PINNED);
        assertThat(clock.millis()).isEqualTo(PINNED.toEpochMilli());
    }

    @Test
    void releasingGoesBackToTheSystemClock() {
        clock.pin(PINNED);
        clock.release();

        assertThat(Duration.between(clock.instant(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void inAnotherZoneAPinnedClockStaysPinned() {
        clock.pin(PINNED);

        assertThat(clock.withZone(ZoneOffset.ofHours(9)).instant()).isEqualTo(PINNED);
        assertThat(clock.withZone(ZoneOffset.ofHours(9)).getZone()).isEqualTo(ZoneOffset.ofHours(9));
    }

    @Test
    void inAnotherZoneAnUnpinnedClockFollowsTheSystem() {
        assertThat(clock.withZone(ZoneOffset.UTC).getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(Duration.between(clock.withZone(ZoneOffset.UTC).instant(), Instant.now()).abs())
                .isLessThan(Duration.ofSeconds(5));
    }
}
