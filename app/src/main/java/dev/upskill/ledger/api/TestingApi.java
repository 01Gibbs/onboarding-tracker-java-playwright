package dev.upskill.ledger.api;

import dev.upskill.ledger.LedgerlineProperties;
import dev.upskill.ledger.seed.DataSeeder;
import dev.upskill.ledger.seed.DataSeeder.SeedSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Test-support hooks. Disabled unless ledgerline.test-support-enabled=true. */
@RestController
public class TestingApi {

    private final DataSeeder seeder;
    private final LedgerlineProperties properties;

    public TestingApi(DataSeeder seeder, LedgerlineProperties properties) {
        this.seeder = seeder;
        this.properties = properties;
    }

    @PostMapping("/api/testing/reset")
    public SeedSummary reset() {
        if (!properties.testSupportEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return seeder.reset();
    }
}
