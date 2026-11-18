package dev.upskill.ledger.auth;

import dev.upskill.ledger.LedgerlineProperties;
import dev.upskill.ledger.LedgerlineProperties.UserConfig;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The fixed set of users configured in application.yml. Plain-text passwords: this is a training app. */
@Component
public class UserDirectory {

    public sealed interface LoginResult {
        record Success(CurrentUser user) implements LoginResult {
        }

        record Failure(String message) implements LoginResult {
        }
    }

    private final LedgerlineProperties properties;

    public UserDirectory(LedgerlineProperties properties) {
        this.properties = properties;
    }

    /** Spaces around the username are ignored, however someone signs in; the password must match exactly. */
    public LoginResult authenticate(String username, String password) {
        String name = username.strip();
        Optional<UserConfig> match = properties.users().stream()
                .filter(u -> u.username().equals(name) && u.password().equals(password))
                .findFirst();
        if (match.isEmpty()) {
            return new LoginResult.Failure("Invalid username or password");
        }
        UserConfig user = match.get();
        if (user.locked()) {
            return new LoginResult.Failure("This user is locked. Contact an administrator.");
        }
        return new LoginResult.Success(new CurrentUser(user.username(), user.displayName(), user.role()));
    }
}
