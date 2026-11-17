package dev.upskill.ledger.api;

import dev.upskill.ledger.common.LedgerClock;
import dev.upskill.ledger.common.NotFoundException;
import dev.upskill.ledger.ledger.Fault;
import dev.upskill.ledger.ledger.Faults;
import dev.upskill.ledger.seed.DataSeeder;
import dev.upskill.ledger.seed.DataSeeder.SeedSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final Faults faults;

    public TestingApi(DataSeeder seeder, LedgerClock clock, Faults faults) {
        this.seeder = seeder;
        this.clock = clock;
        this.faults = faults;
    }

    @PostMapping("/api/testing/reset")
    public SeedSummary reset() {
        faults.clear(); // account ids restart, so a fault armed for "account 7" must not hit the new account 7
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

    /** Arms a planted fault (mission B0) for one account; see Fault for what each one breaks. */
    @PutMapping("/api/testing/faults/{name}")
    public ArmedFault armFault(@PathVariable String name, @RequestBody ArmedFault request) {
        faults.arm(fault(name), request.accountId());
        return new ArmedFault(name, request.accountId());
    }

    @DeleteMapping("/api/testing/faults/{name}")
    public ResponseEntity<Void> disarmFault(@PathVariable String name, @RequestParam long accountId) {
        faults.disarm(fault(name), accountId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/testing/faults")
    public ResponseEntity<Void> clearFaults() {
        faults.clear();
        return ResponseEntity.noContent().build();
    }

    public record ArmedFault(String name, long accountId) {
    }

    private static Fault fault(String name) {
        return Fault.fromSlug(name).orElseThrow(() -> new NotFoundException("Fault", name));
    }
}
