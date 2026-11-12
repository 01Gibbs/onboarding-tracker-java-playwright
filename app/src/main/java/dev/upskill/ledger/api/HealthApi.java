package dev.upskill.ledger.api;

import java.util.Map;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Readiness for scripts and CI. The port opens before the seed data is loaded, so this answers
 * 503 until Spring Boot marks the app ready, which happens after DataSeeder has run.
 */
@RestController
public class HealthApi {

    private final ApplicationAvailability availability;

    public HealthApi(ApplicationAvailability availability) {
        this.availability = availability;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        if (availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC) {
            return ResponseEntity.ok(Map.of("status", "UP"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "STARTING"));
    }
}
