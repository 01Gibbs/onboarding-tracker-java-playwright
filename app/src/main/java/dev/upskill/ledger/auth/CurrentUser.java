package dev.upskill.ledger.auth;

import java.io.Serializable;

public record CurrentUser(String username, String displayName, Role role) implements Serializable {

    public boolean canEdit() {
        return role == Role.TELLER;
    }
}
