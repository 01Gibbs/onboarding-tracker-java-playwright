package dev.upskill.ledger.customer;

public enum CustomerStatus {
    ACTIVE("Active"),
    SUSPENDED("Suspended");

    private final String label;

    CustomerStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
