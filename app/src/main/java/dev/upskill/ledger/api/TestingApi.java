package dev.upskill.ledger.api;

import dev.upskill.ledger.LedgerlineProperties;
import dev.upskill.ledger.common.LedgerClock;
import dev.upskill.ledger.seed.DataSeeder;
import dev.upskill.ledger.seed.DataSeeder.SeedSummary;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Test-support hooks. Disabled (404) unless ledgerline.test-support-enabled=true, e.g. by the dev profile. */
@RestController
public class TestingApi {

    private final DataSeeder seeder;
    private final LedgerClock clock;
    private final LedgerlineProperties properties;

    public TestingApi(DataSeeder seeder, LedgerClock clock, LedgerlineProperties properties) {
        this.seeder = seeder;
        this.clock = clock;
        this.properties = properties;
    }

    @PostMapping("/api/testing/reset")
    public SeedSummary reset() {
        requireEnabled();
        return seeder.reset();
    }

    /** Pins the server's "now" until released. The browser's clock is separate: see page.clock(). */
    @PutMapping("/api/testing/clock")
    public ClockState pinClock(@RequestBody ClockState request) {
        requireEnabled();
        clock.pin(request.instant());
        return new ClockState(clock.instant());
    }

    @DeleteMapping("/api/testing/clock")
    public ResponseEntity<Void> releaseClock() {
        requireEnabled();
        clock.release();
        return ResponseEntity.noContent().build();
    }

    public record ClockState(Instant instant) {
    }

    private void requireEnabled() {
        if (!properties.testSupportEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
