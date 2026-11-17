package dev.upskill.ledger.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.upskill.ledger.LedgerlineProperties;
import dev.upskill.ledger.LedgerlineProperties.UserConfig;
import dev.upskill.ledger.auth.UserDirectory.LoginResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Who may sign in, and as what. No Spring needed. */
class UserDirectoryTest {

    private final UserDirectory users = new UserDirectory(new LedgerlineProperties(0, false, "£", List.of(
            new UserConfig("teller", "teller-pass", "Terry Teller", Role.TELLER, false),
            new UserConfig("auditor", "auditor-pass", "Alex Auditor", Role.AUDITOR, false),
            new UserConfig("locked", "locked-pass", "Lee Locked", Role.TELLER, true))));

    @Test
    void theRightPasswordSignsYouInWithYourRole() {
        assertThat(users.authenticate("auditor", "auditor-pass"))
                .isEqualTo(new LoginResult.Success(new CurrentUser("auditor", "Alex Auditor", Role.AUDITOR)));
    }

    @Test
    void aWrongPasswordOrUnknownUserGetsTheSameAnswer() {
        LoginResult invalid = new LoginResult.Failure("Invalid username or password");

        assertThat(users.authenticate("teller", "auditor-pass")).isEqualTo(invalid);
        assertThat(users.authenticate("nobody", "teller-pass")).isEqualTo(invalid);
        assertThat(users.authenticate("Teller", "teller-pass")).isEqualTo(invalid);
    }

    @Test
    void aLockedUserIsToldTheyAreLocked() {
        assertThat(users.authenticate("locked", "locked-pass"))
                .isEqualTo(new LoginResult.Failure("This user is locked. Contact an administrator."));
    }

    @Test
    void onlyTellersCanEdit() {
        assertThat(new CurrentUser("t", "T", Role.TELLER).canEdit()).isTrue();
        assertThat(new CurrentUser("a", "A", Role.AUDITOR).canEdit()).isFalse();
    }

    @Test
    void rolesHaveTheLabelsTheHeaderShows() {
        assertThat(Role.TELLER.getLabel()).isEqualTo("Teller");
        assertThat(Role.AUDITOR.getLabel()).isEqualTo("Auditor");
    }
}
