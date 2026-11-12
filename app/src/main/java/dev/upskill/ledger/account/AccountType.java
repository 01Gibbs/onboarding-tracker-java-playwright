package dev.upskill.ledger.account;

public enum AccountType {
    CURRENT("Current"),
    SAVINGS("Savings");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
