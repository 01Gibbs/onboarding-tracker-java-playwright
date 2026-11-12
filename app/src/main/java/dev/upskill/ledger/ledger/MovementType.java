package dev.upskill.ledger.ledger;

/** The kinds of money movement a user can request from an account page. */
public enum MovementType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER("Transfer");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
