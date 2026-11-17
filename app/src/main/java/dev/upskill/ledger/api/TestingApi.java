package dev.upskill.ledger.api;

import dev.upskill.ledger.common.LedgerClock;
import dev.upskill.ledger.seed.DataSeeder;
import dev.upskill.ledger.seed.DataSeeder.SeedSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-support hooks, only registered when ledgerline.test-support-enabled=true (e.g. by the dev profile).
 * Otherwise ApiFallback answers 404, so nothing (not even input validation) reveals that they exist.
 */
@RestController
@ConditionalOnProperty(prefix = "ledgerline", name = "test-support-enabled", havingValue = "true")
public class TestingApi {

    private final DataSeeder seeder;
    private final LedgerClock clock;

    public TestingApi(DataSeeder seeder, LedgerClock clock) {
        this.seeder = seeder;
        this.clock = clock;
    }

    @PostMapping("/api/testing/reset")
    public SeedSummary reset() {
        return seeder.reset();
    }

    /** Pins the server's "now" until released. The browser's clock is separate: see page.clock(). */
    @PutMapping("/api/testing/clock")
    public ClockState pinClock(@Valid @RequestBody ClockState request) {
        clock.pin(request.instant());
        return new ClockState(clock.instant());
    }

    @DeleteMapping("/api/testing/clock")
    public ResponseEntity<Void> releaseClock() {
        clock.release();
        return ResponseEntity.noContent().build();
    }

    public record ClockState(@NotNull(message = "instant is required") Instant instant) {
    }
}
