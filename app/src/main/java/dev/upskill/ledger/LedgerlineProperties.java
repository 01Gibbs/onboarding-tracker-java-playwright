package dev.upskill.ledger;

import dev.upskill.ledger.auth.Role;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerline")
public record LedgerlineProperties(
        long processingDelayMs,
        boolean testSupportEnabled,
        String currencySymbol,
        List<UserConfig> users) {

    public record UserConfig(String username, String password, String displayName, Role role, boolean locked) {
    }
}
