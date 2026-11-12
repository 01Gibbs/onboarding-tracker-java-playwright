package dev.upskill.ledger.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

public final class Sessions {

    public static final String USER_ATTRIBUTE = "currentUser";

    private Sessions() {
    }

    public static Optional<CurrentUser> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? Optional.empty()
                : Optional.ofNullable((CurrentUser) session.getAttribute(USER_ATTRIBUTE));
    }
}
