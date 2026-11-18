package dev.upskill.ledger.ledger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Which planted faults are armed, and for which accounts. Empty unless a test arms one through TestingApi. */
@Component
public class Faults {

    private final Map<Fault, Set<Long>> armed = new ConcurrentHashMap<>();

    public void arm(Fault fault, long accountId) {
        armed.computeIfAbsent(fault, f -> ConcurrentHashMap.newKeySet()).add(accountId);
    }

    public void disarm(Fault fault, long accountId) {
        armed.computeIfPresent(fault, (f, accounts) -> {
            accounts.remove(accountId);
            return accounts;
        });
    }

    public void clear() {
        armed.clear();
    }

    public boolean isArmed(Fault fault, Long accountId) {
        return armed.getOrDefault(fault, Set.of()).contains(accountId);
    }
}
