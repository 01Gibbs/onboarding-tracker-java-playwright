package dev.upskill.ledger.auth;

public enum Role {
    /** Can view and change everything. */
    TELLER("Teller"),
    /** Read-only access. */
    AUDITOR("Auditor");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
