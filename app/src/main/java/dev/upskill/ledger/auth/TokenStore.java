package dev.upskill.ledger.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory bearer tokens for the JSON API. Tokens live until the app restarts. */
@Component
public class TokenStore {

    private final Map<String, CurrentUser> tokens = new ConcurrentHashMap<>();

    public String issue(CurrentUser user) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, user);
        return token;
    }

    public Optional<CurrentUser> find(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    public void revoke(String token) {
        tokens.remove(token);
    }
}
