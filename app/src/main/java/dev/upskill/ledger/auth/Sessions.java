package dev.upskill.ledger.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

public final class Sessions {

    public static final String USER_ATTRIBUTE = "currentUser";

    private Sessions() {
    }

    /** Signs {@code user} in on a brand-new session, so a session id seen before sign-in is never reused. */
    public static void start(HttpServletRequest request, CurrentUser user) {
        end(request);
        request.getSession(true).setAttribute(USER_ATTRIBUTE, user);
    }

    public static void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static Optional<CurrentUser> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? Optional.empty()
                : Optional.ofNullable((CurrentUser) session.getAttribute(USER_ATTRIBUTE));
    }
}
