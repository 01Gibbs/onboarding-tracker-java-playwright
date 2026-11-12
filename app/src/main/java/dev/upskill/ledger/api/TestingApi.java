package dev.upskill.ledger.api;

import dev.upskill.ledger.LedgerlineProperties;
import dev.upskill.ledger.common.LedgerClock;
import dev.upskill.ledger.common.NotFoundException;
import dev.upskill.ledger.ledger.Fault;
import dev.upskill.ledger.ledger.Faults;
import dev.upskill.ledger.seed.DataSeeder;
import dev.upskill.ledger.seed.DataSeeder.SeedSummary;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Test-support hooks. Disabled (404) unless ledgerline.test-support-enabled=true, e.g. by the dev profile. */
@RestController
public class TestingApi {

    private final DataSeeder seeder;
    private final LedgerClock clock;
    private final Faults faults;
    private final LedgerlineProperties properties;

    public TestingApi(DataSeeder seeder, LedgerClock clock, Faults faults, LedgerlineProperties properties) {
        this.seeder = seeder;
        this.clock = clock;
        this.faults = faults;
        this.properties = properties;
    }

    @PostMapping("/api/testing/reset")
    public SeedSummary reset() {
        requireEnabled();
        faults.clear(); // account ids restart, so a fault armed for "account 7" must not hit the new account 7
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

    /** Arms a planted fault (mission B0) for one account; see Fault for what each one breaks. */
    @PutMapping("/api/testing/faults/{name}")
    public ArmedFault armFault(@PathVariable String name, @RequestBody ArmedFault request) {
        requireEnabled();
        faults.arm(fault(name), request.accountId());
        return new ArmedFault(name, request.accountId());
    }

    @DeleteMapping("/api/testing/faults/{name}")
    public ResponseEntity<Void> disarmFault(@PathVariable String name, @RequestParam long accountId) {
        requireEnabled();
        faults.disarm(fault(name), accountId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/testing/faults")
    public ResponseEntity<Void> clearFaults() {
        requireEnabled();
        faults.clear();
        return ResponseEntity.noContent().build();
    }

    public record ArmedFault(String name, long accountId) {
    }

    private static Fault fault(String name) {
        return Fault.fromSlug(name).orElseThrow(() -> new NotFoundException("Fault", name));
    }

    private void requireEnabled() {
        if (!properties.testSupportEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
