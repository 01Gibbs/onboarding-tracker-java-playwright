package dev.upskill.ledger.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * The app's only source of "now". Follows the system clock unless a test pins it through
 * PUT /api/testing/clock, so server-side time rules can be tested without waiting.
 */
@Component
public class LedgerClock extends Clock {

    private final Clock system = Clock.systemDefaultZone();
    private final AtomicReference<Instant> pinned = new AtomicReference<>();

    public void pin(Instant instant) {
        pinned.set(instant);
    }

    public void release() {
        pinned.set(null);
    }

    @Override
    public Instant instant() {
        Instant fixed = pinned.get();
        return fixed != null ? fixed : system.instant();
    }

    @Override
    public ZoneId getZone() {
        return system.getZone();
    }

    /** A snapshot in another zone: pinned if pinned, otherwise the system clock. */
    @Override
    public Clock withZone(ZoneId zone) {
        Instant fixed = pinned.get();
        return fixed != null ? Clock.fixed(fixed, zone) : system.withZone(zone);
    }
}
