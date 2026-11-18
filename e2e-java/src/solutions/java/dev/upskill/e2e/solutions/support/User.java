package dev.upskill.e2e.solutions.support;

/** The users configured in the app's application.yml. */
public record User(String username, String password, String displayName) {

    public static final User TELLER = new User("teller", "teller-pass", "Terry Teller");
    public static final User AUDITOR = new User("auditor", "auditor-pass", "Alex Auditor");
    public static final User LOCKED = new User("locked", "locked-pass", "Lee Locked");
}
